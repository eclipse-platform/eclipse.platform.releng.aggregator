#!/usr/bin/env bash


APP_NAME=org.eclipse.wtp.releng.tools.addRepoProperties

devworkspace=${PWD}/workspaceAddRepoProperties



#REPO="/shared/eclipse/builds/4I/siteDir/updates"
#REPO_TYPE=4.3milestones (4.3-I-builds, 4.3, etc)
#BUILD_ID=S-4.3RC3-20101209114749
# not currently used
#STATS_TAG_SUFFIX=_kepler
#STATS_TAG_VERSIONINDICATOR=/kepler

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

ART_REPO_NAME="Eclipse Project Repository for Kepler"
CON_REPO_NAME="Eclipse Project Repository for Kepler"

MIRRORS_URL_ARG="-Dp2MirrorsURL=${MIRRORURL_ARG}"
ART_REPO_ARG="-DartifactRepoDirectory=${REPO}"
CON_REPO_ARG="-DmetadataRepoDirectory=${REPO}"
ART_REPO_NAME_ARG="-Dp2ArtifactRepositoryName=${ART_REPO_NAME}"
CON_REPO_NAME_ARG="-Dp2MetadataRepositoryName=${CON_REPO_NAME}"

# not currently used
#-Dp2StatsURI=http://download.eclipse.org/stats/eclipse/updates${STATS_TAG_VERSIONINDICATOR} -DstatsArtifactsSuffix="${STATS_TAG_SUFFIX}" -DstatsTrackedArtifacts=org.eclipse.wst.jsdt.feature,org.eclipse.wst.xml_ui.feature,org.eclipse.wst.web_ui.feature,org.eclipse.jst.enterprise_ui.feature"


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
