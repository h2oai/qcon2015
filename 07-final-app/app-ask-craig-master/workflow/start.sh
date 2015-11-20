#!/usr/bin/env bash
set -ex

./gradlew run > stdout.log 2> stderr.log &
PID=$!
if ps -p $PID > /dev/null ; then
  echo $PID > server.pid
else
  echo "Could not start workflow server"
  exit 1
fi
