#!/usr/bin/env bash

# Utility script to get "ebuilder"

BUILD_DIR=$1
EBUILDER_HASH=$2


if [[ -z "${BUILD_DIR}" ]]
then
    echo "BUILD_DIR not supplied, will assume current directory, for testing."
    BUILD_DIR=${PWD}
else
    # normally will exist by now, but if not, we'll create it.
    if [[ ! -d "${BUILD_DIR}" ]]
    then
        echo "WARNING: BUILD_DIR did not exist, so created it: $BUILD_DIR."
        mkdir -p $BUILD_DIR
    fi
fi

if [[ -z "${EBUILDER_HASH}" ]]
then 
    echo "EBUILDER HASH, BRANCH, or TAG was not supplied, assuming 'master'"
    EBUILDER_HASH=master 
fi

EBUILDER=eclipse.platform.releng.aggregator

RC=0
# don't clone, if already exists. 
if [[ ! -d ${BUILD_DIR}/${EBUILDER} ]]
then
    # Not sure 'REPO_AND_ACCESS' is defined in all possible scenerios, so we'll provide a default. 
    # It is in main scenerios, but not sure about things like "re-running unit tests at a later time".
    git clone ${REPO_AND_ACCESS:-git://git.eclipse.org/gitroot}/platform/${EBUILDER} ${BUILD_DIR}/${EBUILDER}
    RC=$?
    if [[ $RC != 0 ]] 
    then
       echo "[ERROR] Cloning EBUILDER returned non zero return code: $RC"
       exit $RC
     fi
else 
    echo "INFO: ebuilder directory found to exist. Not re-cloneing."
    echo "INFO:    Found: ${BUILD_DIR}/${EBUILDER}"
    echo "INFO:    But fetching to make sure up to date,"
    echo "INFO:    before checking out specific HASH (which may make it detached)."
    pushd ${BUILD_DIR}/${EBUILDER}
    git fetch
    RC=$?
    if [[ $RC != 0 ]] 
    then
       echo "[ERROR] Fetch EBUILDER returned non zero return code: $RC"
       exit $RC
     fi
    git checkout $EBUILDER_HASH
        RC=$?
    if [[ $RC != 0 ]] 
    then
       echo "[ERROR] Checking out EBUILDER for $EBUILDER_HASH returned non zero return code: $RC"
       exit $RC
     fi
    popd
fi 

exit $RC

