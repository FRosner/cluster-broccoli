#!/bin/bash
set -eou pipefail

cd elm
yarn install
yarn setup
cd ..
