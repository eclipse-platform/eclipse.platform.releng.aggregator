#!/usr/bin/env bash

# Utility script to "bootstrap" Hudson Eclipse Platform Unit tests, to get the
# basic files needed to get all the other required files and start the test framework.

EBUILDER_HASH=$1
WORKSPACE=$2

if [[ -z "${WORKSPACE}" ]]
then
  echo "WORKSPACE not supplied, will assume current directory"
  WORKSPACE=${PWD}
else
  if [[ ! -d "${WORKSPACE}" ]]
  then
    echo "ERROR: WORKSPACE did not exist."
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
# TODO: May need to provide a "force" parameter to use when testing?
if [[ ! -d ${WORKSPACE}/${TARGETNAME} ]]
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
  mkdir -p ${WORKSPACE}/${TARGETNAME}
  rsync --recursive "tempebuilder/${EBUILDER}-${EBUILDER_HASH}/" "${WORKSPACE}/${TARGETNAME}/"
  rccode=$?
  if [[ $rccode != 0 ]]
  then
    echo "ERROR: rsync did not complete normally.rccode: $rccode"
    exit $rccode
  fi
else
  echo "INFO: ebuilder directory found to exist. Not re-fetching."
  echo "INFO:    ${WORKSPACE}/${TARGETNAME}"
fi
# copy to well-known location so subsequent steps do not need to know which ebuilder they came from
#cp ${WORKSPACE}/${ESCRIPT_LOC}/getBaseBuilder.xml ${WORKSPACE}
#cp ${WORKSPACE}/${ESCRIPT_LOC}/runTests2.xml ${WORKSPACE}

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

