#!/bin/bash
set -eou pipefail

cd public
npm install
npm run setup
npm run package
cd ..
sbt dist
