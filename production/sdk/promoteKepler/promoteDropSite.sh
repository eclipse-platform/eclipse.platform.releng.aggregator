#!/usr/bin/env bash

DROP_ID=$1
DL_LABEL=$2
HIDE_SITE=$3

function usage ()
{
    printf "\n\tUsage: %s DROP_ID DL_LABEL HIDE_SITE" $(basename $0) >&2
    printf "\n\t\t%s\t%s" "DROP_ID " "such as I20121031-2000." >&2
    printf "\n\t\t%s\t%s" "DL_LABEL " "such as 4.3M3." >&2
    printf "\n\t\t%s\t%s" "HIDE_SITE " "true or false." >&2
}

if [[ -z "${DROP_ID}" || -z "${DL_LABEL}" || -z "${HIDE_SITE}" ]]
then
    printf "\n\n\t%s\n\n" "ERROR: arguments missing in call to $( basename $0 )" >&2
    usage
    exit 1
fi

DL_DROP_ID=${DL_TYPE}-${DL_LABEL}-${BUILD_TIMESTAMP}

cd /shared/eclipse/builds/4I/siteDir/eclipse/downloads/drops4
cp /shared/eclipse/sdk/renameBuild.sh ${PWD}

printf "\n\n\t%s\n" "Promoting Eclipse site."

printf "\n\t%s\n\t%s to \n\t%s\n" "Making backup copy of original ..." "$DROP_ID" "${DROP_ID}ORIG"
rsync -ra ${DROP_ID}/ ${DROP_ID}ORIG

printf "\n\t%s\n" "Doing rename of original."
./renameBuild.sh ${DROP_ID} ${DL_DROP_ID} ${DL_LABEL}

printf "\n\t%s\n" "Moving backup copy back to original."
mv ${DROP_ID}ORIG ${DROP_ID}

rm renameBuild.sh

# keep hidden, initially, both to confirm all is correct, 
# and in theory could wait a bit to get a mirror or two 
# (in some cases). 
if [[ "${HIDE_SITE}" == "true" ]]
then
   touch ${DL_DROP_ID}/buildHidden
   if [[ $? != 0 ]] 
   then 
       echo "touch failed. Exiting."
       exit 1
   fi
   echo "Remember to remove 'buildHidden' file, and re-run updateIndexes.sh since HIDE_SITE was ${HIDE_SITE}." >> "${CL_SITE}/checklist.txt"
else
   echo "HIDE_SITE value was ${HIDE_SITE}"
fi

printf "\n\t%s\n" "rsync to downloads."
# Here we can rsync with committer id. For Equinox, we have to create a promotion file.
rsync --recursive --exclude="*apitoolingreference*" --exclude="*org.eclipse.releng.basebuilder*" --exclude="*eclipse.platform.releng.aggregator*" --exclude="*repository*" --exclude="*workspace-*" ${DL_DROP_ID} /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4/
rccode=$?
if [ $rccode -eq 0 ]
then
    printf "\n\t%s\n" "Update main overall download index page so it shows new build."
    source /shared/eclipse/sdk/updateIndexFilesFunction.shsource
    updateIndex 4 MAIN
else
    printf "\n\n\t%s\n\n" "ERROR: rsync failed. rccode: $rccode" >&2
    exit $rccode
fi

