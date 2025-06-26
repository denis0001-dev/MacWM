#!/bin/bash

set -e

# 1. Cd to the correct dir
cd "$(dirname "$0")" || exit

# 2. Constants and variables
IMAGE=ghcr.io/denis0001-dev/macwm

default() {
  local var="$1"
  local def="$2"
  if [ -z "${!var}" ]; then
    eval "$var=\"$def\""
  fi
  echo "$var=${!var}"
}

default DEBUG false
default BUILD_CONTAINER false
default PUSH false
default RUN true

PUSH_VERSION="1.0-$(if [[ $DEBUG == "true" ]]; then echo "debug"; else echo "release"; fi)"

# 3. Build
if [[ $BUILD_CONTAINER == "true" ]]; then
  # 3.1. Build the container with the label
  DOCKER_BUILDKIT=1 docker build \
    --build-arg DEBUG="$DEBUG" \
    -t \
    $IMAGE:"$PUSH_VERSION" \
    .

  # 3.2. Push if needed
  if [[ $PUSH == "true" ]]; then
    docker push "$IMAGE:$PUSH_VERSION"
  fi
fi

# 4. Run
if [[ "$RUN" == "true" ]]; then
  # shellcheck disable=SC2046
  docker run \
   -it \
   --rm \
   -e DISPLAY=docker.for.mac.host.internal:0 \
   -v .:/app \
   $([[ "$DEBUG" != "true" ]] || echo "-p 1234:1234") \
   "$IMAGE:$PUSH_VERSION"
fi