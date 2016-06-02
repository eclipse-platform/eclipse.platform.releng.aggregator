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


# Currently this isn't used much, but is intended to be for "advanced debugging".
export TRACE_LOG=${CL_SITE}/traceLog.txt

if [[ "${STREAM}" =~ ^([[:digit:]]+)\.([[:digit:]]+)\.([[:digit:]]+)$ ]]
then
  export BUILD_MAJOR=${BASH_REMATCH[1]}
  export BUILD_MINOR=${BASH_REMATCH[2]}
  export BUILD_SERVICE=${BASH_REMATCH[3]}
else
  echo "STREAM must contain major, minor, and service versions, such as 4.3.0"
  echo "    but found ${STREAM}"
  exit 1
fi


# remove any of the scripts we create, such as for 'dry-run'.
# Notice the "verbose", but "ignore non-existent files"
rm -vf ${CL_SITE}/*.txt ${CL_SITE}/deferred*

# regex section
# BUILD_TYPE is the prefix of the build -- 
# that is, for what we are renaming the build FROM
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
  export BUILD_LABEL_EQ=$DROP_ID
  export DROP_ID_EQ=$DROP_ID
else 
  echo -e "\n\tERROR: DROP_ID, ${DROP_ID}, did not match any expected pattern."
  exit 1
fi

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
    # NEWS_ID is for milestones only, not RCs. So we check that CHECHPOINT starts 
    # with 'M' (M1, M2, ... M7) and do not created if CHECKPOINT is an RC (RC1, RC2 ... RC4)
    if [[ "${CHECKPOINT}" =~ ^M.*$ ]]
    then
      export NEWS_ID=${BUILD_MAJOR}.${BUILD_MINOR}/${CHECKPOINT}
    fi
    # except for RC4. Since it it intended to be "final" we include the variables 
    # just like they were for an "R" build. (See bug 495252). 
    if [[ "${CHECKPOINT}" == "RC4" ]]
    then
      export NEWS_ID=${BUILD_MAJOR}.${BUILD_MINOR}
      export ACK_ID=${BUILD_MAJOR}.${BUILD_MINOR}
      export README_ID=${BUILD_MAJOR}.${BUILD_MINOR}
    fi
    ;;
  "R" )
    export REPO_SITE_SEGMENT=${BUILD_MAJOR}.${BUILD_MINOR}
    export NEWS_ID=${BUILD_MAJOR}.${BUILD_MINOR}
    export ACK_ID=${BUILD_MAJOR}.${BUILD_MINOR}
    export README_ID=${BUILD_MAJOR}.${BUILD_MINOR}
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

# This "localBuildProperties" file is not for production runs. 
# It is only for local testing, where some key locations or hosts may be 
# defined differently.
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
  
  # mail template

  # start with empty line, and '>' to be sure re-created if exists already.  
  printf "\n" > "${CL_SITE}/mailtemplate.txt"
  
  printf "\n%s\n\n" "${INITIAL_MAIL_LINES}" >> "${CL_SITE}/mailtemplate.txt"
  
  printf "\t%s\n" "Eclipse downloads:" >> "${CL_SITE}/mailtemplate.txt"
  printf "\t%s\n\n" "http://download.eclipse.org/eclipse/downloads/drops4/${ECLIPSE_DL_DROP_DIR_SEGMENT}/" >> "${CL_SITE}/mailtemplate.txt"

  printf "\t%s\n" "Update existing (non-production) installs:" >> "${CL_SITE}/mailtemplate.txt"
  printf "\t%s\n\n" "http://download.eclipse.org/eclipse/updates/${REPO_SITE_SEGMENT}/" >> "${CL_SITE}/mailtemplate.txt"

  printf "\t%s\n" "Specific repository good for building against:" >> "${CL_SITE}/mailtemplate.txt"
  printf "\t%s\n\n" "http://download.eclipse.org/eclipse/updates/${REPO_SITE_SEGMENT}/${ECLIPSE_DL_DROP_DIR_SEGMENT}/" >> "${CL_SITE}/mailtemplate.txt"

  printf "\t%s\n" "Equinox specific downloads:" >> "${CL_SITE}/mailtemplate.txt"
  printf "\t%s\n\n" "http://download.eclipse.org/equinox/drops/${EQUINOX_DL_DROP_DIR_SEGMENT}/" >> "${CL_SITE}/mailtemplate.txt"

  printf "\n\n%s\n" "${CLOSING_MAIL_LINES}" >> "${CL_SITE}/mailtemplate.txt"

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

if [[ "${DRYRUN}" == "true" ]]
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
  printf "\n\t\t%s\n" "[INFO] The scripts for \"stage 2\" (deferred) promotion are in ${CL_SITE}"
exit 0
