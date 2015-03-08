#!/usr/bin/env bash

# Utility to invoke eclipse antrunner
#    the build file, if not build.xml, must be first argument
#    that can be followed be target or other arguments


BUILD_HOME=${BUILD_HOME:-/shared/eclipse/eclipse3I}


if [ ! -d "${BUILD_HOME}" ]
then
  echo "ERROR: BUILD_HOME was not an existing directory as expected: ${BUILD_HOME}"
  exit 1
fi

# For most uses, this directory does not HAVE to literally be
# the eclipseBuider. It is in production, but for testing, it can
# be any directory where ${ECLIPSEBUILDER_DIR}/scripts are located.
ECLIPSEBUILDER_DIR=${ECLIPSEBUILDER_DIR:-${BUILD_HOME}/build/supportDir/org.eclipse.releng.eclipsebuilder}

if [ ! -d "${ECLIPSEBUILDER_DIR}/scripts" ]
then
  echo "ERROR: builder scripts was not an existing directory as expected: ${ECLIPSEBUILDER_DIR}/scripts}"
  exit 1
fi

# specify devworkspace and JRE to use to runEclipse
# remember, we want to use Java 5 for processing artifacts.
# Ideally same one used to pre-condition (normalize, -repack)
# the jars in the first place.
#JAVA_5_HOME=${JAVA_5_HOME:-/home/shared/orbit/apps/ibm-java2-i386-50/jre}
#JAVA_5_HOME=${JAVA_5_HOME:-${HOME}/jdks/ibm-java2-x86_64-50}
JAVA_6_HOME=${JAVA_6_HOME:-/shared/common/jdk-1.6.0_26.x86_64}
JAVA_7_HOME=${JAVA_7_HOME:-/shared/common/jdk1.7.0-latest}
JAVA_8_HOME=${JAVA_8_HOME:-/shared/common/jdk1.8.0_x64-latest}

export JAVA_HOME=${JAVA_HOME:-${JAVA_8_HOME}}

devJRE=$JAVA_HOME/jre/bin/java

if [ ! -n ${devJRE} -a -x ${devJRE} ]
then
  echo "ERROR: could not find (or execute) JRE were expected: ${devJRE}"
  exit 1
else
  # display version, just to be able to log it.
  echo "JRE Location and Version: ${devJRE}"
  echo $( $devJRE -version )
fi

# remember, the eclispe install must match the VM used (e.g. both 64 bit, both 32 bit, etc).
ECLIPSE_EXE=${ECLIPSE_EXE:-/shared/eclipse/eclipse3I/build/supportDir/org.eclipse.releng.basebuilder/eclipse}

if [ ! -n ${ECLIPSE_EXE} -a -x ${ECLIPSE_EXE} ]
then
  echo "ERROR: ECLIPSE_EXE is not defined or not executable: ${ECLIPSE_EXE}"
  exit 1001
fi



buildId=$1
shift
if [[ -z "${buildId}" ]]
then
  echo "need buildId as first argument"
fi

BUILDFILE=$1
shift
if [ -e $BUILDFILE ]
then
  BUILDFILESTR=" -file $BUILDFILE"
  shift
else
  BUILDFILESTR=" -file build.xml"
fi

# use special $@ to keep all arguments quoted (instead of one big string)
extraArgs="$@"

echo
echo " BUILDFILESTR: $BUILDFILESTR"
if [ -n "${extraArgs}" ]
then
  echo "   extraArgs: ${extraArgs}"
  echo "      as it is right now, target name must be first \"extraArg\" if specifying one."
fi
echo


devworkspace="${BUILD_HOME}"/workspace-antRunner
devArgs="-Xmx512m -DbuildId=${buildId}"

echo
echo "   dev script:   $0"
echo "   devworkspace: $devworkspace"
echo "   devArgs:      $devArgs"
echo

if [ -n ${ECLIPSE_EXE} -a -x ${ECLIPSE_EXE} ]
then

  ${ECLIPSE_EXE}  -Dhuson=true --launcher.suppressErrors  -nosplash -console -data $devworkspace -application org.eclipse.ant.core.antRunner $BUILDFILESTR ${extraArgs} -vm $devJRE -vmargs $devArgs
  RC=$?
else
  echo "ERROR: ECLIPSE_EXE is not defined to executable eclipse"
  RC=1
fi
exit $RC
