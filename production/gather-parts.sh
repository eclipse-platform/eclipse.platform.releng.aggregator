#!/usr/bin/env bash
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

SCRIPT_PATH=${SCRIPT_PATH:-$(pwd)}

source $SCRIPT_PATH/build-functions.shsource

source "$1"

cd $BUILD_ROOT

# derived values
gitCache=$( fn-git-cache "$BUILD_ROOT" "$BRANCH" )
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )

if [ -z "$BUILD_ID" ]; then
    BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
fi

buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BUILD_ID" "$STREAM")
basebuilderDir=$( fn-basebuilder-dir "$BUILD_ROOT" "$BUILD_ID" "$STREAM" )

$SCRIPT_PATH/getEBuilderForDropDir.sh $buildDirectory $EBUILDER_HASH

fn-checkout-basebuilder "$basebuilderDir" "$BASEBUILDER_TAG"

launcherJar=$( fn-basebuilder-launcher "$basebuilderDir" )

fn-gather-repo "$BUILD_ID" "$aggDir" "$buildDirectory"
fn-gather-sdk "$BUILD_ID" "$aggDir" "$buildDirectory"
fn-gather-platform "$BUILD_ID" "$aggDir" "$buildDirectory"
fn-gather-swt-zips "$BUILD_ID" "$aggDir" "$buildDirectory"
fn-gather-test-zips "$BUILD_ID" "$aggDir" "$buildDirectory"
fn-gather-ecj-jars "$BUILD_ID" "$aggDir" "$buildDirectory"
fn-slice-repos "$BUILD_ID" "$aggDir" "$buildDirectory" "$launcherJar"

