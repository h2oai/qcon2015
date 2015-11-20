#!/usr/bin/env bash
set -ex

pushd workflow
  ./install.sh
popd

pushd web
  ./install.sh
popd

echo "Installation completed!"
