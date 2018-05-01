#!/usr/bin/env node
if (process.env.GOOGLE_CLOUD_PROJECT !== 'moosepax-1248') {
  console.error('Error: command only allowed for development project moosepax-1248');
  process.exit(1);
}
if (process.env.NODE_ENV === 'production') {
  console.error('Error: command not allowed in production');
  process.exit(1);
}
