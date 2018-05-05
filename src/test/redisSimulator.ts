import {
  startQueueProcessor,
  shouldSortUser,
  onUserReadyForSort,
} from '../redis';
import _ = require("underscore");
let users = ['a', 'b', 'c'];

let count = 100;
let interval = setInterval(async function() {
  let [user] = _.sample(users, 1);
  if (await shouldSortUser(user)) {
    console.log('Processing user primary', user, new Date());
  }
  count--;
  if (count <= 0) {
    console.log('Stopped interval');
    clearInterval(interval);
  }
}, 50);

startQueueProcessor();

onUserReadyForSort(function(userId) {
  console.log('Processing user async', userId, new Date());
});
