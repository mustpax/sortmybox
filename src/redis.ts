import Redis = require('ioredis');
import moment = require('moment');

const client = new Redis();

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

const queueIntervalMs = 1e3; // 5 seconds
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
    queueProcessorInterval = setInterval(async function() {
      console.log('No items to process');
      let item = await dequeue(queueName, minTimeBetweenSortSec);
      let maxBatch = 100;
      while (item && maxBatch > 0) {
        eventEmitter.emit(eventName, item.item);
        item = await dequeue(queueName, minTimeBetweenSortSec);
        maxBatch--;
      }
    }, queueIntervalMs);
  }
}

export function stopQueueProcessor() {
  if (queueProcessorInterval) {
    clearInterval(queueProcessorInterval);
    queueProcessorInterval = undefined;
  }
}

// Returns true if the given Dropbox user should be processed (i.e. their files
// sorted) right now.
// If the user has been processed too recently, they'll be queued up to be
// processed
export async function shouldSortUser(userDbxId: string): Promise<boolean> {
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
