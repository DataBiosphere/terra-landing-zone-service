#!/usr/bin/env bash

secs=900
endTime=$(( $(date +%s) + secs ))

while [ $(date +%s) -lt $endTime ] ; do
  result=$(az account get-access-token --output jsonc | jq .expiresOn)
  echo "Fetched a token, expires on ${result}"
  sleep 10
done