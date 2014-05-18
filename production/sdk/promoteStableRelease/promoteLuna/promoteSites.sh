#!/usr/bin/env bash

export DROP_ID=I20140515-1230

export DL_LABEL=4.4RC1
export DL_LABEL_EQ=LunaRC1

# for I builds, stable and RCs go to in milestones
export REPO_SITE_SEGMENT=4.4milestones
#REPO_SITE_SEGMENT=4.4

export HIDE_SITE=true
#HIDE_SITE=false

export CL_SITE=${PWD}
echo "CL_SITE: ${CL_SITE}"

# These are what precedes main drop directory name
export DL_TYPE=S
#export DL_TYPE=R
#export DL_TYPE=M

# variables used on tagging aggregator for milestones (and RCs?)
# Could probably compute this tag ... but for now easier to type it in each time.
export NEW_TAG=S4_4_0_RC1
# For now, we'll just use handy Equinox label for tag annotation, but could elaborate in future
export NEW_ANNOTATION="${DL_LABEL_EQ}"
# later combined with BUILD_ROOT, so we get the correct clone
# should very seldom need to change, if ever.
export AGGR_LOCATION="gitCache/eclipse.platform.releng.aggregator"

export TRACE_LOG=${PWD}/traceLog.txt

# Used in naming repo, etc
export TRAIN_NAME=Luna

# Build machine locations (would very seldom change)
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

# If all goes well, we create the "tag script", but don't actually run it
# until we make the site visible, after doing sanity checking, etc.
# Note, this script relies on a number of exported variables
${PROMOTE_IMPL}/tagPromotedBuilds.sh
rccode=$?
if [[ $rccode != 0 ]]
then
  printf "\n\n\t%s\n\n" "ERROR: tagPromotedBuilds.sh failed."
  exit $rccode
fi

exit 0
