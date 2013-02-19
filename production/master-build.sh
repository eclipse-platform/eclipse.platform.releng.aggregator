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
TIMESTAMP=$( date +%Y%m%d-%H%M --date='@'$RAWDATE )

# These files have variable/value pairs for this build, suitable for use in 
# shell scripts, PHP files, or as Ant (or Java) properties
export BUILD_ENV_FILE=${buildDirectory}/buildproperties.shsource
export BUILD_ENV_FILE_PHP=${buildDirectory}/buildproperties.php
export BUILD_ENV_FILE_PROP=${buildDirectory}/buildproperties.properties

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

RUN_MAVEN_BUILD_LOG="${logsDirectory}/mb060_run-maven-build_output.txt"

# These variables, from original env file, are re-written to BUILD_ENV_FILE, 
# with values for this build (some of them computed) partially for documentation, and 
# partially so this build can be re-ran or re-started using it, instead of 
# original env file, which would compute different values (in some cases).
# The function also writes into appropriate PHP files and Properties files.
# Init once, here at beginning, but don't close until much later since other functions
# may write variables at various points
fn-write-property-init
fn-write-property PATH
fn-write-property INITIAL_ENV_FILE
fn-write-property BUILD_ROOT
fn-write-property BRANCH
fn-write-property STREAM
fn-write-property BUILD_TYPE
fn-write-property TIMESTAMP
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

echo "# Build ${BUILD_ID}, ${BUILD_PRETTY_DATE}" > ${buildDirectory}/directory.txt

$SCRIPT_PATH/get-aggregator.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/mb010_get-aggregator_output.txt
checkForErrorExit $? "Error occurred while getting aggregator"

$SCRIPT_PATH/update-build-input.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/mb020_update-build-input_output.txt
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

#TODO: Should we do this only at end, if build (relatively) successful?
pushd "$aggDir"
git commit -m "Build input for build $BUILD_ID"
# exits with 1 here? 
#checkForErrorExit $? "Error occurred during commit of build_id"

# just echos, for the moment
$GIT_PUSH origin HEAD
#checkForErrorExit $? "Error occurred during push of build_id commit"
popd

$SCRIPT_PATH/tag-build-input.sh $BUILD_ENV_FILE 2>&1 | tee $RUN_MAVEN_BUILD_LOG
checkForErrorExit $? "Error occurred during tag of build input"

$SCRIPT_PATH/install-parent.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/mb040_install-parent_output.txt
checkForErrorExit $? "Error occurred during install parent script"

$SCRIPT_PATH/pom-version-updater.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/mb050_pom-version-updater_output.txt
checkForErrorExit $? "Error occurred during pom version updater"

$SCRIPT_PATH/run-maven-build.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/mb060_run-maven-build_output.txt
# does not seem be be "catching" error code via $?. Perhaps due to tee? 
# errors are "indicated" by special file
if [[ -f "${buildDirectory}/buildFailed-run-maven-build" ]]
then
    mavenBuildFailed=true
    /bin/grep "\[ERROR\]" "${RUN_MAVEN_BUILD_LOG}" >> "${buildDirectory}/buildFailed-run-maven-build"
fi
if [[ "${mavenBuildFailed}" ]] 
then 
    # TODO: eventually put in more logic to "track" the failure, so
    # proper actions and emails can be sent. For example, we'd still want to 
    # publish what we have, but not start the tests.  
    echo "BUILD FAILED. See mb060_run-maven-build_output.txt." 
fi

# if build failed, no need to gather parts
if [[ ! "${mavenBuildFailed}" ]] 
then 
  $SCRIPT_PATH/gather-parts.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/mb070_gather-parts_output.txt
  checkForErrorExit $? "Error occurred during gather parts"
fi 

$SCRIPT_PATH/publish-eclipse.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/mb080_publish-eclipse_output.txt
checkForErrorExit $? "Error occurred during publish-eclipse"

# if all ended well, put "promotion scripts" in known locations
$SCRIPT_PATH/promote-build.sh CBI $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/mb090_promote-build_output.txt
checkForErrorExit $? "Error occurred during promote-build"

fn-write-property-close

# dump ALL environment variables in case its helpful in documenting or 
# debugging build results or differences between runs, especially on different machines
env 2>&1 | tee $logsDirectory/mb100_all-env-variables_output.txt

exit $buildrc
