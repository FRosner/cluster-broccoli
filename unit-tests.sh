#!/bin/bash
set -eou pipefail

./setup.sh
cd public
npm run compile
npm run test
cd ..
sbt test
