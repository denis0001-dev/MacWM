# xinitrc file for testing our window manager.

# 1. Start some programs to play with.
xclock &
xeyes &
# dolphin &
sleep 2 && xterm &

# 2. Start our window manager.
export GLOG_logtostderr=1
realpath .
ls
if [ DEBUGGING = true ]; then
  echo "Running in debug mode."
  gdbserver :1234 ./build/macwm
else
  exec ./build/macwm
fi
