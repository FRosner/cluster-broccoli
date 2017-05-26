#!/bin/bash
set -eou pipefail

./setup.sh
cd public
npm run package
cd ..
sbt clean && sbt dist
