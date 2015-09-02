#!/usr/bin/env bash

# Utility to invoke p2.process.artifacts via eclipse antrunner
# First argument must be the absolute directory path to the
# (simple) artifact repository.

JAVA_5_HOME=${JAVA_5_HOME:-/shared/common/jdk1.5.0-latest}
JAVA_6_HOME=${JAVA_6_HOME:-/shared/common/jdk1.6.0-latest}
JAVA_7_HOME=${JAVA_7_HOME:-/shared/common/jdk1.7.0-latest}
JAVA_8_HOME=${JAVA_8_HOME:-/shared/common/jdk1.8.0_x64-latest}

export JAVA_HOME=${JAVA_8_HOME}

devJRE="${JAVA_HOME}/bin/java"

if [[ ! -n ${devJRE} -a -x ${devJRE} ]]
then
  echo "ERROR: could not find (or execute) JRE were expected: ${devJRE}"
  exit 1
else
  # display version, just to be able to log it.
  echo "DEBUG: JRE Location and Version: ${devJRE}"
  echo "DEBUG: $( $devJRE -version )"
fi


# remember, the eclipse install must match the VM used (e.g. both 64 bit, both 32 bit, etc).
ECLIPSE_EXE=${ECLIPSE_EXE:-/shared/simrel/tools/eclipse45/eclipse/eclipse}

if [ ! -n ${ECLIPSE_EXE} -a -x ${ECLIPSE_EXE} ]
then
  echo "ERROR: ECLIPSE_EXE is not defined or not executable: ${ECLIPSE_EXE}"
  exit 2
fi

BUILDFILE=cleanComposite.xml
if [[ -z "${BUILDFILE}" ]]
then
  printf "\n\t%s\t%s\n" "ERROR" "must provide ant file to perform composite update as first argument"
  exit 1
fi
if [[ ! -e "${BUILDFILE}" ]]
then
  printf "\n\t%s\t%s\n" "ERROR" "BUILDFILE does not exist: ${BUILDFILE}"
  exit 1
fi
BUILDFILESTR="-f ${BUILDFILE}"

repodir=$1
if [[ -z "${repodir}" ]]
then
  printf "\n\t%s\t%s\n" "ERROR" "repodir must be specified as first argument"
  exit 1
fi
if [[ ! -e "${repodir}" ]]
then
  printf "\n\t%s\t%s\n" "ERROR" "repodir does not exist: ${repodir}"
  exit 1
else
  REPODIRSTR="-Drepodir=${repodir}"
fi

devArgs="$devArgs $REPODIRSTR $COMPLOCATIONSTR"

devworkspace=workspace-updateComposite

${ECLIPSE_EXE}  --launcher.suppressErrors  -nosplash -console -debug -data $devworkspace -application org.eclipse.ant.core.antRunner $BUILDFILESTR ${extraArgs} -vm $devJRE -vmargs $devArgs
RC=$?
if [[ $RC != 0 ]]
then
  echo "error occurred in composite operation RC: $RC"
  exit $RC
fi

exit 0

