#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: instances-0.6.0-to-0.7.0.sh <instanceDir>"
  exit 1
fi

set -euo pipefail

instanceDir="$1"
backupDir="$instanceDir.bak_$(date +%s)"

echo "Backing up $instanceDir to $backupDir."
cp -r $instanceDir $backupDir
echo "Converting instances format in $instanceDir from Broccoli <0.6.0 to 0.7.0."
for instanceFile in $instanceDir/*.json; do
  echo "- Converting $instanceFile"
  tmpInstanceFile="$instanceFile.tmp"
  cat $instanceFile | jq '.template.parameterInfos = (.template.parameterInfos | with_entries(.value.id = .value.name))' > $tmpInstanceFile
  mv $tmpInstanceFile $instanceFile
done

echo "Conversion finished. Looks like everything went well."
read -p "Delete $backupDir? [y/n] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
  echo "Deleting $backupDir."
  rm -rf $backupDir
else
  echo "Keeping $backupDir for now. You have to delete it manually."
fi
