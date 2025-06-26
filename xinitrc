#!/bin/bash

set -e

# 1. Constants
WM=./build/bin/flwm

if [ -z "$DEBUG" ]; then
  DEBUG=false
fi

cleanup() {
  sleep 3
  kill $WMPID $XTERMPID $XEYESPID
}

# 2. Start some programs to play with
sleep 2
xterm &
XTERMPID=$!
xeyes &
XEYESPID=$!

# 3. Set up a trap to kill all the windows on exit
trap cleanup EXIT

# 3. Start our window manager
if $DEBUG; then
  echo "Running in debug mode."
  gdbserver :1234 $WM
  WMPID=$!
else
  $WM
  WMPID=$!
fi