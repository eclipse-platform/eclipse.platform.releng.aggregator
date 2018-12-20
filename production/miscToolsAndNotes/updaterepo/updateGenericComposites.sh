#!/usr/bin/env bash
#*******************************************************************************
# Copyright (c) 2015 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     David Williams - initial API and implementation
#*******************************************************************************

# Utility to invoke p2.process.artifacts via eclipse antrunner
# First argument must be the absolute directory path to the
# (simple) artifact repository.

JAVA_5_HOME=${JAVA_5_HOME:-/shared/common/jdk1.5.0-latest}
JAVA_6_HOME=${JAVA_6_HOME:-/shared/common/jdk1.6.0-latest}
JAVA_7_HOME=${JAVA_7_HOME:-/shared/common/jdk1.7.0-latest}
JAVA_8_HOME=${JAVA_8_HOME:-/shared/common/jdk1.8.0_x64-latest}

export JAVA_HOME=${JAVA_HOME:-${JAVA_8_HOME}}

devJRE="${JAVA_HOME}"/bin/java

if [ ! -n ${devJRE} -a -x ${devJRE} ]
then
  echo "ERROR: could not find (or execute) JRE where expected: ${devJRE}"
  exit 1
else
  # display version, just to be able to log it.
  echo "JRE Location and Version: ${devJRE}"
  echo $( $devJRE -version )
fi

# Assumed there is an Eclipse SDK already installed. 
# remember, the eclipse install must match the VM used (e.g. both 64 bit, both 32 bit, etc).
ECLIPSE_EXE=${ECLIPSE_EXE:-utilities/eclipse.platform.releng.tychoeclipsebuilder/eclipse/org.eclipse.releng.basebuilder/eclipse}

if [[ ! -n ${ECLIPSE_EXE} && -x ${ECLIPSE_EXE} ]]
then
  echo "ERROR: ECLIPSE_EXE is not defined or not executable: ${ECLIPSE_EXE}"
  exit 2
fi

BUILDFILE=updateGenericComposites.xml
if [[ -z "${BUILDFILE}" ]]
then
  printf "\n\t%s\t%s\n" "ERROR:" "must provide ant file to perform composite update"
  exit 1
fi
if [[ ! -e "${BUILDFILE}" ]]
then
  printf "\n\t%s\t%s\n" "ERROR:" "BUILDFILE does not exist: ${BUILDFILE}"
  exit 1
fi
BUILDFILESTR="-f ${BUILDFILE}"

currentStream=$1
if [[ -z "${currentStream}" ]]
then
  printf "\n\t%s\t%s\n" "WARNING:" "Current stream version not specified on command line, assuming 4.11"
  currentStream="4.11"
fi

maintenanceStream=$2
if [[ -z "${maintenanceStream}" ]]
then
  printf "\n\t%s\t%s\n" "WARNING:" "Maintenance stream version not specified on command line, assuming 4.10"
  maintenanceStream="4.10"
fi



devArgs="$devArgs -DcurrentStream=$currentStream -DmaintenanceStream=$maintenanceStream"

devworkspace=workspace-updateGenericComposite

${ECLIPSE_EXE}  --launcher.suppressErrors  -nosplash -console -data $devworkspace -application org.eclipse.ant.core.antRunner $BUILDFILESTR ${extraArgs} -vm $devJRE -vmargs $devArgs
RC=$?
if [[ $RC != 0 ]]
then
  echo "error occurred in composite operation RC: $RC"
  exit $RC
fi



exit 0

