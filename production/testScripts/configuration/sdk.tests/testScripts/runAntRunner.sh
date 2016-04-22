#!/usr/bin/env bash

# Utility to invoke eclipse antrunner, from Eclipse platform, which must already be
# installed.

# This script here in "testScripts" is not used during the build or testing, but is 
# handy to have nearby to test things like "createDeltaPack.xml". 

bash_script=${0##*/}

BUILDFILE=$1
if [ -e $BUILDFILE ]
then
  BUILDFILESTR=" -file $BUILDFILE"
  shift
else
  BUILDFILESTR=" -file build.xml"
fi

# use special $@ to keep all (remaining) arguments quoted (instead of one big string)
# they are passed through to the antrunner via -vmargs. 

extraArgs="$@"

echo
echo " BUILDFILESTR: $BUILDFILESTR"
if [ -n "${extraArgs}" ]
then
  echo "   extraArgs: ${extraArgs}"
  echo "      as it is right now, target name must be first \"extraArg\" if specifying one."
fi
echo

APP_NAME=org.eclipse.ant.core.antRunner

devworkspace="${PWD}/workspace-antRunner"

echo
echo "   buildId:           ${buildId}"
echo "   buildType:         ${buildType}"
echo "   script:            ${bash_script}"
echo "   devworkspace:      $devworkspace"
echo "   APP_NAME:          $APP_NAME"
echo "   BUILDFILESTR:      $BUILDFILESTR"
echo "   extraArgs:         ${extraArgs}"
echo
export JAVA_HOME=/shared/common/jdk1.8.0_x64-latest
JAVA_EXEC_DIR=${JAVA_HOME}/bin
JAVA_CMD=${JAVA_EXEC_DIR}/java
ECLIPSE_EXE=${PWD}/eclipse/eclipse
echo
$JAVA_CMD -version
echo

${ECLIPSE_EXE} -nosplash -consolelog -debug  -data ${devworkspace} -vm ${JAVA_EXEC_DIR}  -application ${APP_NAME} ${BUILDFILESTR} ${extraArgs}
RC=$?

exit $RC
