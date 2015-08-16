#!/usr/bin/env bash

# This file is used on production machine, running tests on Hudson, Linux

echo "command line as passed into $(basename ${0}): ${*}"
echo "command line (quoted) as passed into $(basename ${0}): ${@}"

# set minimal path to allow consistency.
# plus, want to have "home"/bin directory, to allow overrides in 'localTestsProperties'
# for non-production builds.
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:~/bin

source localBuildProperties.shsource 2>/dev/null


# jvm should already be defined, by now, in production tests
# export jvm=${jvm:-/shared/common/jdk1.8.0_x64/jre/bin/java}
# but if not, we use a simple 'java'. 
if [[ -z "{jvm}" ]]
then
  echo "WARNING: jvm was not defined, so using simple 'java'."
  export jvm=$(which java)
fi
echo "jvm: $jvm"

if [[ -z "${jvm}" || ! -x ${jvm} ]]
then
  echo "ERROR: No JVM define, or the defined one was found to not be executable"
  echo "    jvm: $jvm"
  exit 1
fi

# On production, WORKSPACE is the 'hudson' workspace. 
# But, if running standalone, we'll assume "up two" from current directoy
WORKSPACE=${WORKSPACE:-"../../"};

stableEclipseSDK=${stableEclipseSDK:-eclipse-SDK-4.5-linux-gtk-x86_64.tar.gz}
stableEclipseInstdallLocation=${stableEclipseInstallLocation:-${WORKSPACE}/org.eclipse.releng.basebuilder}

# Note: test.xml will "reinstall" fresh install of what we are testing,
# but we do need an install for initial launcher, and, later, need one for a
# stable version of p2 director. For both purposes, we
# we should use "old and stable" version,
# which needs to be installed in ${stableEclipseInstallLocation}.
# Note: for production tests, we use ${WORKSPACE}/org.eclipse.releng.basebuilder,
# for historical reasons. The "true" (old) basebuilder does not have an 'eclipse' directory;
# plugins is directly under org.eclipse.releng.basebuilder.
if [ ! -r ${stableEclipseInstallLocation} || ! -r "${stableEclipseInstallLocation}/eclipse" ]
then
  mkdir -p ${stableEclipseInstallLocation}
  tar -xf ${stableEclipseSDK} -C ${stableEclipseInstallLocation}
fi

launcher=$(find ${stableEclipseInstallLocation} -name "org.eclipse.equinox.launcher_*.jar" )
if [ -z "${launcher}" ]
then
   echo "ERROR: launcher not found in ${stableEclipseInstallLocation}"
   exit 1
fi
echo "launcher: $launcher"

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
      tests="$tests\ ${1}";;
  esac
  shift
done

echo "Specified test targets (if any): ${tests}"

echo "Specified ext dir (if any): ${extdirproperty}"

# for *nix systems, os, ws and arch values must be specified
if [[ -z "${os}" || -z "${ws}" || -z "${arch}" ]]
then
  echo >&2 "WARNING: On some systems, os, ws, and arch values must be specified,"
  echo >&2 "         but can usually be correctly inferred given the running VM, etc."
  echo >&2 "$usage"
else 
  platformArgString=""
  platformParmString=""
  if [[ -n "${os}" ]]
  then
    platformArgString="${platformArgString} -Dosgi.os=$os"
    platformParmString="${platformParmString} -Dos=$os"
  fi
  if [[ -n "${ws}" ]]
  then
    platformArgString="${platformArgString} -Dosgi.ws=$ws"
    platformParmString="${platformParmString} -Dws=$ws"
  fi
  if [[ -n "${arch}" ]]
  then
    platformArgString="${platformArgString} -Dosgi.arch=$arch"
    platformParmString="${platformParmString} -Darch=$arch"
  fi
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

# During production tests, we define 'testedPlatform' as a combination of 
# os, ws, arch, and vm level. But for stand alone tests, by default, 
# we will just label simply. 
if [[ -z "${testedPlatform}" ]]
then 
   export testedPlatform=output
fi

echo "extdirprop in runtest.sh: ${extdirprop}"
echo "extdirproperty in runtest.sh: ${extdirproperty}"
echo "ANT_OPTS in runtests.sh: ${ANT_OPTS}"
echo "platformArgString: ${platformArgString}"
echo "platformParmString: ${platformParmString}"

# -Dtimeout=300000 "${ANT_OPTS}"
if [[ -n "${extdirproperty}" ]]
then
  echo "running with extdir defined"
  $jvm ${ANT_OPTS} "${extdirproperty}" ${platformArgString} -jar $launcher -data workspace -application org.eclipse.ant.core.antRunner -file ${PWD}/test.xml ${ANT_OPTS} ${platformParmString} -D$installmode=true $properties -logger org.apache.tools.ant.DefaultLogger $tests
else
  echo "running without extdir defined"
  $jvm ${ANT_OPTS} ${platformArgString}  -jar $launcher -data workspace -application org.eclipse.ant.core.antRunner -file ${PWD}/test.xml  ${ANT_OPTS} ${platformParmString} -D$installmode=true $properties -logger org.apache.tools.ant.DefaultLogger  $tests
fi
