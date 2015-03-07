#!/usr/bin/env bash

# Utility to invoke p2.process.artifacts via eclipse antrunner
# First argument must be the absolute directory path to the
# (simple) artifact repository.

JAVA_5_HOME=${JAVA_5_HOME:-/shared/common/jdk1.5.0-latest}
JAVA_6_HOME=${JAVA_6_HOME:-/shared/common/jdk1.6.0-latest}
JAVA_7_HOME=${JAVA_7_HOME:-/shared/common/jdk1.7.0-latest}
JAVA_8_HOME=${JAVA_8_HOME:-/shared/common/jdk1.8.0_x64-latest}

export JAVA_HOME=${JAVA_8_HOME}

devJRE="${JAVA_HOME}"/bin/java

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
ECLIPSE_EXE=${ECLIPSE_EXE:-/shared/eclipse/sdk/eclipse43RC2/eclipse/eclipse}

if [ ! -n ${ECLIPSE_EXE} -a -x ${ECLIPSE_EXE} ]
then
  echo "ERROR: ECLIPSE_EXE is not defined or not executable: ${ECLIPSE_EXE}"
  exit 2
fi

BUILDFILE=$1
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

repodir=$2
if [[ -z "${repodir}" ]]
then
  printf "\n\t%s\t%s\n" "ERROR" "repodir must be specified as second argument"
  exit 1
fi
if [[ ! -e "${repodir}" ]]
then
  printf "\n\t%s\t%s\n" "ERROR" "repodir does not exist: ${repodir}"
  exit 1
else
  REPODIRSTR="-Drepodir=${repodir}"
fi

complocation=$3
if [[ -z "${complocation}" ]]
then
  printf "\n\t%s\t%s\n" "WARNING" "complocation not specified. Assumed to be in ant xml file."
else
  COMPLOCATIONSTR="-Dcomplocation=${complocation}"
fi


devArgs="$devArgs $REPODIRSTR $COMPLOCATIONSTR"

devworkspace=workspace-updateComposite

${ECLIPSE_EXE}  --launcher.suppressErrors  -nosplash -console -data $devworkspace -application org.eclipse.ant.core.antRunner $BUILDFILESTR ${extraArgs} -vm $devJRE -vmargs $devArgs
RC=$?
if [[ $RC != 0 ]]
then
  echo "error occured in composite operation RC: $RC"
  exit $RC
fi

# sanity check existence of directory or else risk removing wrong stuff
compdir="${repodir}/${complocation}"
if [[ -n {$complocation} && -d ${compdir} ]]
then
  # comment out/echo for now, since we don't expect to use
  # this and echo can serve as a test if working as intended.
  # otherwise, we'd remove whole repo!
  echo "rm -fr ${compdir}"
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo "remove failed: ${compdir}: $RC"
    exit $RC
  fi
else
  echo "WARNING: expected complocation directory did not exist. Nothing removed. Assumed done in ant xml file or remove manually."
fi

exit 0

