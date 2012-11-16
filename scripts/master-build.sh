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
gitCache=$( fn-git-cache "$BUILD_ROOT" "$BRANCH" )
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )
localRepo=$gitCache/localMavenRepo


if [ -z "$BUILD_ID" ]; then
	BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
fi

/bin/bash $SCRIPT_PATH/get-aggregator.sh "$1"
/bin/bash $SCRIPT_PATH/update-build-input.sh "$1"
/bin/bash $SCRIPT_PATH/pom-version-updater.sh "$1"

pushd "$aggDir"
git commit -m "Build input for build $BUILD_ID"
echo git push origin HEAD
popd

#/bin/bash $SCRIPT_PATH/tag-build-input.sh "$1"
#/bin/bash $SCRIPT_PATH/run-maven-build.sh "$1"

