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
mkdir -p "$buildDirectory"/buildlogs
checkForErrorExit $? "Could not create buildlogs directory"

LOG=$buildDirectory/buildlogs/buildOutput.txt
exec >>$LOG 2>&1

BUILD_ENV_FILE=$buildDirectory/env.$$
gitCache=$( fn-git-cache "$BUILD_ROOT" "$BRANCH" )
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )
localRepo=$gitCache/localMavenRepo


cp "$1" $BUILD_ENV_FILE
echo "BUILD_ENV_FILE=$1" >>$BUILD_ENV_FILE
echo "BUILD_ID=$BUILD_ID" >>$BUILD_ENV_FILE
echo "BUILD_DATE=\"$(date)\"" >>$BUILD_ENV_FILE

$SCRIPT_PATH/get-aggregator.sh $BUILD_ENV_FILE |& tee $buildDirectory/$buildDirectory/get-aggregator-ouptut.txt
checkForErrorExit $? "Error occurred while getting aggregator"

$SCRIPT_PATH/update-build-input.sh $BUILD_ENV_FILE |& tee $buildDirectory/$buildDirectory/update-build-input-ouptut.txt
checkForErrorExit $? "Error occurred while updating build input"

pushd "$aggDir"
git commit -m "Build input for build $BUILD_ID"
# exits with 1 here? 
#checkForErrorExit $? "Error occurred during commit of build_id"

# just echos, for the moment
$GIT_PUSH origin HEAD
#checkForErrorExit $? "Error occurred during push of build_id commit"
popd

$SCRIPT_PATH/tag-build-input.sh $BUILD_ENV_FILE |& tee $buildDirectory/$buildDirectory/tag-build-input-ouptut.txt
checkForErrorExit $? "Error occurred during tag of build input"

$SCRIPT_PATH/install-parent.sh $BUILD_ENV_FILE |& tee $buildDirectory/$buildDirectory/install-parent-ouptut.txt
checkForErrorExit $? "Error occurred during install parent script"

$SCRIPT_PATH/pom-version-updater.sh $BUILD_ENV_FILE |& tee $buildDirectory/$buildDirectory/pom-version-updater-ouptut.txt
checkForErrorExit $? "Error occurred during pom version updater"


$SCRIPT_PATH/run-maven-build.sh $BUILD_ENV_FILE |& tee $buildDirectory/$buildDirectory/run-maven-build-ouptut.txt
checkForErrorExit $? "Error occurred during run maven build"

$SCRIPT_PATH/gather-parts.sh $BUILD_ENV_FILE |& tee $buildDirectory/$buildDirectory/gather-parts-ouptut.txt
checkForErrorExit $? "Error occurred during gather parts"

$SCRIPT_PATH/parse-logs.sh $BUILD_ENV_FILE |& tee $buildDirectory/parse-logs-ouptut.txt
checkForErrorExit $? "Error occurred during parse-logs"


