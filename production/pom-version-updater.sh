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

source $SCRIPT_PATH/build-functions.shsource

cd $BUILD_ROOT

# derived values
gitCache=$( fn-git-cache "$BUILD_ROOT" )
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )
buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BUILD_ID" "$STREAM" )

if [ -z "$BUILD_ID" ]; then
  BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
fi

fn-pom-version-updater "$aggDir" "$LOCAL_REPO" $MVN_DEBUG $MVN_QUIET
RC=$?
if [[ $RC != 0 ]]
then
  buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BUILD_ID" "$STREAM" )
  # create as "indicator file" ... gets filled in more once there is a log to grep
  touch  "${buildDirectory}/buildFailed-pom-version-updater"
  echo "ERROR: fn-pom-version-updater returned non-zero return code: $RC"
  exit $RC1
fi
fn-pom-version-report "$BUILD_ID" "$aggDir"  "$buildDirectory"

