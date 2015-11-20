#!/usr/bin/env bash

touch \
  web/stdout.log \
  web/stderr.log \
  workflow/stdout.log \
  workflow/stderr.log

tail \
  -f web/stdout.log \
  -f web/stderr.log \
  -f workflow/stdout.log \
  -f workflow/stderr.log

