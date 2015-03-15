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

source "$1"

SCRIPT_PATH=${SCRIPT_PATH:-$(pwd)}

cd $BUILD_ROOT

source $SCRIPT_PATH/build-functions.shsource


# derived values
gitCache=$( fn-git-cache "$BUILD_ROOT")
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )

if [ -z "$BUILD_ID" ]; then
  BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
fi

buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BUILD_ID" "$STREAM" )
basebuilderDir=$( fn-basebuilder-dir "$BUILD_ROOT" "$BUILD_ID" "$STREAM" )

printf "/n/tINFO: %s/n" "calling getEBuilderForDropDir.sh from publish-eclipse.sh"
$SCRIPT_PATH/getEBuilderForDropDir.sh $buildDirectory $EBUILDER_HASH

fn-checkout-basebuilder "$basebuilderDir"

launcherJar=$( fn-basebuilder-launcher "$basebuilderDir" )

EBuilderDir="$buildDirectory"/eclipse.platform.releng.aggregator/eclipse.platform.releng.tychoeclipsebuilder

# Temporary fork/condition
if [[ "true" == "${USING_TYCHO_SNAPSHOT}" ]]
then
  fn-gather-23-compile-logs "$BUILD_ID" "$aggDir" "$buildDirectory"
else
  fn-gather-compile-logs "$BUILD_ID" "$aggDir" "$buildDirectory"
fi

fn-parse-compile-logs "$BUILD_ID" \
  "${EBuilderDir}/eclipse/helper.xml" \
  "$buildDirectory" "$launcherJar"

fn-summarize-comparator-logs "$BUILD_ID" \
  "${EBuilderDir}/eclipse/buildScripts/eclipse_compare.xml" \
  "$buildDirectory" "$launcherJar"

fn-summarize-apitooling "$BUILD_ID" \
  "${EBuilderDir}/eclipse/buildScripts/api-tools-builder.xml" \
  "$buildDirectory" "$launcherJar"

fn-publish-eclipse "$BUILD_TYPE" "$STREAM" "$BUILD_ID" "$aggDir" "$buildDirectory" "$launcherJar"
