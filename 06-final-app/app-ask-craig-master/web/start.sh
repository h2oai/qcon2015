#!/usr/bin/env bash
set -ex

./node_modules/.bin/coffee server.coffee > stdout.log 2> stderr.log &
PID=$!
if ps -p $PID > /dev/null ; then
  echo $PID > server.pid
else
  echo "Could not start app server"
  exit 1
fi
