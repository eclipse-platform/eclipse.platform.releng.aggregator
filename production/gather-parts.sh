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

if [ -z "$BUILD_ID" ]; then
  BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
fi

buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BUILD_ID" "$STREAM")
basebuilderDir=$( fn-basebuilder-dir "$BUILD_ROOT" "$BUILD_ID" "$STREAM" )

# copy "mvn.properties" created/saved by parent pom to buildDirectory,
# so can more easily be used by other scripts, reports, php page?
# Note: likely need to "fixup" some variables to be usable by PHP.
cp "${aggDir}/eclipse-platform-parent/target/mavenproperties.properties" "${buildDirectory}/mavenproperties.properties"

$SCRIPT_PATH/getEBuilderForDropDir.sh $buildDirectory $EBUILDER_HASH

fn-checkout-basebuilder "$basebuilderDir"

launcherJar=$( fn-basebuilder-launcher "$basebuilderDir" )

# if doing PATCH_BUILD we do not need these first 4
if [[ -z "${PATCH_BUILD}" ]]
then
  fn-gather-sdk "$BUILD_ID" "$aggDir" "$buildDirectory"
  fn-gather-platform "$BUILD_ID" "$aggDir" "$buildDirectory"
  fn-gather-swt-zips "$BUILD_ID" "$aggDir" "$buildDirectory"
  fn-gather-test-zips "$BUILD_ID" "$aggDir" "$buildDirectory"
fi
# but we do (always) need these two, at least for JDT patch build
fn-gather-repo "$BUILD_ID" "$aggDir" "$buildDirectory"
fn-gather-ecj-jars "$BUILD_ID" "$aggDir" "$buildDirectory"

# if doing PATCH_BUILD we do not need to slice repos
if [[ -z "${PATCH_BUILD}" ]]
then
  # Note, we check for error here, because of all these functions this is one
  # that I've seen occur once.
  fn-slice-repos "$BUILD_ID" "$aggDir" "$buildDirectory" "$launcherJar"
  RC=$?
  if [[ $RC != 0 ]]
  then
    BUILD_FAILED_OUTPUT="${buildDirectory}/buildFailed-gather-parts"
    echo "   ERROR: a function from gather-parts.sh returned non-zero return code, $RC" >>${BUILD_FAILED_OUTPUT}
    exit $RC
  fi
fi 

exit 0

