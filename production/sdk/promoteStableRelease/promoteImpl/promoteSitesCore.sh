#!/usr/bin/env bash

# TODO: Should check that all required variables are defined. 
# That is, those variables defined in promoteSites.sh are in fact 
# defined and have reasonable values.

# For initial releases, do not include service in label
if [[ "${BUILD_SERVICE}" == "0" ]]
then
  export DL_LABEL=${BUILD_MAJOR}.${BUILD_MINOR}${CHECKPOINT}
else
  export DL_LABEL=${BUILD_MAJOR}.${BUILD_MINOR}.${BUILD_SERVICE}${CHECKPOINT}
fi
export DL_LABEL_EQ=${TRAIN_NAME}${CHECKPOINT}

# This is DL_DROP_ID for Eclipse. The one for equinox has DL_LABEL_EQ in middle.
export DL_DROP_ID=${DL_TYPE}-${DL_LABEL}-${BUILD_TIMESTAMP}
export DL_DROP_ID_EQ=${DL_TYPE}-${DL_LABEL_EQ}-${BUILD_TIMESTAMP}


# for I builds, stable and RCs go to in milestones
# for M builds, even RCs also go in <version>-M-builds
case ${DL_TYPE} in
  "M" )
    export REPO_SITE_SEGMENT=${BUILD_MAJOR}.${BUILD_MINOR}-${BUILD_TYPE}-builds
    ;;
  "S" )
    export REPO_SITE_SEGMENT=${BUILD_MAJOR}.${BUILD_MINOR}milestones
    export NEWS_ID=${BUILD_MAJOR}.${BUILD_MINOR}/${CHECKPOINT}
    ;;
  "R" )
    export REPO_SITE_SEGMENT=${BUILD_MAJOR}.${BUILD_MINOR}
    export NEWS_ID=${BUILD_MAJOR}.${BUILD_MINOR}
    export ACK_ID=${BUILD_MAJOR}.${BUILD_MINOR}
    export $README_ID=${BUILD_MAJOR}.${BUILD_MINOR}
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

source localBuildProperties.shsource 2>/dev/null
# Build machine locations (would very seldom change)
export BUILD_HOME=${BUILD_HOME:-/shared/eclipse/builds}
export BUILD_ROOT=${BUILD_HOME}/${BUILD_MAJOR}${BUILD_TYPE}
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
  printf "\n\t%20s%25s" "DROP_ID" "$DROP_ID" >> "${CL_SITE}/checklist.txt"
  printf "\n\t%20s%25s" "BUILD_LABEL" "$BUILD_LABEL" >> "${CL_SITE}/checklist.txt"
  printf "\n\t%20s%25s" "DROP_ID_EQ" "$DROP_ID_EQ" >> "${CL_SITE}/checklist.txt"
  printf "\n\t%20s%25s" "BUILD_LABEL_EQ" "$BUILD_LABEL_EQ" >> "${CL_SITE}/checklist.txt"
  printf "\n" >> "${CL_SITE}/checklist.txt"
  printf "\n\t%20s%25s" "DL_TYPE" "$DL_TYPE" >> "${CL_SITE}/checklist.txt"
  printf "\n\t%20s%25s" "DL_LABEL" "$DL_LABEL" >> "${CL_SITE}/checklist.txt"
  printf "\n\t%20s%25s" "DL_LABEL_EQ" "$DL_LABEL_EQ" >> "${CL_SITE}/checklist.txt"
  printf "\n\t%20s%25s" "ECLIPSE_DL_DROP_DIR_SEGMENT" "$ECLIPSE_DL_DROP_DIR_SEGMENT" >> "${CL_SITE}/checklist.txt"
  printf "\n\t%20s%25s" "EQUINOX_DL_DROP_DIR_SEGMENT" "$EQUINOX_DL_DROP_DIR_SEGMENT" >> "${CL_SITE}/checklist.txt"
  printf "\n\t%20s%25s" "REPO_SITE_SEGMENT" "$REPO_SITE_SEGMENT" >> "${CL_SITE}/checklist.txt"
  printf "\n\t%20s%25s\n" "HIDE_SITE" "${HIDE_SITE}" >> "${CL_SITE}/checklist.txt"

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
    # We change "the old location", on build machine ... since files are not copied yet.
    echo -e "\$NEWS_ID = \"${BUILD_MAJOR}.${BUILD_MINOR}\";" >> "${BUILDMACHINE_BASE_DL}/${DROP_ID}/buildproperties.php"
    echo -e "\$ACK_ID = \"${BUILD_MAJOR}.${BUILD_MINOR}\";" >> "${BUILDMACHINE_BASE_DL}/${DROP_ID}/buildproperties.php"
    echo -e "\$README_ID = \"${BUILD_MAJOR}.${BUILD_MINOR}\";" >> "${BUILDMACHINE_BASE_DL}/${DROP_ID}/buildproperties.php"
  fi
else
  printf "\n\tINFO: %s\n" "Doing an INDEX_ONLY run, so tagging script not created."
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

if [[ ${DRYRUN} == "dry-run" ]]
then
  printf "\n\t%s" "Doing dry-run ..."
  printf "\n"
  printf "\n\t%20s%25s" "DROP_ID:" "${DROP_ID}"
  printf "\n"
  printf "\n\t%20s%25s" "BUILD_TIMESTAMP:" "$BUILD_TIMESTAMP"
  printf "\n"
  printf "\n\t%20s%25s" "BUILD_TYPE:" "$BUILD_TYPE" 
  printf "\n\t%20s%25s" "DL_TYPE:" "${DL_TYPE}"
  printf "\n\t%20s%25s" "BUILD_LABEL:" "$BUILD_LABEL"
  printf "\n\t%20s%25s" "BUILD_LABEL_EQ:" "${BUILD_LABEL_EQ}"
  printf "\n\t%20s%25s" "DL_DROP_ID:" "$DL_DROP_ID"
  printf "\n\t%20s%25s" "DL_DROP_ID_EQ:" "${DL_DROP_ID_EQ}"
  printf "\n"
  printf "\n\t%20s%25s" "NEWS_ID:" "${NEWS_ID}"
  printf "\n"
  printf "\n\t%20s%25s" "DEBUG: CL_SITE:" "${CL_SITE}"
  printf "\n\n\t%s\n" "Be sure to inspect supporting scripts produced in CL_SITE, such as checklist.txt, deferredSteps.sh, deferredTag.sh or similar."
  exit 101 
fi 

# ### Begins the point of making modifications to the build ###

# This testNotes.html file is inserted for display by main index.php file, if it exists.
# SIGNOFF_BUG should not be defined if there are no JUnit failures to investigate and explain
if [[ -n "${SIGNOFF_BUG}" ]] 
then
 echo -e "<p>Any unit test failures below have been investigated and found to be test-related and do not affect the quality of the build.\nSee the sign-off page <a href=\"https://bugs.eclipse.org/bugs/show_bug.cgi?id=${SIGNOFF_BUG}\">(bug ${SIGNOFF_BUG})</a> for details.</p>" > "${BUILDMACHINE_BASE_DL}/${DROP_ID}/testNotes.html"
fi

# There is no "new and noteworthy" for RC builds, only Milestones.
# TODO: PLUS, there is one for final release! 
if [[ "${DL_TYPE}" == "S" && "${CHECKPOINT}" =~ ^M.*$ ]]
then
  printf "\tINFO: %s\n" "Created NEWS_ID and added to buildproperties.php, since doing Milestone promote."
  echo -e "\$NEWS_ID = \"${NEWS_ID}\";" >> "${BUILDMACHINE_BASE_DL}/${DROP_ID}/buildproperties.php"
fi

# ### Do the actual promotions ###

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
