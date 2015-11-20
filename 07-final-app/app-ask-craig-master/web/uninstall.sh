#!/usr/bin/env bash
set -ex

# Drop database
mongo app-ask-craig --eval "db.dropDatabase()"

