#!/bin/bash
# xinitrc file for testing our window manager.

set -e

# 1. Constants
WM=./build/bin/flwm

if [ -z "$DEBUGGING" ]; then
  export DEBUGGING=false
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
if $DEBUGGING; then
  echo "Running in debug mode."
  gdbserver :1234 $WM
  WMPID=$!
else
  $WM
  WMPID=$!
fi