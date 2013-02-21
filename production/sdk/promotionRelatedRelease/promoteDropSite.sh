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

# BUILD_TYPE, I or M, inferred from first letter of BUILD_ID
# is build type on build machine.
# DROP_TYPE from input, is what we are promoting to
# I build's go to S or R. M builds go only to R (release).

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

# remove letter I,M and hypen to form pure timestamp for end of drop id
BUILD_TIMESTAMP=${BUILD_ID//[MI-]/}

DL_DROP_ID=${DROP_TYPE}-${DL_LABEL}-${BUILD_TIMESTAMP}

DL_SITE_PATH=/home/data/httpd/download.eclipse.org/eclipse/downloads/drops/
if [[ ${BUILD_MAJOR} == 4 ]]
then
    DL_SITE_PATH=/home/data/httpd/download.eclipse.org/eclipse/downloads/drops4/
fi

# sanity check
if [[ ! -d ${DL_SITE_PATH} ]]
then
    echo "ERROR: DL_SITE_PATH does not exist?: ${DL_SITE_PATH}"
    exit 1
fi
# note: always working from "I build" even when going from RC to S
# so any "custom hand editing" to RC build has to be re-done.
BUILD_DIR=/opt/public/eclipse/eclipse${BUILD_MAJOR}${BUILD_TYPE}/siteDir/eclipse/downloads/drops
if [[ ${BUILD_MAJOR} == 4 ]]
then
    BUILD_DIR=/opt/public/eclipse/eclipse${BUILD_MAJOR}${BUILD_TYPE}/siteDir/eclipse/downloads/drops${BUILD_MAJOR}
fi

# sanity check
if [[ ! -d ${BUILD_DIR} ]]
then
    echo "ERROR: BUILD_DIR does not exist?: ${BUILD_DIR}"
    exit 1
fi




# change to parent of drop, and work from there
# (rename build assumes it)
cd ${BUILD_DIR}
echo "PWD: ${PWD}"
cp /opt/public/eclipse/sdk/renameBuild.sh .

RC=$?
if [[ $RC != 0 ]]
then
    echo "ERROR: copy of renameBuild.sh returned non-zero return code: $RC"
    exit $RC
fi

echo "save temp backup copy to ${BUILD_ID}ORIG"
rsync -ra ${BUILD_ID}/ ${BUILD_ID}ORIG
RC=$?
if [[ $RC != 0 ]]
then
    echo "ERROR: backup of original returned non-zero return code: $RC"
    exit $RC
fi

echo "rename ${BUILD_ID} ${DL_DROP_ID} ${DL_LABEL}"
./renameBuild.sh ${BUILD_ID} ${DL_DROP_ID} ${DL_LABEL}
RC=$?
if [[ $RC != 0 ]]
then
    echo "ERROR: renameBuild.sh returned non-zero return code: $RC"
    exit $RC
fi


# keep releases hidden until release day
if [[ ${DROP_TYPE} == "R" ]]
then
  touch ${DL_DROP_ID}/buildHidden
fi

# note to maintain times (-t), so future rsyncs can be more efficient,
# EXCEPT for directories (-O) which allows mirrors
# system to work as intended
echo "rsync ${DL_DROP_ID} to ${DL_SITE_PATH}"
rsync -r -t -O ${DL_DROP_ID} ${DL_SITE_PATH}
RC=$?
if [[ $RC != 0 ]]
then
    echo "ERROR: rsync from build machine to downloads returned non-zero return code: $RC"
    exit $RC
fi

# no need to update of index, bug with "buildHidden" file in place, should not show up
# note: updating the index is a minor operation, no need to check for error.
if [[ ${DROP_TYPE} != "R" ]]
then
    source /shared/eclipse/sdk/updateIndexFilesFunction.shsource
    updateIndex ${BUILD_MAJOR}
fi

# if we get to here without error exit, then ok to move back to original name, and remove
# working files
echo "move backup back to original"
mv ${BUILD_ID}ORIG ${BUILD_ID}

rm renameBuild.sh

