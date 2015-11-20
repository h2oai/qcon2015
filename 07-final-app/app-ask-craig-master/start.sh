#!/usr/bin/env bash
set -ex

pushd workflow
  ./start.sh
popd

pushd web
  ./start.sh
popd

