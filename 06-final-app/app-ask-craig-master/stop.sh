#!/usr/bin/env bash
set -ex

pushd web
  ./stop.sh
popd

pushd workflow
  ./stop.sh
popd

