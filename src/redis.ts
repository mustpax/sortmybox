import Redis = require('ioredis');
import moment = require('moment');

const {
  REDIS_PORT,
  REDIS_HOST,
  REDIS_PASSWORD,
} = process.env;

console.log(`Connecting to Redis. Host: ${REDIS_HOST} Port: ${REDIS_PORT}`);
const client = new Redis({
  port: parseInt(REDIS_PORT as string),
  host: REDIS_HOST,
  password: REDIS_PASSWORD,
});

interface QueueItem {
  item: string;
  timestamp: Date;
}

async function dequeue(queue: string, delaySecs: number): Promise<QueueItem|undefined> {
  // There are items to process
  let resp = await client
    .multi()
    .zrange(queue, 0, 0, 'WITHSCORES')
    .zremrangebyrank(queue, 0, 0)
    .exec();

  // first reply is a 2-d array that starts with null for some weird reason
  let item = resp[0][1];
  if (! item || item.length === 0) {
    return;
  }
  let itemTime = moment(parseInt(item[1]));

  let processBefore = moment().subtract(delaySecs, 's');
  if (itemTime.isAfter(processBefore)) {
    // Item is not ready to be processed yet, put it back
    // (but if somebody inserted it back first, do not modify
    await client.zadd(queue, 'NX', item[1], item[0]);
    return;
  }
  return {
    item: item[0],
    timestamp: itemTime.toDate(),
  };
}

export async function enqueue(queue: string, item: string) {
  let timestamp = Date.now();
  return await client.zadd(queue, String(timestamp), item);
}

const queueIntervalMs = 3000;
const queueName = 'sortqueue';
const minTimeBetweenSortSec = 5;

let queueProcessorInterval: NodeJS.Timer|undefined;

import EventEmitter = require("events");
const eventEmitter = new EventEmitter();

const eventName = 'sortuser';

export function onUserReadyForSort(callback: ((userDbxId: string) => void)) {
  eventEmitter.on(eventName, callback);
}

export function startQueueProcessor() {
  if (! queueProcessorInterval) {
    console.log(`Starting sorting queue processor with ${queueIntervalMs}ms interval`);
    queueProcessorInterval = setInterval(async function() {
      let item = await dequeue(queueName, minTimeBetweenSortSec);
      let maxBatch = 100;
      while (item && maxBatch > 0) {
        console.log(`Sorting queue is processing`, item);
        eventEmitter.emit(eventName, item.item);
        item = await dequeue(queueName, minTimeBetweenSortSec);
        maxBatch--;
      }
    }, queueIntervalMs);
  }
}

export function stopQueueProcessor() {
  console.log('Stopping sorting queue processor');
  if (queueProcessorInterval) {
    clearInterval(queueProcessorInterval);
    queueProcessorInterval = undefined;
  }
}

/**
 * Returns true if the given Dropbox user should be processed (i.e. their files
 * sorted) right now.
 * If the user has been processed too recently, they'll be queued up to be
 * processed.
 */
export async function shouldSortUser(userDbxId: string): Promise<boolean> {
  if (! await isSortingEnabled(userDbxId)) {
    return false;
  }

  const key = `sortlock|${userDbxId}`;
  let result = await client.set(key, true, 'nx');
  if (result) {
    // We took the lock, prevent user from being processed for the next N
    // seconds
    await client.expire(key, minTimeBetweenSortSec);
    return true;
  } else {
    // Enqueue the user to be processed later
    await enqueue(queueName, userDbxId);
    return false;
  }
}

function getDisabledKey(userDbxId: string): string {
  return `sortdisabled|${userDbxId}`;
}

export async function isSortingEnabled(userDbxId: string): Promise<boolean> {
  let k = getDisabledKey(userDbxId);
  let result = await client.get(k);
  return result !== 'true';
}

export async function enableSorting(userDbxId: string): Promise<void> {
  let k = getDisabledKey(userDbxId);
  await client.del(k);
}

export async function disableSorting(userDbxId: string): Promise<void> {
  let k = getDisabledKey(userDbxId);
  await client.set(k, 'true');
}
