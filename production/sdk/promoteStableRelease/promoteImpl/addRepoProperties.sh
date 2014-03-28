#!/usr/bin/env bash

# Utility to add "stats" to repos. For documentation, see 
# https://wiki.eclipse.org/WTP/Releng/Tools/addRepoProperties


export PROMOTE_IMPL=${PROMOTE_IMPL:-/shared/eclipse/sdk/promoteStableRelease/promoteImpl}

source ${PROMOTE_IMPL}/promoteUtilities.shsource
source promoteUtilities.shsource

APP_NAME=org.eclipse.wtp.releng.tools.addRepoProperties

devworkspace=${devworkspace:-${PWD}/workspaceAddRepoProperties}

REPO=$1
REPO_TYPE=$2
BUILD_ID=$3
STATS_TAG_VERSIONINDICATOR=$4
STATS_TAG_SUFFIX=$5

if [[ -z "${REPO}" ]]
then
    echo "ERROR: this script requires a repository to add properties to."
    exit 1
fi

echo "REPO: ${REPO}"
echo "REPO_TYPE: ${REPO_TYPE}"
echo "BUILD_ID: ${BUILD_ID}"
echo "STATS_TAG_VERSIONINDICATOR: ${STATS_TAG_VERSIONINDICATOR}"
echo "STATS_TAG_SUFFIX: ${STATS_TAG_SUFFIX}"

if [[ -z "${REPO_TYPE}" || -z "${BUILD_ID}"  ]]
then 
    echo "WARNING: no mirror URL specified.";
    MIRRORURL=""
else
    MIRRORURL="/eclipse/updates/${REPO_TYPE}/${BUILD_ID}"
fi

if [ ! -z $MIRRORURL ] 
then 
    # remember, the '&' should NOT be unescaped here ... the p2 api (or underlying xml) will escape it. 
    MIRRORURL_ARG="http://www.eclipse.org/downloads/download.php?format=xml&file=${MIRRORURL}"
else
    MIRRORURL_ARG=""
fi

#ART_REPO_NAME="Eclipse Project Repository for ${TRAIN_NAME}"
#CON_REPO_NAME="Eclipse Project Repository for ${TRAIN_NAME}"
ART_REPO_NAME="Eclipse Project Java 8 Patch Repository for Kepler SR2"
CON_REPO_NAME="Eclipse Project Java 8 Patch Repository for Kepler SR2"

MIRRORS_URL_ARG="-Dp2MirrorsURL=${MIRRORURL_ARG}"
ART_REPO_ARG="-DartifactRepoDirectory=${REPO}"
CON_REPO_ARG="-DmetadataRepoDirectory=${REPO}"
ART_REPO_NAME_ARG="-Dp2ArtifactRepositoryName=${ART_REPO_NAME}"
CON_REPO_NAME_ARG="-Dp2MetadataRepositoryName=${CON_REPO_NAME}"

# not currently used
#-Dp2StatsURI=http://download.eclipse.org/stats/eclipse/updates${STATS_TAG_VERSIONINDICATOR} -DstatsArtifactsSuffix="${STATS_TAG_SUFFIX}" -DstatsTrackedArtifacts=org.eclipse.wst.jsdt.feature,org.eclipse.wst.xml_ui.feature,org.eclipse.wst.web_ui.feature,org.eclipse.jst.enterprise_ui.feature"
-Dp2StatsURI=http://download.eclipse.org/stats/eclipse/updates${STATS_TAG_VERSIONINDICATOR} -DstatsArtifactsSuffix="${STATS_TAG_SUFFIX}" -DstatsTrackedArtifacts=org.eclipse.jdt.java8patch"


echo "dev:               ${BASH_SOURCE}"
echo "devworkspace:      ${devworkspace}"
echo "JAVA_EXEC_DIR:     ${JAVA_EXEC_DIR}"
echo "APP_NAME:          ${APP_NAME}"
echo "MIRRORS_URL_ARG:   ${MIRRORS_URL_ARG}"
echo "ART_REPO_ARG:      ${ART_REPO_ARG}"
echo "CON_REPO_ARG:      ${CON_REPO_ARG}"
echo "ART_REPO_NAME_ARG: ${ART_REPO_NAME_ARG}"
echo "CON_REPO_NAME_ARG: ${CON_REPO_NAME_ARG}"

echo
$JAVA_CMD -version
echo

${ECLIPSE_EXE} -nosplash -consolelog -debug -data ${devworkspace} -vm ${JAVA_EXEC_DIR} -application ${APP_NAME}  -vmargs "${MIRRORS_URL_ARG}" "${ART_REPO_ARG}" "${CON_REPO_ARG}" "${ART_REPO_NAME_ARG}" "${CON_REPO_NAME_ARG}"
RC=$?

exit $RC
