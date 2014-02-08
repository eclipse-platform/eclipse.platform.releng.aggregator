#!/usr/bin/env bash

# Utility to get basic "startup" files for Platform's CBI Build

# this buildeclipse.shsource file is to ease local builds to override some variables. 
# It should not be used for production builds.
source buildeclipse.shsource 2>/dev/null
export BUILD_HOME=${BUILD_HOME:-/shared/eclipse/builds}

function usage () 
{
    printf "\n\t%s\n" "$( basename $0 ) <BRANCH> <BUILD_TYPE> <STREAM> where"
    printf "\t\t%s\t%s\n" "BRANCH:" "BRANCH to build, such as master, R4_2_mainenance, R3_8_maintenance"
    printf "\t\t%s\t%s\n" "BUILD_TYPE:" "M, I, N"
    printf "\t\t%s\t%s\n" "STREAM: " "eclipse release being built, such as 4.3.0, 4.2.2, 3.8.2"
}

if [[ $# != 3 ]] 
then
    usage
    exit 1
fi

export BRANCH=${BRANCH:-"${1}"}
export BUILD_TYPE=${BUILD_TYPE:-"${2}"}
export STREAM=${STREAM:-"${3}"}

reponame=eclipse.platform.releng.aggregator

# contrary to intuition (and previous behavior, bash 3.1) do NOT use quotes around right side of expression.
if [[ "${STREAM}" =~ ^([[:digit:]]+)\.([[:digit:]]+)\.([[:digit:]]+)$ ]]
then
    export STREAMMajor=${BASH_REMATCH[1]}
    export STREAMMinor=${BASH_REMATCH[2]}
    export STREAMService=${BASH_REMATCH[3]}
else
    echo "STREAM must contain major, minor, and service versions, such as 4.3.0"
    echo "    but found ${STREAM}"
    exit 1
fi

if [[ ! "${BUILD_TYPE}" =~ [IMNPXY] ]]
then
    echo "BUILD_TYPE must by I,M, N, P, X, or Y"
    echo "    but found ${BUILD_TYPE}"
    exit 1
fi

# if not defined "externally", we use default for eclipse.org
if [[ -z $REPO_AND_ACCESS ]]
then
    # unless we are on 'build' machine
    if [[ "build" == "$(hostname)" ]]
    then
        export REPO_AND_ACCESS=file:///gitroot
    else
        export REPO_AND_ACCESS=git://git.eclipse.org/gitroot
    fi
fi


export BUILD_ROOT=${BUILD_ROOT:-${BUILD_HOME}/${STREAMMajor}${BUILD_TYPE}}
echo "Exporting production scripts ... "
echo "  STREAM: $STREAM"
echo "  STREAMMajor: $STREAMMajor"
echo "  STREAMMinor: $STREAMMinor"
echo "  STREAMService: $STREAMService"
echo "  BUILD_TYPE: $BUILD_TYPE"
echo "  BUILD_ROOT: $BUILD_ROOT"
echo "  BUILD_HOME: $BUILD_HOME"
echo "  PRODUCTION_SCRIPTS_DIR: $PRODUCTION_SCRIPTS_DIR"
echo "  REPO_AND_ACCESS: $REPO_AND_ACCESS"

# We put these in build_root in case two builds get started at once. 
# (such as N and I build, both from master).  

# TODO: could maybe do pull here, if already exists?
rm -fr $BUILD_ROOT/tmp/eclipse.platform.releng.aggregator 2>/dev/null
if [[ $? != 0 ]] 
then
    echo "[ERROR] Exiting, since could not remove $BUILD_ROOT/tmp/eclipse.platform.releng.aggregator as expected."
    exit 1
fi

# Make dir in case first run (note: this must be after the above "removes", 
# or else first time through they will remove this directory itself. 
mkdir -p ${BUILD_ROOT}/tmp
if [[ $? != 0 ]] 
then
    echo "[ERROR] Exiting, since could not make $BUILD_ROOT/tmp, as expected."
    exit 1
fi


git clone -b ${BRANCH} ${REPO_AND_ACCESS}/platform/eclipse.platform.releng.aggregator $BUILD_ROOT/tmp/eclipse.platform.releng.aggregator
RC=$?
if [[ $RC != 0 ]]
then 
    echo "Could not clone repo as expected"
    exit $RC
fi

#remove any previous production scripts, to make sure completely fresh
rm -fr $BUILD_ROOT/${PRODUCTION_SCRIPTS_DIR} 2>/dev/null

# cp whole script directory "up" so directly under build_root, in constant place
cp -r $BUILD_ROOT/tmp/eclipse.platform.releng.aggregator/${PRODUCTION_SCRIPTS_DIR} ${BUILD_ROOT}

if [[ $? != 0 ]] 
then
    echo "Exiting, since could not copy production scripts, as expected."
    exit 1
fi

chmod +x ${BUILD_ROOT}/${PRODUCTION_SCRIPTS_DIR}/*.sh

if [[ $? != 0 ]] 
then
    echo "Could not chmod of production scripts to executable. Running under wrong id?"
    exit 1
fi

