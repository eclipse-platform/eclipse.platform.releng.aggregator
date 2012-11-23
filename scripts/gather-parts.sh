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
repositories=$( echo $SCRIPT_PATH/repositories.txt )


if [ -z "$BUILD_ID" ]; then
	BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
fi

buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BRANCH" "$BUILD_ID" )

fn-gather-repo "$BUILD_ID" "$aggDir" "$buildDirectory"
fn-gather-static-drop "$BUILD_ID" "$aggDir" "$buildDirectory"

fn-gather-sdk "$BUILD_ID" "$aggDir" "$buildDirectory"
fn-gather-platform "$BUILD_ID" "$aggDir" "$buildDirectory"
fn-gather-swt-zips "$BUILD_ID" "$aggDir" "$buildDirectory"
fn-gather-main-index "$BUILD_ID" "$aggDir" "$buildDirectory" "$STREAM" "$BUILD_TYPE" "$BUILD_DATE"
