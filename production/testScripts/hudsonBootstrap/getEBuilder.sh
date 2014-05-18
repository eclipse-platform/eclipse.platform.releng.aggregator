#!/usr/bin/env bash

# Utility script to "bootstrap" Hudson Eclipse Platform Unit tests, to get the
# basic files needed to get all the other required files and start the test framework.

#BUILD_KIND=$1
#EBUILDER_HASH=$2
#WORKSPACE=$3

if [[ -z "${WORKSPACE}" ]]
then
  echo "WORKSPACE not supplied, will assume current directory"
  WORKSPACE=${PWD}
else
  if [[ ! -d "${WORKSPACE}" ]]
  then
    echo "ERROR: WORKSPACE did not exist. Perhaps you meant its parent?"
    exit 1
  fi
fi

if [[ -z "${EBUILDER_HASH}" ]]
then
  echo "EBUILDER HASH, BRANCH, or TAG was not supplied, assuming 'master'"
  EBUILDER_HASH=master
fi

if [[ -z "${BUILD_KIND}" ]]
then
  echo "BUILD_KIND not supplied, assuming CBI"
  BUILD_KIND=CBI
fi

# remove just in case left from previous failed run
rm ebuilder.zip 2>/dev/null
rm -fr tempebuilder 2>/dev/null

if [[ "${BUILD_KIND}" == "CBI" ]]
then
  EBUILDER=eclipse.platform.releng.aggregator
  TARGETNAME=eclipse.platform.releng.aggregator
  ESCRIPT_LOC=${EBUILDER}/production/testScripts
elif [[ "$BUILD_KIND" == "PDE" ]]
then
  EBUILDER=eclipse.platform.releng.eclipsebuilder
  TARGETNAME=org.eclipse.releng.eclipsebuilder
  ESCRIPT_LOC=${TARGETNAME}
else
  echo "ERROR: Unexpected value of BUILD_KIND: ${BUILD_KIND}"
  exit 1
fi

wget -O ebuilder.zip --no-verbose http://git.eclipse.org/c/platform/${EBUILDER}.git/snapshot/${EBUILDER}-${EBUILDER_HASH}.zip 2>&1
unzip -q ebuilder.zip -d tempebuilder
mkdir -p ${WORKSPACE}/$TARGETNAME
rsync --recursive "tempebuilder/${EBUILDER}-${EBUILDER_HASH}/" "${WORKSPACE}/${TARGETNAME}/"
rccode=$?
if [[ $rccode != 0 ]]
then
  echo "ERROR: rsync did not complete normally.rccode: $rccode"
  exit $rccode
fi

# copy to well-known location so subsequent steps do not need to know which ebuilder they came from
cp ${WORKSPACE}/${EBUILDER}/eclipse.platform.releng.tychoeclipsebuilder/eclipse/getBaseBuilder.xml ${WORKSPACE}
cp ${WORKSPACE}/${ESCRIPT_LOC}/runTests2.xml ${WORKSPACE}

# remove on clean exit
rm ebuilder.zip
rm -fr tempebuilder
exit 0

