#!/bin/bash

# Source: http://blog.nonuby.com/blog/2012/07/05/copying-env-vars-from-one-heroku-app-to-another/

set -e

sourceApp="$1"
targetApp="$2"

while read key value; do
  key=${key%%:}
  echo "Setting $key=$value"
  heroku config:set "$key=$value" --app "$targetApp"
done  < <(heroku config --app "$sourceApp" | sed -e '1d')