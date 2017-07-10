#!/bin/bash
set -eou pipefail

./setup.sh
cd elm
yarn format:validate
yarn compile
yarn test
cd ..
sbt test
