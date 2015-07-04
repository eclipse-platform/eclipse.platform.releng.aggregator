#!/usr/bin/env bash

# Utility to add "stats" to repos. For documentation, see
# https://wiki.eclipse.org/WTP/Releng/Tools/addRepoProperties

# I theory, with a few more variables? This script should be the same 
# as addRepoProperties.sh

export PROMOTE_IMPL=${PROMOTE_IMPL:-/shared/eclipse/sdk/promoteStableRelease/promoteImpl}

APP_NAME=org.eclipse.wtp.releng.tools.addRepoProperties

devworkspace=${devworkspace:-${PWD}/workspaceAddRepoProperties}


export REPO=$1
export REPO_TYPE=$2
export BUILD_ID=$3
export STATS_TAG_FEATURE_LIST=$4
#export STATS_TAG_VERSIONINDICATOR=$5
export STATS_TAG_SUFFIX=$5

source ${PROMOTE_IMPL}/promoteUtilities.shsource


echo "REPO: ${REPO}"
echo "REPO_TYPE: ${REPO_TYPE}"
echo "BUILD_ID: ${BUILD_ID}"
echo "STATS_TAG_FEATURE_LIST=${STATS_TAG_FEATURE_LIST}"
echo "STATS_TAG_SUFFIX: ${STATS_TAG_SUFFIX}"

if [[ -z "${REPO}" ]]
then
  echo "ERROR: this script requires a repository to add properties to."
  exit 1
fi


if [[ -z "${REPO_TYPE}" || -z "${BUILD_ID}"  ]]
then
  echo "WARNING: no mirror URL specified.";
  MIRRORURL=""
else
  MIRRORURL=/eclipse/updates/${REPO_TYPE}/${BUILD_ID}
fi

if [[ ! -z "${MIRRORURL}" ]]
then
  # remember, the '&' should NOT be XML escaped here ... the p2 api (or underlying xml) will escape it.
  MIRRORURL_ARG="http://www.eclipse.org/downloads/download.php?format=xml&file=${MIRRORURL}"
else
  MIRRORURL_ARG=""
fi

# TODO: control with variable!
#ART_REPO_NAME="Eclipse Project Repository for ${TRAIN_NAME}"
#CON_REPO_NAME="Eclipse Project Repository for ${TRAIN_NAME}"
ART_REPO_NAME="Eclipse Project Java 9 Patch Repository for Eclipse 4.5 (Mars Release)"
CON_REPO_NAME="Eclipse Project Java 9 Patch Repository for Eclipse 4.5 (Mars Release)"

MIRRORS_URL_ARG=-Dp2MirrorsURL=${MIRRORURL_ARG}
ART_REPO_ARG=-DartifactRepoDirectory=${REPO}
CON_REPO_ARG=-DmetadataRepoDirectory=${REPO}
ART_REPO_NAME_ARG=-Dp2ArtifactRepositoryName=\"${ART_REPO_NAME}\"
CON_REPO_NAME_ARG=-Dp2MetadataRepositoryName=\"${CON_REPO_NAME}\"

if [[ -n "${STATS_TAG_FEATURE_LIST}" ]]
then
  STATS_TAG_FEATURE_LIST_ARG="-DstatsTrackedArtifacts=${STATS_TAG_FEATURE_LIST}"
  # no sense setting these others, if features not set
  # TODO: more error checking could be done, to warn, such if features list set, but other values not
  STATS_TAG_VERSIONINDICATOR_ARG="-Dp2StatsURI=http://download.eclipse.org/stats${MIRRORURL}"
  if [[ -n ${STATS_TAG_SUFFIX} ]]
  then
    STATS_TAG_SUFFIX_ARG="-DstatsArtifactsSuffix=${STATS_TAG_SUFFIX}"
  fi
fi

echo "dev:               ${BASH_SOURCE}"
echo "devworkspace:      ${devworkspace}"
echo "JAVA_EXEC_DIR:     ${JAVA_EXEC_DIR}"
echo "APP_NAME:          ${APP_NAME}"
echo "MIRRORURL:         ${MIRRORURL}"
echo "MIRRORURL_ARG:     ${MIRRORURL_ARG}"
echo "MIRRORS_URL_ARG:   ${MIRRORS_URL_ARG}"
echo "ART_REPO_ARG:      ${ART_REPO_ARG}"
echo "CON_REPO_ARG:      ${CON_REPO_ARG}"
echo "ART_REPO_NAME:     ${ART_REPO_NAME}"
echo "CON_REPO_NAME:     ${CON_REPO_NAME}"
echo "ART_REPO_NAME_ARG: ${ART_REPO_NAME_ARG}"
echo "CON_REPO_NAME_ARG: ${CON_REPO_NAME_ARG}"
echo "STATS_TAG_FEATURE_LIST_ARG: ${STATS_TAG_FEATURE_LIST_ARG}"
echo "STATS_TAG_VERSIONINDICATOR_ARG: ${STATS_TAG_VERSIONINDICATOR_ARG}"
echo "STATS_TAG_SUFFIX_ARG: ${STATS_TAG_SUFFIX_ARG}"

if [[ -x ${JAVA_CMD} ]]
then
  echo
  $JAVA_CMD -version
  echo

  findEclipseExe $BUILD_ID
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo "non zero return code returned from findEclipse: $RC"
  else
    if [[ -x ${ECLIPSE_EXE} ]]
    then
      echo "found eclipse executable, will execute:"
      echo "${ECLIPSE_EXE} --launcher.suppressErrors -nosplash -consolelog -debug -data ${devworkspace} -application ${APP_NAME} -vm ${JAVA_EXEC_DIR} -vmargs ${MIRRORS_URL_ARG} ${ART_REPO_ARG} ${CON_REPO_ARG} ${ART_REPO_NAME_ARG} ${CON_REPO_NAME_ARG} ${STATS_TAG_FEATURE_LIST_ARG} ${STATS_TAG_VERSIONINDICATOR_ARG} ${STATS_TAG_SUFFIX_ARG}"
      # we may need to 'clean' here, since using an installation that has been moved?
       ${ECLIPSE_EXE} --launcher.suppressErrors -nosplash -consolelog -debug -data ${devworkspace} -application ${APP_NAME} -vm ${JAVA_EXEC_DIR} -vmargs ${MIRRORS_URL_ARG} -Dp2ArtifactRepositoryName="${ART_REPO_NAME}" -Dp2MetadataRepositoryName="${CON_REPO_NAME}" ${ART_REPO_ARG} ${CON_REPO_ARG} ${STATS_TAG_FEATURE_LIST_ARG} ${STATS_TAG_VERSIONINDICATOR_ARG} ${STATS_TAG_SUFFIX_ARG}
      #${ECLIPSE_EXE} --launcher.suppressErrors -nosplash -consolelog -debug -data ${devworkspace} -application ${APP_NAME} -vm ${JAVA_EXEC_DIR} -vmargs ${MIRRORS_URL_ARG} -Dp2ArtifactRepositoryName="${ART_REPO_NAME}" -Dp2MetadataRepositoryName="${CON_REPO_NAME}" ${ART_REPO_ARG} ${CON_REPO_ARG}
      RC=$?
    else
      printf "\n\tERROR: %s\n\n" "The Eclipse commmand, ${ECLIPSE_EXE}, was not executable or not specified"
      RC=1
    fi
  fi
else
  printf "\n\tERROR: %s\n\n" "The Java commmand, ${JAVA_CMD}, was not executable or not specified"
  RC=1
fi
echo "RC: $RC"
exit $RC
