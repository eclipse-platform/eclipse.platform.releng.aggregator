#!/usr/bin/env bash

# Utility script to get "ebuilder"

BUILD_DIR=$1
EBUILDER_HASH=$2


if [[ -z "${BUILD_DIR}" ]]
then
    echo "BUILD_DIR not supplied, will assume current directory, for testing."
    BUILD_DIR=${PWD}
else
    if [[ ! -d "${BUILD_DIR}" ]]
    then
        echo "ERROR: BUILD_DIR did not exist."
        exit 1
    fi
fi

if [[ -z "${EBUILDER_HASH}" ]]
then 
    echo "EBUILDER HASH, BRANCH, or TAG was not supplied, assuming 'master'"
    EBUILDER_HASH=master 
fi

EBUILDER=eclipse.platform.releng.aggregator
TARGETNAME=eclipse.platform.releng.aggregator
ESCRIPT_LOC=${EBUILDER}/production/testScripts

# don't re-fetch, if already exists. 
if [[ ! -d ${BUILD_DIR}/${TARGETNAME} ]]
then
    # remove just in case left from previous failed run
    # if they exist
    if [[ -f ebuilder.zip ]]
    then 
        rm ebuilder.zip
    fi 
    if [[ -d tempebuilder ]]
    then
        rm -fr tempebuilder
    fi
    wget -O ebuilder.zip --no-verbose http://git.eclipse.org/c/platform/${EBUILDER}.git/snapshot/${EBUILDER}-${EBUILDER_HASH}.zip 2>&1
    unzip -q ebuilder.zip -d tempebuilder
    mkdir -p ${BUILD_DIR}/${TARGETNAME}
    rsync --recursive "tempebuilder/${EBUILDER}-${EBUILDER_HASH}/" "${BUILD_DIR}/${TARGETNAME}/"
    rccode=$? 
    if [[ $rccode != 0 ]]
    then
        echo "ERROR: rsync did not complete normally in $0. rccode: $rccode"
        exit $rccode
    fi
else 
    echo "INFO: ebuilder directory found to exist. Not re-fetching."
    echo "INFO:    Found: ${BUILD_DIR}/${TARGETNAME}"
fi 

# remove on clean exit, if they exist
if [[ -f ebuilder.zip ]]
then 
    rm ebuilder.zip
fi 
if [[ -d tempebuilder ]]
then
    rm -fr tempebuilder
fi
exit 0

