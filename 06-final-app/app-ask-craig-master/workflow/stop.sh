#!/usr/bin/env bash
set -ex

PID=$(cat server.pid)
while kill $PID > /dev/null
do
  sleep 1
  if ! ps -p $PID > /dev/null ; then
    break
  fi
done
rm server.pid
