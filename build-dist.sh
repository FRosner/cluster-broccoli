#!/bin/bash
set -e

./setup.sh
cd elm
npm run package
cd ..
sbt clean && sbt dist

set +e
