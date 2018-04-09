import express = require('express');

import { asyncRoute } from './utils';

const app: express.Router = express.Router();

app.get('/', asyncRoute(async function(req, res) {
  res.send('hi');
}));

export default app;
