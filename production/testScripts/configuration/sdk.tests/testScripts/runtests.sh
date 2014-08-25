#!/usr/bin/env bash

# This file is used on production machine, running tests on Hudson, Linux

echo "command line as passed into $(basename ${0}): ${*}"
echo "command line (quoted) as passed into $(basename ${0}): ${@}"

# set minimal path to allow consistency. 
# plus, want to have "home"/bin directory, to allow overrides in 'localTestsProperties' 
# for non-production builds.
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:~/bin

source localTestsProperties.shsource 2>/dev/null


# should already be defined, by now, in production tests
export jvm=${jvm:-/shared/common/jdk-1.6.x86_64/jre/bin/java}
echo "jvm: $jvm"

if [ -z "${jvm}" -o ! -e ${jvm} ] 
then 
  echo "No JVM define, or the defined one was found to not be executable"
  echo "    jvm: $jvm"
  exit 1
fi

stableEclipseSDK=${stableEclipseSDK:-eclipse-SDK-4.4-linux-gtk-x86_64.tar.gz}
stableEclipseInstallLocation=${stableEclipseInstallLocation:-${WORKSPACE}/org.eclipse.releng.basebuilder}
# Note: test.xml will "reinstall" fresh install of what we are testing, 
# but we do need an install for initial launcher, and, later, need one for a 
# stable version of p2 director. For both purposes, we
# we should use "old and stable" version, 
# which needs to be installed in ${stableEclipseInstallLocation}. 
# Note: for production tests, we use ${WORKSPACE}/org.eclipse.releng.basebuilder, 
# for historical reasons. The "true" (old) basebuilder does not have an 'eclipse' directory; 
# plugins is directly under org.eclipse.releng.basebuilder.
if [ ! -r ${stableEclipseInstallLocation} ]
then
  mkdir stableEclipseSDK
  tar -xf ${stableEclipseSDK} -C ${stableEclipseInstallLocation}
  # note, the file pattern to match, must not start with */plugins because there is no leading '/' in the zip file, since they are repos.
  #We no longer use "dropins" for tests! test.xml will "install" them, from "test repo"
  #unzip -qq -o -C eclipse-junit-tests-*.zip plugins/org.eclipse.test* -d eclipse/dropins/
fi

launcher=$(find ${stableEclipseInstallLocation} -name "org.eclipse.equinox.launcher_*.jar" )
echo "launcher: $launcher"
if [ -z "${launcher}" ]
then
   echo "launcher not found in ${stableEclipseInstallLocation}"
   exit 1
fi


# define, but null out variables we expect on the command line

# operating system, windowing system and architecture variables
os=
ws=
arch=

# list of tests (targets) to execute in test.xml
tests=

# default value to determine if eclipse should be reinstalled between running of tests
installmode="clean"

# name of a property file to pass to Ant
properties=

# ext dir customization. Be sure "blank", if not defined explicitly on command line
extdirproperty=

# message printed to console
usage="usage: $0 -os <osType> -ws <windowingSystemType> -arch <architecture> [-noclean] [<test target>][-properties <path>]"


# proces command line arguments
while [ $# -gt 0 ]
do
  case "${1}" in
    -dir)
      dir="${2}"; shift;;
    -os)
      os="${2}"; shift;;
    -ws)
      ws="${2}"; shift;;
    -arch)
      arch="${2}"; shift;;
    -noclean)
      installmode="noclean";;
    -properties)
      properties="-propertyfile ${2}";shift;;
    -extdirprop)
      extdirproperty="-Djava.ext.dirs=${2}";shift;;
    -vm)
      jvm="${2}"; shift;;
    *)
      tests=$tests\ ${1};;
  esac
  shift
done

echo "Specified test targets (if any): ${tests}"

echo "Specified ext dir (if any): ${extdirproperty}"

# for *nix systems, os, ws and arch values must be specified
if [ "x$os" = "x" ]
then
  echo >&2 "$usage"
  exit 1
fi

if [ "x$ws" = "x" ]
then
  echo >&2 "$usage"
  exit 1
fi

if [ "x$arch" = "x" ]
then
  echo >&2 "$usage"
  exit 1
fi



# run tests

#echo " = = = Start list environment variables in effect = = = ="
#env
#echo " = = = End list environment variables in effect = = = ="

# TODO: consider moving all this to 'testAll.sh'. (If testAll.sh stays around) 
# make sure there is a window manager running. See bug 379026
# we should not have to, but may be a quirk/bug of hudson setup
# assuming metacity attaches to "current" display by default (which should have
# already been set by Hudson). We echo its value here just for extra reference/cross-checks.

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

echo "extdirprop in runtest: ${extdirprop}"
echo "extdirproperty in runtest: ${extdirproperty}"
echo "ANT_OPTS in runtests: ${ANT_OPTS}"


# -Dtimeout=300000 "${ANT_OPTS}"
if [[ ! -z "${extdirproperty}" ]]
then
  $jvm ${ANT_OPTS} "${extdirproperty}" -Dosgi.os=$os -Dosgi.ws=$ws -Dosgi.arch=$arch -jar $launcher -data workspace -application org.eclipse.ant.core.antRunner -file ${PWD}/test.xml ${ANT_OPTS} -Dws=$ws -Dos=$os -Darch=$arch -D$installmode=true $properties -logger org.apache.tools.ant.DefaultLogger $tests
else
  $jvm  ${ANT_OPTS} -Dosgi.os=$os -Dosgi.ws=$ws -Dosgi.arch=$arch  -jar $launcher -data workspace -application org.eclipse.ant.core.antRunner -file ${PWD}/test.xml  ${ANT_OPTS} -Dws=$ws -Dos=$os -Darch=$arch -D$installmode=true $properties -logger org.apache.tools.ant.DefaultLogger  $tests 
fi
