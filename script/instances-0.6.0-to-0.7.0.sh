#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: instances-0.6.0-to-0.7.0.sh <instanceDir>"
  exit 1
fi

set -euo pipefail

jq_version="$(jq --version 2>&1)" # redirecting stderr https://github.com/stedolan/jq/issues/1452
if [[ $jq_version != *"1.5"* ]]; then
  echo "ERROR: This script was tested against jq-1.5 and should be run against that as well."
else
  instanceDir="$1"
  backupDir="$instanceDir.bak_$(date +%s)"

  echo "Backing up $instanceDir to $backupDir."
  cp -r "$instanceDir" "$backupDir"
  echo "Converting instances format in $instanceDir from Broccoli <0.6.0 to 0.7.0."
  for instanceFile in "$instanceDir"/*.json; do
    echo "- Converting $instanceFile"
    tmpInstanceFile="$instanceFile.tmp"
    jq '.template.parameterInfos = (.template.parameterInfos | with_entries(.value.id = .value.name))' < "$instanceFile" > "$tmpInstanceFile"
    mv "$tmpInstanceFile" "$instanceFile"
  done

  echo "Conversion finished. Looks like everything went well."
  read -p "Delete $backupDir? [y/n] " -n 1 -r
  echo
  if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Deleting $backupDir."
    rm -rf "$backupDir"
  else
    echo "Keeping $backupDir for now. You have to delete it manually."
  fi
fi
