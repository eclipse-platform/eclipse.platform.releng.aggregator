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

# DROP_ID is the name of the build we are promoting. 
# That is, the FROM build. The TO name is computed from it, 
# and a few other variables, below. 
export DROP_ID=M-4.5.1RC3-201509040015
#export DROP_ID=M20150904-0015
# TODO: Can later infer what EQ drop id and label would be
export DROP_ID_EQ=M-Mars.1RC3-201509040015
export BUILD_LABEL_EQ=Mars.1RC3

export BUILD_MAJOR=4
export BUILD_MINOR=5
export BUILD_SERVICE=1
# checkpoint means either milestone or release candidate
# should be empty for final release
export CHECKPOINT=
# Used in naming repo and equinox download pages.
export TRAIN_NAME=Mars.1

# These are what precedes main drop directory name -- 
# that is, for what we are naming the build TO
# For Maintenance, it's always 'M' (from M-build) until it's 'R'.
# for main line code, it's 'S' (from I-build) until it's 'R'
#export DL_TYPE=S
export DL_TYPE=R
#export DL_TYPE=M

export CL_SITE=${PWD}
echo "CL_SITE: ${CL_SITE}"

# BUILD_TYPE is the prefix of the build -- 
# that is, for what we are naming the build FROM
RCPATTERN="^([MI])-(${BUILD_MAJOR}\.${BUILD_MINOR}\.${BUILD_SERVICE}RC[12345]{1}[abcd]?)-([[:digit:]]{12})$"
PATTERN="^([MI])([[:digit:]]{8})-([[:digit:]]{4})$"
if [[ "${DROP_ID}" =~ $RCPATTERN ]]
then
  export BUILD_TYPE=${BASH_REMATCH[1]}
  export BUILD_LABEL=${BASH_REMATCH[2]}
  export BUILD_TIMESTAMP=${BASH_REMATCH[3]}
elif [[ "${DROP_ID}" =~ $PATTERN ]]
then
  export BUILD_TYPE=${BASH_REMATCH[1]}
  export BUILD_TIMESTAMP=${BASH_REMATCH[2]}${BASH_REMATCH[3]}
  # Label and ID are the same, in this case
  export BUILD_LABEL=$DROP_ID
else 
  echo -e "\n\tERROR: DROP_ID, ${DROP_ID}, did not match any expected pattern."
  exit 1
fi

# DEBUG/TEST exit
#echo "BUILD_TYPE: $BUILD_TYPE"
#echo "BUILD_LABEL: $BUILD_LABEL"
#echo "BUILD_TIMESTAMP: $BUILD_TIMESTAMP"
#exit 

#export BUILD_TYPE=${DROP_ID:0:1}
#export BUILD_TIMESTAMP=${DROP_ID//[MI-]/}
# = = = = = = = Things past here seldom need to be updated

# For initial releases, do not include service in label
if [[ "${BUILD_SERVICE}" == "0" ]]
then
  export DL_LABEL=${BUILD_MAJOR}.${BUILD_MINOR}${CHECKPOINT}
else
  export DL_LABEL=${BUILD_MAJOR}.${BUILD_MINOR}.${BUILD_SERVICE}${CHECKPOINT}
fi
export DL_LABEL_EQ=${TRAIN_NAME}${CHECKPOINT}



# for I builds, stable and RCs go to in milestones
# for M builds, even RCs also go in <version>-M-builds
case ${DL_TYPE} in
  "M" )
    export REPO_SITE_SEGMENT=${BUILD_MAJOR}.${BUILD_MINOR}-${BUILD_TYPE}-builds
    ;;
  "S" )
    export REPO_SITE_SEGMENT=${BUILD_MAJOR}.${BUILD_MINOR}milestones
    ;;
  "R" )
    export REPO_SITE_SEGMENT=${BUILD_MAJOR}.${BUILD_MINOR}
    ;;
  *)
    echo -e "\n\tERROR: case statement for repo output did not match any pattern."
    echo -e   "\t       Not written to handle DL_TYPE of ${DL_TYPE}\n"
    exit 1
esac

if [[ "$INDEX_ONLY" == "true" ]]
then
  export HIDE_SITE=false
else
  export HIDE_SITE=true
fi

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
# We use this for "deferred tagging" so important to "leave the same",
# until tagged.
export AGGR_LOCATION="gitCache/eclipse.platform.releng.aggregator"


# Build machine locations (would very seldom change)
# Almost always "I", but does change to "M" for maintenance stream.
export BUILD_ROOT=/shared/eclipse/builds/${BUILD_MAJOR}${BUILD_TYPE}
export BUILDMACHINE_BASE_SITE=${BUILD_ROOT}/siteDir/updates/${BUILD_MAJOR}.${BUILD_MINOR}-${BUILD_TYPE}-builds

export BUILDMACHINE_BASE_DL=${BUILD_ROOT}/siteDir/eclipse/downloads/drops4
export BUILDMACHINE_BASE_EQ=${BUILD_ROOT}/siteDir/equinox/drops



# Eclipse Drop Site (final segment)
export ECLIPSE_DL_DROP_DIR_SEGMENT=${DL_TYPE}-${DL_LABEL}-${BUILD_TIMESTAMP}
# Equinox Drop Site (final segment)
export EQUINOX_DL_DROP_DIR_SEGMENT=${DL_TYPE}-${DL_LABEL_EQ}-${BUILD_TIMESTAMP}

if [[ ! "${INDEX_ONLY}" == "true" ]]
then
  printf "\n\t%s\n\n" "Promoted on: $( date )" > "${CL_SITE}/checklist.txt"
  printf "\n\t%20s%25s\n" "DROP_ID" "$DROP_ID" >> "${CL_SITE}/checklist.txt"
  printf "\n\t%20s%25s\n" "BUILD_LABEL" "$BUILD_LABEL" >> "${CL_SITE}/checklist.txt"
  printf "\n\t%20s%25s\n" "DROP_ID_EQ" "$DROP_ID_EQ" >> "${CL_SITE}/checklist.txt"
  printf "\n\t%20s%25s\n" "BUILD_LABEL_EQ" "$BUILD_LABEL_EQ" >> "${CL_SITE}/checklist.txt"
  printf "\n" >> "${CL_SITE}/checklist.txt"
  printf "\t%20s%25s\n" "DL_TYPE" "$DL_TYPE" >> "${CL_SITE}/checklist.txt"
  printf "\t%20s%25s\n" "DL_LABEL" "$DL_LABEL" >> "${CL_SITE}/checklist.txt"
  printf "\t%20s%25s\n" "DL_LABEL_EQ" "$DL_LABEL_EQ" >> "${CL_SITE}/checklist.txt"
  printf "\t%20s%25s\n" "ECLIPSE_DL_DROP_DIR_SEGMENT" "$ECLIPSE_DL_DROP_DIR_SEGMENT" >> "${CL_SITE}/checklist.txt"
  printf "\t%20s%25s\n" "EQUINOX_DL_DROP_DIR_SEGMENT" "$EQUINOX_DL_DROP_DIR_SEGMENT" >> "${CL_SITE}/checklist.txt"
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
  if [[ "${DL_TYPE}" != "R" ]]
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
    printf "\n\tINFO: %s\n" "No tagging script created, since promoting to an R-Build."
    printf "\tINFO: %s\n" "But, we did create NEWS_ID, ACK_ID and README_ID and added to buildproperties.php, since doing Release promote."
    # We change "the new location", on build machine ... since files are already copied.
    echo -e "\$NEWS_ID = \"${BUILD_MAJOR}.${BUILD_MINOR}\";" >> "${BUILDMACHINE_BASE_DL}/${ECLIPSE_DL_DROP_DIR_SEGMENT}/buildproperties.php"
    echo -e "\$ACK_ID = \"${BUILD_MAJOR}.${BUILD_MINOR}\";" >> "${BUILDMACHINE_BASE_DL}/${ECLIPSE_DL_DROP_DIR_SEGMENT}/buildproperties.php"
    echo -e "\$README_ID = \"${BUILD_MAJOR}.${BUILD_MINOR}\";" >> "${BUILDMACHINE_BASE_DL}/${ECLIPSE_DL_DROP_DIR_SEGMENT}/buildproperties.php"
  fi
else
  printf "\n\tINFO: %s\n" "Doing an INDEX_ONLY run, so tagging script not created."
fi

if [[ "${DL_TYPE}" == "S" ]]
then
  printf "\tINFO: %s\n" "Created NEWS_ID and added to buildproperties.php, since doing Milestone promote."
  echo -e "\$NEWS_ID = \"${BUILD_MAJOR}.${BUILD_MINOR}/${CHECKPOINT}\";" >> "${BUILDMACHINE_BASE_DL}/${ECLIPSE_DL_DROP_DIR_SEGMENT}/buildproperties.php"
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
# ### Do the actual promotion to downloads last ###
if [[ ! "${INDEX_ONLY}" == "true" ]]
then
  # we do Equinox first, since it has to wait in que until
  # cronjob promotes it
  ${PROMOTE_IMPL}/promoteDropSiteEq.sh ${DROP_ID_EQ} ${BUILD_LABEL_EQ} ${DL_LABEL_EQ} ${HIDE_SITE}
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

exit 0
