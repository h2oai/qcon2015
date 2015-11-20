#!/usr/bin/env bash
set -e
set -x

pushd workflow
  mongoexport \
    --verbose \
    --host=$DB_HOST \
    --db=app-ask-craig \
    --collection=jobs \
    --type=csv \
    --fields=category,jobtitle \
    --out=./data/craigslistJobTitles.csv
popd

pushd web
  ./node_modules/.bin/coffee retrain.coffee
popd
