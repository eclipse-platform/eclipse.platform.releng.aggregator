#!/usr/bin/env bash

if [[ $# != 3 ]]
then
  echo "ERROR: This script requires 3 parameters, in order:"
  echo "        BUILD_ID, such as I20120608-1200"
  echo "        DL_LABEL, such as 4.2M7, 4.2RC4, 4.2"
  echo "        DROP_TYPE, either S, R"
  exit 1
fi

BUILD_ID=$1
DL_LABEL=$2
DROP_TYPE=$3

echo "BUILD_ID: ${BUILD_ID}"
echo "DL_LABEL: ${DL_LABEL}"
echo "DROP_TYPE: ${DROP_TYPE}"

DROP_SITE_LABEL=${DROP_TYPE}-${DL_LABEL}

BUILD_TIMESTAMP=${BUILD_ID//[IM-]/}

DL_SITE_ID="${DROP_SITE_LABEL}"-"${BUILD_TIMESTAMP}"

# first character taken as build type (should be I or M)
BUILD_TYPE=${BUILD_ID:0:1}
if [[ $BUILD_TYPE != I && $BUILD_TYPE != M ]]
then
    echo "ERROR: BUILD_TYPE was unexected: ${BUILD_TYPE}"
    exit 1
fi

# first character taken as build major number (should be 3 or 4, for a long long time)
BUILD_MAJOR=${DL_LABEL:0:1}
if [[ $BUILD_MAJOR != 3 && $BUILD_MAJOR != 4 ]]
then
    echo "ERROR: BUILD_MAJOR number was unexected: ${BUILD_MAJOR}"
    exit 1
fi
# third character taken as build minor number (should be 8 or 2, for now)
BUILD_MINOR=${DL_LABEL:2:1}
if [[ $BUILD_MINOR != 8 && $BUILD_MINOR != 2 ]]
then
    echo "ERROR: BUILD_MINOR number was unexected: ${BUILD_MINOR}"
    exit 1
fi

BUILDMACHINE_BASE_SITE=/opt/public/eclipse/eclipse${BUILD_MAJOR}${BUILD_TYPE}/siteDir/updates/${BUILD_MAJOR}.${BUILD_MINOR}-${BUILD_TYPE}-builds

DLMACHINE_BASE_SITE=/home/data/httpd/download.eclipse.org/eclipse/updates/${BUILD_MAJOR}.${BUILD_MINOR}milestones
if [[ ${DROP_TYPE} == "R" ]]
then
 DLMACHINE_BASE_SITE=/home/data/httpd/download.eclipse.org/eclipse/updates/${BUILD_MAJOR}.${BUILD_MINOR}
fi

BUILDMACHINE_SITE=${BUILDMACHINE_BASE_SITE}/${BUILD_ID}

DLMACHINE_SITE=${DLMACHINE_BASE_SITE}/${DL_SITE_ID}

# remember, need trailing slash since going from existing directories
# contents to new directories contents
echo "BUILDMACHINE_SITE: ${BUILDMACHINE_SITE}/"
echo "DLMACHINE_SITE: ${DLMACHINE_SITE}"
rsync -vr "${BUILDMACHINE_SITE}/"  "${DLMACHINE_SITE}"

echo " ... remember to update composite files ... "

