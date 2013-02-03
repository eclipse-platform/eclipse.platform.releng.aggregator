#!/usr/bin/env bash
# master script to drive Eclipse Platform builds.
RAWDATE=$( date +%s )

if [ $# -ne 1 ]; then
    echo USAGE: $0 env_file
    exit 1
fi

INITIAL_ENV_FILE=$1

if [ ! -r "$INITIAL_ENV_FILE" ]; then
    echo "$INITIAL_ENV_FILE" cannot be read
    echo USAGE: $0 env_file
    exit 1
fi

export SCRIPT_PATH="${BUILD_ROOT}/production"


source "${SCRIPT_PATH}/build-functions.sh"

source "${INITIAL_ENV_FILE}"


cd $BUILD_ROOT

# derived values


BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BRANCH" "$BUILD_ID" "$STREAM" )
logsDirectory="${buildDirectory}/buildlogs"
mkdir -p "${logsDirectory}"
checkForErrorExit $? "Could not create buildlogs directory"

LOG=$buildDirectory/buildlogs/buildOutput.txt
#exec >>$LOG 2>&1

BUILD_PRETTY_DATE=$( date --date='@'$RAWDATE )

# These files have variable/value pairs for this build, suitable for use in 
# shell scripts, PHP files, or as Ant (or Java) properties
BUILD_ENV_FILE=${buildDirectory}/buildproperties.shsource
BUILD_ENV_FILE_PHP=${buildDirectory}/buildproperties.php
BUILD_ENV_FILE_PROP=${buildDirectory}/buildproperties.properties

gitCache=$( fn-git-cache "$BUILD_ROOT" "$BRANCH" )
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )
export LOCAL_REPO="${BUILD_ROOT}"/localMavenRepo

export STREAMS_PATH="${aggDir}/streams"

BUILD_TYPE_NAME="Integration"
if [ "$BUILD_TYPE" = M ]; then
      BUILD_TYPE_NAME="Maintenance"
  elif [ "$BUILD_TYPE" = N ]; then
      BUILD_TYPE_NAME="Nightly (HEAD)"
  elif [ "$BUILD_TYPE" = S ]; then
      BUILD_TYPE_NAME="Stable (Milestone)"
fi

# These variables, from original env file, are re-written to BUILD_ENV_FILE, 
# with values for this build (some of them computed) partially for documentation, and 
# partially so this build can be re-ran or re-started using it, instead of 
# original env file, which would compute different values (in some cases).
# The function also writes into appropriate PHP files and Properties files. 
fn-write-property PATH
fn-write-property INITIAL_ENV_FILE
fn-write-property BUILD_ROOT
fn-write-property BRANCH
fn-write-property STREAM
fn-write-property BUILD_TYPE
fn-write-property TMP_DIR
fn-write-property JAVA_HOME
fn-write-property MAVEN_OPTS
fn-write-property MAVEN_PATH
fn-write-property AGGREGATOR_REPO
fn-write-property BASEBUILDER_TAG
fn-write-property SIGNING_REPO
fn-write-property SIGNING_BRANCH
fn-write-property B_GIT_EMAIL
fn-write-property B_GIT_NAME
fn-write-property COMMITTER_ID
fn-write-property COMPARATOR
fn-write-property SIGNING
fn-write-property UPDATE_BRANDING
fn-write-property FORCE_LOCAL_REPO
fn-write-property MAVEN_BREE
fn-write-property GIT_PUSH
fn-write-property LOCAL_REPO
fn-write-property INITIAL_ENV_FILE
fn-write-property SCRIPT_PATH
fn-write-property STREAMS_PATH
# any value of interest/usefulness can be added to BUILD_ENV_FILE
fn-write-property BUILD_ENV_FILE
fn-write-property BUILD_ID
fn-write-property BUILD_PRETTY_DATE
fn-write-property BUILD_TYPE_NAME

# dump ALL environment variables in case its helpful in documenting or 
# debugging build results or differences between runs, especially on different machines
env 2>&1 | tee $logsDirectory/all-env-variables.txt

$SCRIPT_PATH/get-aggregator.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/get-aggregator-ouptut.txt
checkForErrorExit $? "Error occurred while getting aggregator"

$SCRIPT_PATH/update-build-input.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/update-build-input-ouptut.txt
checkForErrorExit $? "Error occurred while updating build input"

#if [[ $BUILD_ID =~ [IN] ]] 
#then
# temp hack for bug 398141 and others
# apply the pre-created patch from tempPatches
#echo "INFO: apply temp patch"
#echo "DEBUG: aggDir: $aggDir"
#echo "DEBUG: pwd: $PWD"
#patch -p1  --backup -d $aggDir/rt.equinox.bundles/bundles  -i $aggDir/tempPatches/sbep2.patch
#patch -p1  --backup -d $aggDir/eclipse.platform.ui/features  -i $aggDir/tempPatches/e4rcpsource.patch
#patch -p1  --backup -d $aggDir/rt.equinox.framework/bundles  -i $aggDir/tempPatches/ppc.patch
#checkForErrorExit $? "Error occurred applying patch"
#fi 

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

$SCRIPT_PATH/parse-logs.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/parse-logs-ouptut.txt
checkForErrorExit $? "Error occurred during parse-logs"

$SCRIPT_PATH/publish-eclipse.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/publish-eclipse-ouptut.txt
checkForErrorExit $? "Error occurred during publish-eclipse"

# if all ended well, put "promotion scripts" in known locations
$SCRIPT_PATH/promote-build.sh CBI $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/promote-build-ouptut.txt
checkForErrorExit $? "Error occurred during promote-build"
