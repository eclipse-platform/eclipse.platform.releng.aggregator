#!/usr/bin/env bash
# master script to drive Eclipse Platform builds.


if [ $# -ne 1 ]; then
	echo USAGE: $0 env_file
	exit 1
fi

if [ ! -r "$1" ]; then
	echo "$1" cannot be read
	echo USAGE: $0 env_file
	exit 1
fi

pushd $( dirname $0 ) >/dev/null
SCRIPT_PATH=$(pwd)
popd >/dev/null

. $SCRIPT_PATH/build-functions.sh

. "$1"


cd $BUILD_ROOT

# derived values


BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BRANCH" "$BUILD_ID" )
logsDirectory="${buildDirectory}/buildlogs"
mkdir -p "${logsDirectory}"
checkForErrorExit $? "Could not create buildlogs directory"

LOG=$buildDirectory/buildlogs/buildOutput.txt
exec >>$LOG 2>&1

BUILD_ENV_FILE=$logsDirectory/$BUILD_ID.env
gitCache=$( fn-git-cache "$BUILD_ROOT" "$BRANCH" )
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )
localRepo=$gitCache/localMavenRepo

# These variables, from original env file, are re-written to BUILD_ENV_FILE, 
# with values for this build (some of them computed) partially for documentation, and 
# partially so this build can be re-ran or re-started using it, instead of 
# original env file, which would compute different values (in some cases).
echo "BUILD_ROOT=$BUILD_ROOT"  >>$BUILD_ENV_FILE
echo "BRANCH=$BRANCH" >>$BUILD_ENV_FILE
echo "STREAM=$STREAM" >>$BUILD_ENV_FILE
echo "BUILD_TYPE=$BUILD_TYPE" >>$BUILD_ENV_FILE
echo "TMP_DIR=$TMP_DIR" >>$BUILD_ENV_FILE 
echo "JAVA_HOME=$JAVA_HOME" >>$BUILD_ENV_FILE
echo "MAVEN_OPTS=$MAVEN_OPTS" >>$BUILD_ENV_FILE
echo "MAVEN_PATH=$MAVEN_PATH" >>$BUILD_ENV_FILE
echo "AGGREGATOR_REPO=$AGGREGATOR_REPO" >>$BUILD_ENV_FILE
echo "BASEBUILDER_TAG=$BASEBUILDER_TAG" >>$BUILD_ENV_FILE
echo "SIGNING_REPO=$SIGNING_REPO" >>$BUILD_ENV_FILE
echo "SIGNING_BRANCH=$SIGNING_BRANCH" >>$BUILD_ENV_FILE
echo "B_GIT_EMAIL=$B_GIT_EMAIL" >>$BUILD_ENV_FILE
echo "B_GIT_NAME=$B_GIT_NAME" >>$BUILD_ENV_FILE
echo "COMMITTER_ID=$COMMITTER_ID" >>$BUILD_ENV_FILE
echo "COMPARATOR=$COMPARATOR" >>$BUILD_ENV_FILE
echo "SIGNING=$SIGNING" >>$BUILD_ENV_FILE
echo "UPDATE_BRANDING=$UPDATE_BRANDING" >>$BUILD_ENV_FILE
echo "FORCE_LOCAL_REPO=$FORCE_LOCAL_REPO" >>$BUILD_ENV_FILE
# any value of interest/usefulness can be added to BUILD_ENV_FILE
echo "BUILD_ENV_FILE=$1" >>$BUILD_ENV_FILE
echo "BUILD_ID=$BUILD_ID" >>$BUILD_ENV_FILE
echo "BUILD_DATE=\"$(date)\"" >>$BUILD_ENV_FILE

# dump ALL environment variables in case its helpful in documenting or 
# debugging build results or differences between runs, especially on different machines
env 2>&1 | tee $logsDirectory/all-env-variables.txt

$SCRIPT_PATH/get-aggregator.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/get-aggregator-ouptut.txt
checkForErrorExit $? "Error occurred while getting aggregator"

$SCRIPT_PATH/update-build-input.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/update-build-input-ouptut.txt
checkForErrorExit $? "Error occurred while updating build input"

pushd "$aggDir"
git commit -m "Build input for build $BUILD_ID"
# exits with 1 here? 
#checkForErrorExit $? "Error occurred during commit of build_id"

# just echos, for the moment
$GIT_PUSH origin HEAD
#checkForErrorExit $? "Error occurred during push of build_id commit"
popd

$SCRIPT_PATH/tag-build-input.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/tag-build-input-ouptut.txt
checkForErrorExit $? "Error occurred during tag of build input"

$SCRIPT_PATH/install-parent.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/install-parent-ouptut.txt
checkForErrorExit $? "Error occurred during install parent script"

$SCRIPT_PATH/pom-version-updater.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/pom-version-updater-ouptut.txt
checkForErrorExit $? "Error occurred during pom version updater"


$SCRIPT_PATH/run-maven-build.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/run-maven-build-ouptut.txt
checkForErrorExit $? "Error occurred during run maven build"

$SCRIPT_PATH/gather-parts.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/gather-parts-ouptut.txt
checkForErrorExit $? "Error occurred during gather parts"

#$SCRIPT_PATH/parse-logs.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/parse-logs-ouptut.txt
#checkForErrorExit $? "Error occurred during parse-logs"

/bin/bash $SCRIPT_PATH/publish-eclipse.sh $BUILD_ENV_FILE
checkForErrorExit $? "Error occurred during parse-logs"


