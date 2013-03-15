#!/usr/bin/env bash

DROP_ID=$1
DL_LABEL=$2

function usage ()
{
    printf "\n\tUsage: %s DROP_ID DL_LABEL " $(basename $0) >&2
    printf "\n\t\t%s\t%s" "DROP_ID " "such as I20121031-2000." >&2
    printf "\n\t\t%s\t%s" "DL_LABEL " "such as 4.3M3." >&2
}

if [[ -z "${DROP_ID}" || -z "${DL_LABEL}" ]]
then
    printf "\n\n\t%s\n\n" "ERROR: arguments missing in call to $( basename $0 )" >&2
    usage
    exit 1
fi

DL_TYPE=S
BUILD_TIMESTAMP=${DROP_ID//[MI-]/}
DL_DROP_ID=${DL_TYPE}-${DL_LABEL}-${BUILD_TIMESTAMP}

cd /shared/eclipse/builds/4I/siteDir/eclipse/downloads/drops4
cp /shared/eclipse/sdk/renameBuild.sh ${PWD}

rsync -ra ${DROP_ID}/ ${DROP_ID}ORIG

./renameBuild.sh ${DROP_ID} ${DL_DROP_ID} ${DL_LABEL}

mv ${DROP_ID}ORIG ${DROP_ID}

rm renameBuild.sh

# Here we can rsync with committer id. For Equinox, we have to create a promotion file.
rsync -r ${DL_DROP_ID} /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4/
rccode=$?

if [ $rccode -eq 0 ]
then
    source /shared/eclipse/sdk/updateIndexFilesFunction.shsource
    updateIndex 4
else
    printf "\n\n\t%s\n\n" "ERROR: rsync failed. rccode: $rccode" >&2
    exit $rccode
fi

