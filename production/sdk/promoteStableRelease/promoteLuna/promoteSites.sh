#!/usr/bin/env bash

DROP_ID=I20130918-2000

DL_LABEL=4.4M2
DL_LABEL_EQ=LunaM2

# for I builds, stable and RCs to in milestones
REPO_SITE_SEGMENT=4.4milestones
#REPO_SITE_SEGMENT=4.4

HIDE_SITE=true
#HIDE_SITE=false

export CL_SITE=${PWD}
echo "CL_SITE: ${CL_SITE}"

# These are what precedes main drop directory name
export DL_TYPE=S
#export DL_TYPE=R
#export DL_TYPE=M

# Used in naming repo, etc
export TRAIN_NAME=Luna

# Build machine locations (would very seldome change)
export BUILD_ROOT=/shared/eclipse/builds/4I
export BUILDMACHINE_BASE_SITE=${BUILD_ROOT}/siteDir/updates/4.4-I-builds

export BUILDMACHINE_BASE_DL=${BUILD_ROOT}/siteDir/eclipse/downloads/drops4
export BUILDMACHINE_BASE_EQ=${BUILD_ROOT}/siteDir/equinox/drops

export PROMOTE_IMPL=/shared/eclipse/sdk/promoteStableRelease/promoteImpl
export BUILD_TIMESTAMP=${DROP_ID//[MI-]/}

# Eclipse Drop Site (final segment)
ECLIPSE_DL_DROP_DIR_SEGMENT=${DL_TYPE}-${DL_LABEL}-${BUILD_TIMESTAMP}
# Equinox Drop Site (final segment)
EQUINOX_DL_DROP_DIR_SEGMENT=${DL_TYPE}-${DL_LABEL_EQ}-${BUILD_TIMESTAMP}

printf "\n\t%s\n\n" "Promoted on: $( date )" > "${CL_SITE}/checklist.txt"
printf "\n\t%20s%25s\n" "DROP_ID" "$DROP_ID" >> "${CL_SITE}/checklist.txt"
printf "\t%20s%25s\n" "DL_LABEL" "$DL_LABEL" >> "${CL_SITE}/checklist.txt"
printf "\t%20s%25s\n" "DL_LABEL_EQ" "$DL_LABEL_EQ" >> "${CL_SITE}/checklist.txt"
printf "\t%20s%25s\n" "REPO_SITE_SEGMENT" "$REPO_SITE_SEGMENT" >> "${CL_SITE}/checklist.txt"
printf "\t%20s%25s\n\n" "HIDE_SITE" "${HIDE_SITE}" >> "${CL_SITE}/checklist.txt"

printf "\t%s\n" "Eclipse downloads:" >> "${CL_SITE}/checklist.txt"
printf "\t%s\n\n" "http://download.eclipse.org/eclipse/downloads/drops4/${ECLIPSE_DL_DROP_DIR_SEGMENT}/" >> "${CL_SITE}/checklist.txt"

printf "\t%s\n" "Update existing (non-production) installs:" >> "${CL_SITE}/checklist.txt"
printf "\t%s\n\n" "http://download.eclipse.org/eclipse/updates/${REPO_SITE_SEGMENT}/" >> "${CL_SITE}/checklist.txt"

printf "\t%s\n" "Specific repository good for building against:" >> "${CL_SITE}/checklist.txt"
printf "\t%s\n\n" "http://download.eclipse.org/eclipse/updates/${REPO_SITE_SEGMENT}/${ECLIPSE_DL_DROP_DIR_SEGMENT}/" >> "${CL_SITE}/checklist.txt"

printf "\t%s\n" "Equinox specific downloads:" >> "${CL_SITE}/checklist.txt"
printf "\t%s\n\n" "http://download.eclipse.org/equinox/drops/${EQUINOX_DL_DROP_DIR_SEGMENT}/" >> "${CL_SITE}/checklist.txt"


# we do Equinox first, since it has to wait in que until 
# cronjob promotes it
${PROMOTE_IMPL}/promoteDropSiteEq.sh ${DROP_ID} ${DL_LABEL_EQ} ${HIDE_SITE}
rccode=$?
if [[ $rccode != 0 ]]
then
    printf "\n\n\t%s\n\n" "ERROR: promoteDropSiteEq.sh failed. Subsequent promotion cancelled."
    exit $rccode
fi

${PROMOTE_IMPL}/promoteDropSite.sh   ${DROP_ID} ${DL_LABEL} ${HIDE_SITE}
rccode=$?
if [[ $rccode != 0 ]]
then
    printf "\n\n\t%s\n\n" "ERROR: promoteDropSite.sh failed. Subsequent promotion cancelled."
    exit $rccode
fi


${PROMOTE_IMPL}/promoteRepo.sh ${DROP_ID} ${DL_LABEL} ${REPO_SITE_SEGMENT} ${HIDE_SITE}
rccode=$?
if [[ $rccode != 0 ]]
then
    printf "\n\n\t%s\n\n" "ERROR: promoteRepo.sh failed."
    exit $rccode
fi

exit 0
