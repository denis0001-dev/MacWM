#!/bin/bash

set -e
cd "$(dirname "$0")" || exit

if [[ -z "$DEBUG" ]]; then
  DEBUG=false
fi

docker build \
  --build-arg DEBUG="$DEBUG" \
  -t \
  macwm \
  .
docker run \
 -it \
 --rm \
 -e DISPLAY=docker.for.mac.host.internal:0 \
 macwm