#!/usr/bin/env bash
# master script to drive Eclipse Platform builds.


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

SCRIPT_PATH="${BUILD_ROOT}/scripts"

source "${SCRIPT_PATH}/build-functions.sh"

source "${INITIAL_ENV_FILE}"


cd $BUILD_ROOT

# derived values


BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BRANCH" "$BUILD_ID" )
logsDirectory="${buildDirectory}/buildlogs"
mkdir -p "${logsDirectory}"
checkForErrorExit $? "Could not create buildlogs directory"

LOG=$buildDirectory/buildlogs/buildOutput.txt
exec >>$LOG 2>&1

BUILD_ENV_FILE=$logsDirectory/$BUILD_ID.env
gitCache=$( fn-git-cache "$BUILD_ROOT" "$BRANCH" )
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )
export LOCAL_REPO="${BUILD_ROOT}"/localMavenRepo

# Just in case it doesn't exist yet
mkdir -p ${TMP_DIR}

# These variables, from original env file, are re-written to BUILD_ENV_FILE, 
# with values for this build (some of them computed) partially for documentation, and 
# partially so this build can be re-ran or re-started using it, instead of 
# original env file, which would compute different values (in some cases).
echo "export PATH=\"${PATH}\"" >$BUILD_ENV_FILE
echo "export INITIAL_ENV_FILE=\"${INITIAL_ENV_FILE}\"" >>$BUILD_ENV_FILE
echo "export BUILD_ROOT=\"${BUILD_ROOT}\""  >>$BUILD_ENV_FILE
echo "export BRANCH=\"${BRANCH}\"" >>$BUILD_ENV_FILE
echo "export STREAM=\"${STREAM}\"" >>$BUILD_ENV_FILE
echo "export BUILD_TYPE=\"${BUILD_TYPE}\"" >>$BUILD_ENV_FILE
echo "export TMP_DIR=\"${TMP_DIR}\"" >>$BUILD_ENV_FILE 
echo "export JAVA_HOME=\"${JAVA_HOME}\"" >>$BUILD_ENV_FILE
echo "export MAVEN_OPTS=\"${MAVEN_OPTS}\"" >>$BUILD_ENV_FILE
echo "export MAVEN_PATH=\"${MAVEN_PATH}\"" >>$BUILD_ENV_FILE
echo "export AGGREGATOR_REPO=\"${AGGREGATOR_REPO}\"" >>$BUILD_ENV_FILE
echo "export BASEBUILDER_TAG=\"${BASEBUILDER_TAG}\"" >>$BUILD_ENV_FILE
echo "export SIGNING_REPO=\"${SIGNING_REPO}\"" >>$BUILD_ENV_FILE
echo "export SIGNING_BRANCH=\"${SIGNING_BRANCH}\"" >>$BUILD_ENV_FILE
echo "export B_GIT_EMAIL=\"${B_GIT_EMAIL}\"" >>$BUILD_ENV_FILE
echo "export B_GIT_NAME=\"${B_GIT_NAME}\"" >>$BUILD_ENV_FILE
echo "export COMMITTER_ID=\"${COMMITTER_ID}\"" >>$BUILD_ENV_FILE
echo "export COMPARATOR=\"${COMPARATOR}\"" >>$BUILD_ENV_FILE
echo "export SIGNING=\"${SIGNING}\"" >>$BUILD_ENV_FILE
echo "export UPDATE_BRANDING=\"${UPDATE_BRANDING}\"" >>$BUILD_ENV_FILE
echo "export FORCE_LOCAL_REPO=\"${FORCE_LOCAL_REPO}\"" >>$BUILD_ENV_FILE
echo "export MAVEN_BREE=\"${MAVEN_BREE}\"" >>$BUILD_ENV_FILE
echo "export GIT_PUSH=\"${GIT_PUSH}\"" >>$BUILD_ENV_FILE
echo "export LOCAL_REPO=\"${LOCAL_REPO}\"" >>$BUILD_ENV_FILE
echo "export INITIAL_ENV_FILE=\"${INITIAL_ENV_FILE}\""  >>$BUILD_ENV_FILE
# any value of interest/usefulness can be added to BUILD_ENV_FILE
echo "export BUILD_ENV_FILE=\"${BUILD_ENV_FILE}\"" >>$BUILD_ENV_FILE
echo "export BUILD_ID=\"${BUILD_ID}\"" >>$BUILD_ENV_FILE
echo "export BUILD_DATE=\"$(date)\"" >>$BUILD_ENV_FILE

# dump ALL environment variables in case its helpful in documenting or 
# debugging build results or differences between runs, especially on different machines
env 2>&1 | tee $logsDirectory/all-env-variables.txt

$SCRIPT_PATH/get-aggregator.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/get-aggregator-ouptut.txt
checkForErrorExit $? "Error occurred while getting aggregator"

$SCRIPT_PATH/update-build-input.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/update-build-input-ouptut.txt
checkForErrorExit $? "Error occurred while updating build input"

if [[ $BUILD_ID =~ [IN] ]] 
then
# temp hack for bug 398141 and others
# apply the pre-created patch from tempPatches
#echo "INFO: apply temp patch"
#echo "DEBUG: aggDir: $aggDir"
#echo "DEBUG: pwd: $PWD"
#patch -p1  --backup -d $aggDir/rt.equinox.bundles/bundles  -i $aggDir/scripts/tempPatches/sbep2.patch
#patch -p1  --backup -d $aggDir/eclipse.platform.ui/features  -i $aggDir/scripts/tempPatches/e4rcpsource.patch
patch -p1  --backup -d $aggDir/rt.equinox.framework/bundles  -i $aggDir/scripts/tempPatches/ppc.patch
checkForErrorExit $? "Error occurred applying patch"
fi 

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

#$SCRIPT_PATH/parse-logs.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/parse-logs-ouptut.txt
#checkForErrorExit $? "Error occurred during parse-logs"

/bin/bash $SCRIPT_PATH/publish-eclipse.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/publish-eclipse-ouptut.txt
checkForErrorExit $? "Error occurred during publish-eclipse"


