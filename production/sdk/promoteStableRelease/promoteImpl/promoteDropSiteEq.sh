#!/usr/bin/env bash
#*******************************************************************************
# Copyright (c) 2016 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     David Williams - initial API and implementation
#*******************************************************************************

currentDropId=$1
currentBuildLabelEQ=$2
DL_LABEL=$3
HIDE_SITE=$4

function usage ()
{
  printf "\n\tUsage: %s currentDropId currentBuildLabelEQ DL_LABEL HIDE_SITE" $(basename $0) >&2
  printf "\n\t\t%s\t%s" "currentDropId " "such as I20121031-2000." >&2
  printf "\n\t\t%s\t%s" "currentBuildLabelEQ " "such as I20121031-2000 or Mars.1RC3" >&2
  printf "\n\t\t%s\t%s" "DL_LABEL " "such as LunaM3." >&2
  printf "\n\t\t%s\t%s" "HIDE_SITE " "true or false." >&2
}

if [[ -z "${currentDropId}" || -z "${DL_LABEL}" || -z "${HIDE_SITE}" ]]
then
  printf "\n\n\t%s\n\n" "ERROR: arguments missing in call to $( basename $0 )" >&2
  usage
  exit 1
fi

DL_DROP_ID=${DL_TYPE}-${DL_LABEL}-${BUILD_TIMESTAMP}

cd ${BUILDMACHINE_BASE_EQ}

printf "\n\n\t%s\n" "Making promote script for Equinox"

printf "\n\t%s\n\t%s to \n\t%s\n" "Making backup copy of original ..." "$currentDropId" "${currentDropId}ORIG"
rsync -ra ${currentDropId}/ ${currentDropId}ORIG

printf "\n\t%s\n" "Doing rename of original."

# if DL_currentDropId already exists, it is from a previous run we are re-doing, do,
# we'll remove first, to make sure it's cleaning re-done.
if [[ -d ${DL_DROP_ID} ]]
then
  echo -e "\n\tWARNING: found and will remove existing, previous, version of ${DL_DROP_ID}"
  rm -fr ${DL_DROP_ID}
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo -e "\n\tERROR: Could not remove previous (failed) version of DL_DROP_ID, ${DL_DROP_ID}"
    exit 1
  fi
fi

${PROMOTE_IMPL}/renameBuild.sh ${currentDropId} ${currentBuildLabelEQ} ${DL_DROP_ID} ${DL_LABEL}
RC=$?
if [[ $RC != 0 ]] 
then 
  echo "ERROR: renameBuild.sh returned non-zero return code: $RC."
  exit $RC
fi

printf "\n\t%s\n" "Moving backup copy back to original."
mv ${currentDropId}ORIG ${currentDropId}


PROMOTE_PREFIX="promote"
MANUAL_PREFIX="manual-${PROMOTE_PREFIX}"

# assume "hide site" is always true (which it is, in practice) to simplify following logic. 
# can improve later if needed. 
#if [[ "${HIDE_SITE}" == "true" ]]
#then

# add buildHidden to "local" (buildMachine) directory
touch ${BUILDMACHINE_BASE_EQ}/${DL_DROP_ID}/buildHidden
# make "deferred" script to remove buildHidden later
PROMOTE_VARIABLE=${MANUAL_PREFIX}
DEF_PFILE="${UTILITIES_HOME}/equinox/promotion/queue/${PROMOTE_VARIABLE}-${DL_LABEL}.sh"
echo "Remember to change Equinox promote script name from ${MANUAL_PREFIX} to ${PROMOTE_PREFIX} when time to promote." >> "${CL_SITE}/checklist.txt"
echo "mv  /home/data/httpd/download.eclipse.org/equinox/drops/${DL_DROP_ID}/buildHidden" \
  "/home/data/httpd/download.eclipse.org/equinox/drops/${DL_DROP_ID}/buildHiddenORIG" \
  > ${DEF_PFILE}
#else
PROMOTE_VARIABLE=${PROMOTE_PREFIX}
IMMED_PFILE="${UTILITIES_HOME}/equinox/promotion/queue/${PROMOTE_VARIABLE}-${DL_LABEL}.sh"
echo "# Script for immediate promotion" > ${IMMED_PFILE}
#fi

printf "\n\t%s\n" "Creating promote script."
echo "rsync -r ${BUILDMACHINE_BASE_EQ}/${DL_DROP_ID} /home/data/httpd/download.eclipse.org/equinox/drops/" \
  >> ${IMMED_PFILE}

# if doing a release, go ahead and archive too.
# TODO: make deferred
if [[ "${DL_TYPE}" == "R" ]]
then
  printf "\n\t%s\n" "Creating archive script."
  echo "rsync -r ${BUILDMACHINE_BASE_EQ}/${DL_DROP_ID} /home/data/httpd/archive.eclipse.org/equinox/drops/" \
    >> ${IMMED_PFILE}
fi

printf "\n\t%s\n" "Making sure Equinox promote scripts is executable ..."
chmod -c +x ${IMMED_PFILE}
chmod -c +x ${DEF_PFILE}
