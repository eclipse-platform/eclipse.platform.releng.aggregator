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
SCRIPT_PATH=${SCRIPT_PATH:-$(pwd)}
popd >/dev/null

source $SCRIPT_PATH/build-functions.sh

source "$1"


cd $BUILD_ROOT

# derived values
gitCache=$( fn-git-cache "$BUILD_ROOT" "$BRANCH" )
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )
buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BRANCH" "$BUILD_ID" "$STREAM" )

if [ -z "$BUILD_ID" ]; then
	BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
fi

fn-pom-version-updater "$aggDir" "$LOCAL_REPO"
fn-pom-version-report "$BUILD_ID" "$aggDir"  "$buildDirectory" 

