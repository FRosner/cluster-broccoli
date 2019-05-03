#!/bin/bash

check_service() {
    scheme=${1:-http}
    url=${2:-localhost}
    port=${3:-9000}
    echo "checking service at $scheme://$url:$port"
    attempt_counter=0
    max_attempts=${BROCCOLI_TIMEOUT_ATTEMPTS:-10}
    until $(curl --output /dev/null --silent --head --fail $scheme://$url:$port); do
    if [[ ${attempt_counter} -eq ${max_attempts} ]];then
        echo "Max attempts reached. Could not connect."
        exit 1
    fi
    attempt_counter=$(($attempt_counter+1))
    printf '.'
    sleep $BROCCOLI_SLEEP_SHORT
    done
    echo "Broccoli started successfully."
}