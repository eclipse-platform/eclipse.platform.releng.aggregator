#!/usr/bin/env bash
#*******************************************************************************
# Copyright (c) 2016 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     David Williams - initial API and implementation
#*******************************************************************************

# Utility to invoke eclipse antrunner, from the "base builder", which should already be
# installed on the build machine, where the build is.
#
# we assume promoteUtilities.shsource has already been sourced, and exported the required variables.
# TODO: eventually could do more error checking to be sure.

# to create composite, we expect "addToComposite.xml" to be passed in.
BUILDFILE=$1
if [ -e $BUILDFILE ]
then
  BUILDFILESTR=" -file $BUILDFILE"
  shift
else
  BUILDFILESTR=" -file build.xml"
fi

# use special $@ to keep all (remaining) arguments quoted (instead of one big string)
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
echo "   dev script:        $BASH_SCRIPT"
echo "   devworkspace:      $devworkspace"
echo "   devArgs (-vmargs):  $devArgs"
echo "   BUILDFILESTR:      $BUILDFILESTR"
echo "   extraArgs:         ${extraArgs}"
echo

echo
$JAVA_CMD -version
echo

${ECLIPSE_EXE} -nosplash -consolelog -debug -data ${devworkspace} -vm ${JAVA_EXEC_DIR} -application ${APP_NAME}  $BUILDFILESTR ${extraArgs}
RC=$?

exit $RC
