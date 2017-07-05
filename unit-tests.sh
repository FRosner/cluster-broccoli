#!/bin/bash
set -eou pipefail

./setup.sh
cd elm
npm run format:validate
npm run compile
npm run test
cd ..
sbt test
