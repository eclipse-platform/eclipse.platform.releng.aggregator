#!/bin/bash
#*******************************************************************************
# Copyright (c) 2019 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Sravan Kumar Lakkimsetti - initial API and implementation
#*******************************************************************************


# Utility to invoke eclipse antrunner 

baseBuilderDir=${WORKSPACE}/eclipse
if [[ ! -d "${baseBuilderDir}" ]]
then
  echo "ERROR: The directory did not exist."
  echo "    baseBuilderDir: ${baseBuilderDir}"
  exit 1
fi

launcherJar=$( find $baseBuilderDir/ -name "org.eclipse.equinox.launcher_*.jar" | sort | head -1 )
if [[ -z "${launcherJar}" || ! -f "${launcherJar}" ]]
then
  echo "ERROR: The launcher did not exist."
  echo "    launcherJar: ${launcherJar}"
  exit 1
fi

JAVA_8_HOME=/opt/tools/java/oracle/jdk-8/latest
export JAVA_HOME=${JAVA_HOME:-${JAVA_8_HOME}} 

if [[ ! -d "${JAVA_HOME}" ]]
then
  echo "ERROR: JAVA_HOME did not exist."
  echo "    JAVA_HOME: ${JAVA_HOME}"
  exit 1
fi
javaCMD=${JAVA_HOME}/bin/java

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


devworkspace="${BUILD_HOME}"/workspace-antRunner
devArgs=-Xmx512m

echo
echo "   buildId:           ${buildId}"
echo "   buildId:           ${eclipseStream}"
echo "   basebuilderParent: ${basebuilderParent}"
echo "   baseBuilderDir:    ${baseBuilderDir}"
echo "   launcherJar:       ${launcherJar}"
echo "   BUILD_HOME:        ${BUILD_HOME}"
echo "   dev script:        $0"
echo "   devworkspace:      $devworkspace"
echo "   devArgs (-vmargs):  $devArgs"
echo "   javaCMD:           $javaCMD"
echo "   BUILDFILESTR:      $BUILDFILESTR"
echo "   extraArgs:         ${extraArgs}"
echo

${javaCMD}  -jar ${launcherJar} -nosplash -consolelog -debug -data $devworkspace -application org.eclipse.ant.core.antRunner $BUILDFILESTR ${extraArgs} -vmargs $devArgs

