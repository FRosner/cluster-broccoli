#!/bin/bash
set -e

./setup.sh
cd public
npm run package
cd ..
sbt clean && sbt dist

set +e
