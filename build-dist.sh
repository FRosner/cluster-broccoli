#!/bin/bash
set -e

./setup.sh
cd elm
yarn package
cd ..
sbt clean && sbt dist

set +e
