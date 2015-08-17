# TODO: consider moving all this to 'testAll.sh'. (If testAll.sh stays around)
# make sure there is a window manager running. See bug 379026
# we should not have to, but may be a quirk/bug of hudson setup
# assuming metacity attaches to "current" display by default (which should have
# already been set by Hudson). We echo its value here just for extra reference/cross-checks.
# TODO: this does not work, when doing local build. I think the
#  solution, for both cases, is to start an instance of xvfb? And then should
# have a "background" flag to run on back ground (and not the active user display)
# since most uses for "local builds" users would want to see the tests running live.
# This next section on window mangers is needed if and only if "running in background" or
# started on another machine, such as Hudson or Cruisecontrol, where it may be running
# "semi headless", but still needs some window manager running for UI tests.
echo "Check if any window managers are running (xfwm|twm|metacity|beryl|fluxbox|compiz|kwin|openbox|icewm):"
wmpss=$(ps -ef | egrep -i "xfwm|twm|metacity|beryl|fluxbox|compiz|kwin|openbox|icewm" | grep -v egrep)
echo "Window Manager processes: $wmpss"
echo

# in this case, do not "--replace" any existing ones, for this DISPLAY
# added bit bucket for errors, in attempt to keep from filling up Hudson log with "warnings", such as hundreds of
#     [exec] Window manager warning: Buggy client sent a _NET_ACTIVE_WINDOW message with a timestamp of 0 for 0x800059 (Java - Ecl)
#     [exec] Window manager warning: meta_window_activate called by a pager with a 0 timestamp; the pager needs to be fixed.
#
metacity --display=$DISPLAY  --sm-disable 2>/dev/null &
METACITYRC=$?
METACITYPID=$!

if [[ $METACITYRC == 0 ]]
then
  # TODO: we may want to kill the one we started, at end of tests?
  echo $METACITYPID > epmetacity.pid
  echo "  metacity (with no --replace) started ok. PID: $METACITYPID"
else
  echo "  metacity (with no --replace) failed. RC: $METACITYRC"
  # This should not interfere with other jobs running on Hudson, the DISPLAY should be "ours".
  metacity --display=$DISPLAY --replace --sm-disable  &
  METACITYRC=$?
  METACITYPID=$!
  if [[ $METACITYRC == 0 ]]
  then
    # TODO: we may want to kill the one we started, at end of tests?
    echo $METACITYPID > epmetacity.pid
    echo "  metacity (with --replace) started ok. PID: $METACITYPID"
  else
    echo "  metacity (with --replace) failed. RC: $METACITYRC"
    echo "   giving up. But continuing tests"
  fi
fi


echo

# list out metacity processes so overtime we can see if they accumulate, or if killed automatically
# when our process exits. If not automatic, should use epmetacity.pid to kill it when we are done.
echo "Current metacity processes running (check for accumulation):"
ps -ef | grep "metacity" | grep -v grep
echo

echo "Triple check if any window managers are running (at least metacity should be!):"
wmpss=$(ps -ef | egrep -i "xfwm|twm|metacity|beryl|fluxbox|compiz|kwin|openbox|icewm" | grep -v egrep)
echo "Window Manager processes: $wmpss"
echo

