import Redis = require('ioredis');
import moment = require('moment');

const client = new Redis();

export interface QueueItem {
  item: string;
  timestamp: Date;
}

// TODO limit items returned
export async function dequeue(queue: string, delaySecs: number): Promise<QueueItem|undefined> {
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
  let timestamp = moment().valueOf();
  return await client.zadd(queue, String(timestamp), item);
}
