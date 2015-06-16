#!/usr/bin/env bash

# Utility to rename build and "promote" it to DL Server.

# INDEX_ONLY means that everything has been promoted once already
# and we merely want to "rename" and "promote" any new unit tests
# or performance tests that have completed since the initial promotion.
# Some ways it differs: If set, existing "site" (on build machine) is
# not deleted first. Only the main Eclipse site is effected, not
# equinox, not update site. None of the "deferred" stuff is set.
#export INDEX_ONLY=true
# We only ever check for 'true'
#export INDEX_ONLY=false

export DROP_ID=I20150603-2000

#export DL_LABEL=4.5RC4
#export DL_LABEL_EQ=MarsRC4
export DL_LABEL=4.5
export DL_LABEL_EQ=Mars

# for I builds, stable and RCs go to in milestones
# for M builds, even RCs also go in <version>-M-builds
#export REPO_SITE_SEGMENT=4.5-M-builds
#export REPO_SITE_SEGMENT=4.5milestones
export REPO_SITE_SEGMENT=4.5

if [[ "$INDEX_ONLY" == "true" ]]
then
  export HIDE_SITE=false
else
  export HIDE_SITE=true
fi

# These are what precedes main drop directory name
# For Maintenance, it's always 'M' (from M-build) until it's 'R'.
# for main line code, it's 'S' (from I-build) until it's 'R'
#export DL_TYPE=S
export DL_TYPE=R
#export DL_TYPE=M

# = = = = = = = Things past here seldom need to be updated

export CL_SITE=${PWD}
echo "CL_SITE: ${CL_SITE}"

export PROMOTE_IMPL=/shared/eclipse/sdk/promoteStableRelease/promoteImpl
export TRACE_LOG=${CL_SITE}/traceLog.txt

source ${PROMOTE_IMPL}/computeTagFromLabel.sh

# variables used for tagging aggregator for milestones and RCs.
# Note we always use "S" at the beginning, for sorting consistency
export NEW_TAG=$( computeTagFromLabel "$DL_LABEL" )
# For now, we'll just use handy Equinox label for tag annotation, but could elaborate in future
export NEW_ANNOTATION="${DL_LABEL_EQ}"
# later combined with BUILD_ROOT, so we get the correct clone
# should very seldom need to change, if ever.
export AGGR_LOCATION="gitCache/eclipse.platform.releng.aggregator"

# Used in naming repo, etc
export TRAIN_NAME=Mars

# Build machine locations (would very seldom change)
export BUILD_ROOT=/shared/eclipse/builds/4I
export BUILDMACHINE_BASE_SITE=${BUILD_ROOT}/siteDir/updates/4.5-I-builds

export BUILDMACHINE_BASE_DL=${BUILD_ROOT}/siteDir/eclipse/downloads/drops4
export BUILDMACHINE_BASE_EQ=${BUILD_ROOT}/siteDir/equinox/drops

export BUILD_TIMESTAMP=${DROP_ID//[MI-]/}

# Eclipse Drop Site (final segment)
export ECLIPSE_DL_DROP_DIR_SEGMENT=${DL_TYPE}-${DL_LABEL}-${BUILD_TIMESTAMP}
# Equinox Drop Site (final segment)
export EQUINOX_DL_DROP_DIR_SEGMENT=${DL_TYPE}-${DL_LABEL_EQ}-${BUILD_TIMESTAMP}

if [[ ! "${INDEX_ONLY}" == "true" ]]
then
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
else
  printf "\n\tINFO: %s\n" "Doing an INDEX_ONLY run, so deferred script not produced."
fi

if [[ ! "${INDEX_ONLY}" == "true" ]]
then
  # we do Equinox first, since it has to wait in que until
  # cronjob promotes it
  ${PROMOTE_IMPL}/promoteDropSiteEq.sh ${DROP_ID} ${DL_LABEL_EQ} ${HIDE_SITE}
  rccode=$?
  if [[ $rccode != 0 ]]
  then
    printf "\n\n\t%s\n\n" "ERROR: promoteDropSiteEq.sh failed. Subsequent promotion cancelled."
    exit $rccode
  fi
else
  printf "\n\tINFO: %s\n" "Doing an INDEX_ONLY run, so equinox not promoted."
fi

${PROMOTE_IMPL}/promoteDropSite.sh   ${DROP_ID} ${DL_LABEL} ${HIDE_SITE}
rccode=$?
if [[ $rccode != 0 ]]
then
  printf "\n\n\t%s\n\n" "ERROR: promoteDropSite.sh failed. Subsequent promotion cancelled."
  exit $rccode
fi


if [[ ! "${INDEX_ONLY}" == "true" ]]
then
  ${PROMOTE_IMPL}/promoteRepo.sh ${DROP_ID} ${DL_LABEL} ${REPO_SITE_SEGMENT} ${HIDE_SITE}
  rccode=$?
  if [[ $rccode != 0 ]]
  then
    printf "\n\n\t%s\n\n" "ERROR: promoteRepo.sh failed."
    exit $rccode
  fi
else
  printf "\n\tINFO: %s\n" "Doing an INDEX_ONLY run, so repo not promoted."
fi

if [[ ! "${INDEX_ONLY}" == "true" ]]
then
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
else
  printf "\n\tINFO: %s\n" "Doing an INDEX_ONLY run, so tagging script not promoted."
fi

if [[ ! "${INDEX_ONLY}" == "true" ]]
then

  # create script that automates the second step, doing all deferred actions at once.
  # (other than sending final email, and updating b3 aggregation file).

  ${PROMOTE_IMPL}/createDeferredStepsScript.sh
  rccode=$?
  if [[ $rccode != 0 ]]
  then
    printf "\n\n\t%s\n\n" "ERROR: createDeferredStepsScript.sh failed."
    exit $rccode
  fi
else
  printf "\n\tINFO: %s\n" "Doing an INDEX_ONLY run, so deferred step script not promoted."
fi


exit 0
