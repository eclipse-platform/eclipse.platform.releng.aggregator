#!/bin/bash
#

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

/bin/bash $SCRIPT_PATH/get-aggregator.sh $BUILD_ENV_FILE

/bin/bash $SCRIPT_PATH/update-build-input.sh $BUILD_ENV_FILE
pushd "$aggDir"
git commit -m "Build input for build $BUILD_ID"
$GIT_PUSH origin HEAD
popd

/bin/bash $SCRIPT_PATH/tag-build-input.sh $BUILD_ENV_FILE

/bin/bash $SCRIPT_PATH/install-parent.sh $BUILD_ENV_FILE

/bin/bash $SCRIPT_PATH/pom-version-updater.sh $BUILD_ENV_FILE


/bin/bash $SCRIPT_PATH/run-maven-build.sh $BUILD_ENV_FILE
/bin/bash $SCRIPT_PATH/gather-parts.sh $BUILD_ENV_FILE


