#!/bin/bash
heroku config:set -a sortmybox GOOGLE_CREDS_STRING="$(base64 google-creds_sortmybox-hrd.json)"
