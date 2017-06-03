#!/bin/bash
set -eou pipefail

cd elm
npm install
npm run setup
cd ..
