#!/usr/bin/env bash

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
    echo -e "/n/tERROR: Could not remove previous (failed) version of DL_DROP_ID, ${DL_DROP_ID}"
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
MANUAL_PREFIX="manual-promote"
if [[ "${HIDE_SITE}" == "true" ]]
then
  # touch buildHidden
  touch ${BUILDMACHINE_BASE_EQ}/${DL_DROP_ID}/buildHidden
  # make "deferred" script to remove buildHidden
  PROMOTE_PREFIX="manual-promote"
  echo "Remember to change Equinox promote script name from ${MANUAL_PREFIX} to ${PROMOTE_PREFIX} when time to promote." >> "${CL_SITE}/checklist.txt"
  echo "rm  /home/data/httpd/download.eclipse.org/equinox/drops/${DL_DROP_ID}/buildHidden" \
  > /shared/eclipse/equinox/promotion/queue/${MANUAL_PREFIX}-${DL_LABEL}.sh
fi

printf "\n\t%s\n" "Creating promote script."
echo "rsync -r ${BUILDMACHINE_BASE_EQ}/${DL_DROP_ID} /home/data/httpd/download.eclipse.org/equinox/drops/" \
  > /shared/eclipse/equinox/promotion/queue/${PROMOTE_PREFIX}-${DL_LABEL}.sh

printf "\n\t%s\n" "Make sure promote scripts are 'executable'."
chmod +x "/shared/eclipse/equinox/promotion/queue/*${DL_LABEL}.sh"

