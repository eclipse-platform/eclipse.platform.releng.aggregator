#!/usr/bin/env bash

# Utility to invoke p2.process.artifacts via eclipse antrunner
# First argument must be the absolute directory path to the
# (simple) artifact repository.

#JAVA_5_HOME=${JAVA_5_HOME:-/home/shared/orbit/apps/ibm-java2-i386-50/jre}
JAVA_5_HOME=${JAVA_5_HOME:-/shared/common/jdk-1.5.0-22.x86_64/jre}
JAVA_6_HOME=${JAVA_6HOME:-/shared/common/sun-jdk1.6.0_21_x64}

export JAVA_HOME=${JAVA_6_HOME}

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
ECLIPSE_EXE=${ECLIPSE_EXE:-/shared/eclipse/eclipsesdk372/eclipse/eclipse}

if [ ! -n ${ECLIPSE_EXE} -a -x ${ECLIPSE_EXE} ]
then
    echo "ERROR: ECLIPSE_EXE is not defined or not executable: ${ECLIPSE_EXE}"
    exit 2
fi

BUILDFILE=$1
if [[ -z "${BUILDFILE}" ]]
then
    printf "/n/t%s/t%s/n" "ERROR" "must provide ant file to perform composite update as first argument"
    exit 1
fi
if [[ ! -e "${BUILDFILE}" ]]
then
    print "/n/t%s/t%s/n" "ERROR" "BUILDFILE does not exist: ${BUILDFILE}"
    exit 1
fi
BUILDFILESTR="-f ${BUILDFILE}"

repodir=$2
if [[ -z "${repodir}" ]]
then
    print "/n/t%s/t%s/n" "ERROR" "repodir must be specified as second argument"
    exit 1
fi
if [[ ! -e "${repodir}" ]]
then
    print "/n/t%s/t%s/n" "ERROR" "repodir does not exist: ${repodir}"
    exit 1
fi

complocation=$3
if [[ -z "${complocation}" ]]
then
    print "/n/t%s/t%s/n" "ERROR" "complocation must be specified as third argument"
    exit 1
fi


REPODIRSTR="-Drepodir=${repodir}"
COMPLOCATION="-Dcomplocation=${complocation}"

devArgs="$devArgs $REPODIRSTR $COMPLOCATION"

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
if [[ -d ${compdir} ]]
then
    rm -fr "${compdir}"
    RC=$?
    if [[ $RC != 0 ]]
    then
        echo "remove failed: ${compdir}: $RC"
        exit $RC
    fi
else
    echo "ERROR: expected directory did not exist: ${compdir}"
    exit 1
fi

exit 0

