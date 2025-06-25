# xinitrc file for testing our window manager.

if [ -z "$DEBUGGING" ]; then
  export DEBUGGING=false
fi

# 1. Start some programs to play with.
if $DEBUGGING; then
  gdbserver :1235 ./build/wallpaper ./src/wallpaper/wallpaper.jpg &
  WALLPAPERPID=$!
else
  ./build/wallpaper ./src/wallpaper/wallpaper.jpg &
  WALLPAPERPID=$!
fi
sleep 2 && xterm &
XTERMPID=$!

# 2. Start our window manager.
export GLOG_logtostderr=1
realpath .
ls
if $DEBUGGING; then
  echo "Running in debug mode."
  gdbserver :1234 ./build/macwm
  WMPID=$!
else
  exec ./build/macwm
  WMPID=$!
fi
export DEBUGGING=false
export DEBUG=false

sleep 3
echo "Killing all windows."
kill $WALLPAPERPID
kill $WMPID
kill $XTERMPID