#!/usr/bin/env bash
set -ex

npm install

# Prime database
mongoimport \
  --verbose \
  --host=$DB_HOST \
  --db=app-ask-craig \
  --collection=jobs \
  --type=csv \
  --headerline \
  --file=../workflow/data/craigslistJobTitles.csv

