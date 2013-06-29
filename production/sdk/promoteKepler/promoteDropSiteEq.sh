#!/usr/bin/env bash

DROP_ID=$1
DL_LABEL=$2
HIDE_SITE=$3

function usage ()
{
    printf "\n\tUsage: %s DROP_ID DL_LABEL HIDE_SITE" $(basename $0) >&2
    printf "\n\t\t%s\t%s" "DROP_ID " "such as I20121031-2000." >&2
    printf "\n\t\t%s\t%s" "DL_LABEL " "such as KeplerM3." >&2
    printf "\n\t\t%s\t%s" "HIDE_SITE " "true or false." >&2
}

if [[ -z "${DROP_ID}" || -z "${DL_LABEL}" || -z "${HIDE_SITE}" ]]
then
    printf "\n\n\t%s\n\n" "ERROR: arguments missing in call to $( basename $0 )" >&2
    usage
    exit 1
fi

DL_DROP_ID=${DL_TYPE}-${DL_LABEL}-${BUILD_TIMESTAMP}

cd /shared/eclipse/builds/4I/siteDir/equinox/drops
cp /shared/eclipse/sdk/renameBuild.sh .

printf "\n\n\t%s\n" "Making promote script for Equinox"

printf "\n\t%s\n\t%s to \n\t%s\n" "Making backup copy of original ..." "$DROP_ID" "${DROP_ID}ORIG"
rsync -ra ${DROP_ID}/ ${DROP_ID}ORIG

printf "\n\t%s\n" "Doing rename of original."
./renameBuild.sh ${DROP_ID} ${DL_DROP_ID} ${DL_LABEL}

printf "\n\t%s\n" "Moving backup copy back to original."
mv ${DROP_ID}ORIG ${DROP_ID}

rm renameBuild.sh

PROMOTE_PREFIX="promote"
if [[ "${HIDE_SITE}" == "true" ]]
then
   PROMOTE_PREFIX="manual"
   echo "Remember to change Equinox promote script name from 'manual-' to 'promote-' when time to promote." >> "${CL_SITE}/checklist.txt" 
fi 

printf "\n\t%s\n" "Creating promote script."
echo "rsync -r /shared/eclipse/builds/4I/siteDir/equinox/drops/${DL_DROP_ID} /home/data/httpd/download.eclipse.org/equinox/drops/" \
    > /shared/eclipse/equinox/promotion/queue/${PROMOTE_PREFIX}-${DL_LABEL}.sh

printf "\n\t%s\n" "Make sure promote script is 'executable'."
chmod +x /shared/eclipse/equinox/promotion/queue/${PROMOTE_PREFIX}-${DL_LABEL}.sh

