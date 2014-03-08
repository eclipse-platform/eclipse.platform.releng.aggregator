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

export BUILD_TIME_PATCHES=${BUILD_TIME_PATCHES:-false}

# force "test build" if doing patch build (since, it really is for tests) 
# and this simply makes sure a human does not forget the "-t" and end up 
# trying to push some patched build (and promoting to downloads). 

if [[ "${BUILD_TIME_PATCHES}" == "true" ]]
then
    testbuildonly=true
fi

# remember, local "test builds" that use this script must change
# or override 'GIT_PUSH' to simply echo, not actually push. Only 
# e4Build should 'push' these tags.
#GIT_PUSH='echo git push'
if [[ "${testbuildonly}" == "true" ]]
then
    GIT_PUSH='echo no git push since testbuildonly'
fi
if [[ "${BUILD_TIME_PATCHES}" == "true" ]]
then
    GIT_PUSH='echo no git push since testbuildonly AND patched build'
fi
if [[ "${BUILD_TYPE}" == "N" ]]
then
    GIT_PUSH='echo no git push done since Nightly'
fi
GIT_PUSH=${GIT_PUSH:-'git push'}



export SCRIPT_PATH="${BUILD_ROOT}/production"


source "${SCRIPT_PATH}/build-functions.shsource"

source "${INITIAL_ENV_FILE}"

# BUILD_KIND allows fine tuning of how promoted, tested, download site name, etc. 
# CBI is only primary production build value. In future may use special values for 
# special cases, such as TEST, JAVA8, etc.
export BUILD_KIND=${BUILD_KIND:-CBI}

cd $BUILD_ROOT

buildrc=0

# derived values

# correct valuse for cbi-jdt-repo.url and cbi-jdt-version are 
# codified in the parent pom. These variables give an easy way 
# to test "experimental versions" in production build. 

if [[ -n $CBI_JDT_REPO_URL ]]
then
   export CBI_JDT_REPO_URL_ARG="-Dcbi-jdt-repo.url=$CBI_JDT_REPO_URL"
fi
if [[ -n $CBI_JDT_VERSION ]]
then
   export CBI_JDT_VERSION_ARG="-Dcbi-jdt-version=$CBI_JDT_VERSION"
fi

BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
export buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BUILD_ID" "$STREAM" )
if [[ -z "${buildDirectory}" ]]
then
    echo "PROGRAM ERROR: buildDirectory returned from fn-build-dir was empty"
    exit 1
else 
    # this should be when we first create buildDirectory
    echo "Making buildDirectory: ${buildDirectory}"
    mkdir -p "${buildDirectory}"
    # it appears GID bit is not always set correctly, so we'll do so explicitly
    chmod -c g+s "${buildDirectory}"
fi
export logsDirectory="${buildDirectory}/buildlogs"
mkdir -p "${logsDirectory}"
checkForErrorExit $? "Could not create buildlogs directory: ${logsDirectory}"

LOG=$buildDirectory/buildlogs/buildOutput.txt
#exec >>$LOG 2>&1

BUILD_PRETTY_DATE=$( date --date='@'$RAWDATE )
TIMESTAMP=$( date +%Y%m%d-%H%M --date='@'$RAWDATE )

# TRACE_OUTPUT is not normally used. But, it comes in handy for debugging
# when output from some functions can not be written to stdout or stderr 
# (due to the nature of the function ... it's "output" being its returned value).
# When needed for local debugging, usually more convenient to provide 
# a value relative to PWD in startup scripts. 
export TRACE_OUTPUT=${TRACE_OUTPUT:-$buildDirectory/buildlogs/trace_output.txt}
echo $BUILD_PRETTY_DATE > ${TRACE_OUTPUT}

# These files have variable/value pairs for this build, suitable for use in 
# shell scripts, PHP files, or as Ant (or Java) properties
export BUILD_ENV_FILE=${buildDirectory}/buildproperties.shsource
export BUILD_ENV_FILE_PHP=${buildDirectory}/buildproperties.php
export BUILD_ENV_FILE_PROP=${buildDirectory}/buildproperties.properties

gitCache=$( fn-git-cache "$BUILD_ROOT" "$BRANCH" )
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )
export LOCAL_REPO="${BUILD_ROOT}"/localMavenRepo

# In production builds, we normally specify CLEAN_LOCAL, 
# and remove any existing LOCAL_REPO, and re-fetch. 
# But CLEAN_LOCAL can be overridden for remote builds, quick test builds, 
# etc. 
export CLEAN_LOCAL=${CLEAN_LOCAL:-true}
# We "remove" by moving to backup, in case there's ever any reason to 
# compare "what's changed". 
if [[ -d ${LOCAL_REPO} && "${CLEAN_LOCAL}" == "true" ]]
then
    # remove existing backup, if it exists
    rm -fr ${LOCAL_REPO}.bak 2>/dev/null
    mv ${LOCAL_REPO} ${LOCAL_REPO}.bak 
fi
export STREAMS_PATH="${aggDir}/streams"

BUILD_TYPE_NAME="Integration"
if [ "$BUILD_TYPE" = M ]; then
    BUILD_TYPE_NAME="Maintenance"
elif [ "$BUILD_TYPE" = N ]; then
    BUILD_TYPE_NAME="Nightly (HEAD)"
elif [ "$BUILD_TYPE" = X ]; then
    BUILD_TYPE_NAME="Experimental Branch"
elif [ "$BUILD_TYPE" = Y ]; then
    BUILD_TYPE_NAME="Experimental Branch"
elif [ "$BUILD_TYPE" = P ]; then
    BUILD_TYPE_NAME="Patch"
elif [ "$BUILD_TYPE" = S ]; then
    BUILD_TYPE_NAME="Stable (Milestone)"
fi

# we need this value for later, post build processing, so want it saved to variables file. 
# We can either compute here, 
# or just make sure it matches that is in parent pom for various streams (e.g. 4.3 vs 4.4)
# and build types, (e.g. M vs. I). For N builds, we still use "I". 
# 5/31013. We no longer do the post build comparator, but leaving it, just 
# in case we want an occasionally double check ... but, should never be routine. 
#comparatorRepository=http://download.eclipse.org/eclipse/updates/4.4-I-builds
comparatorRepository=NOT_CURRENTLY_USED

GET_AGGREGATOR_BUILD_LOG="${logsDirectory}/mb010_get-aggregator_output.txt"
TAG_BUILD_INPUT_LOG="${logsDirectory}/mb030_tag_build_input_output.txt"
POM_VERSION_UPDATE_BUILD_LOG="${logsDirectory}/mb050_pom-version-updater_output.txt"
RUN_MAVEN_BUILD_LOG="${logsDirectory}/mb060_run-maven-build_output.txt"
GATHER_PARTS_BUILD_LOG="${logsDirectory}/mb070_gather-parts_output.txt"

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
fn-write-property B_GIT_EMAIL
fn-write-property B_GIT_NAME
fn-write-property COMMITTER_ID
fn-write-property MVN_DEBUG
fn-write-property MVN_QUIET
fn-write-property SIGNING
fn-write-property REPO_AND_ACCESS
fn-write-property MAVEN_BREE
fn-write-property GIT_PUSH
fn-write-property LOCAL_REPO
fn-write-property SCRIPT_PATH
fn-write-property STREAMS_PATH
fn-write-property BUILD_KIND
fn-write-property CBI_JDT_REPO_URL
fn-write-property CBI_JDT_REPO_URL_ARG
fn-write-property CBI_JDT_VERSION
fn-write-property CBI_JDT_VERSION_ARG
# any value of interest/usefulness can be added to BUILD_ENV_FILE
if [[ "${testbuildonly}" == "true" ]]
then
    fn-write-property testbuildonly
fi
fn-write-property BUILD_ENV_FILE
fn-write-property BUILD_ENV_FILE_PHP
fn-write-property BUILD_ENV_FILE_PROP
fn-write-property BUILD_ID
fn-write-property BUILD_PRETTY_DATE
fn-write-property BUILD_TYPE_NAME
fn-write-property TRACE_OUTPUT
fn-write-property comparatorRepository
fn-write-property logsDirectory
fn-write-property BUILD_TIME_PATCHES
fn-write-property BUILD_HOME



$SCRIPT_PATH/get-aggregator.sh $BUILD_ENV_FILE 2>&1 | tee ${GET_AGGREGATOR_BUILD_LOG}
# if file exists, then get-aggregator failed
if [[ -f "${buildDirectory}/buildFailed-get-aggregator" ]]
then
    buildrc=1
    # Git sometimes fails (returns non-zero error codes) for "warnings". 
    # In most cases, but not all, these would be considered errors. 
    /bin/grep  "^warning: \|\[ERROR\]"  "${GET_AGGREGATOR_BUILD_LOG}" >> "${buildDirectory}/buildFailed-get-aggregator"
    BUILD_FAILED="${GET_AGGREGATOR_BUILD_LOG}"
    fn-write-property BUILD_FAILED
else
    # if get-aggregator failed, there is no reason to try and update input or run build
    $SCRIPT_PATH/update-build-input.sh $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/mb020_update-build-input_output.txt
    checkForErrorExit $? "Error occurred while updating build input"

    if $BUILD_TIME_PATCHES ; then
        # temp patches for bugs
        # apply the pre-created patch from tempPatches
        # patches created, typically, by navigating to repoToPath, then
        # git diff --no-prefix > ../eclipse.platform.releng.aggregator/production/tempPatches/<patchFileName>
        # (then commit and push change to "tempPatches" directory, with normal ID, not with build id)

        repoToPatch=eclipse.jdt.core
        patchFile=jdtComparatorFix.patch
        echo "INFO: apply patch file, $patchFile, in repo $repoToPatch"
        patch -p0  --backup -d $aggDir/$repoToPatch  -i $aggDir/production/tempPatches/$patchFile
        #checkForErrorExit $? "Error occurred applying patch"

        # Note: to "simulate" qualifier increases, when needed,
        # the fix/patch must be "committed" (to build repo, not pushed to origin).
        # This requires more effort to "reset" ... say to HEAD~1, or re-clone the repo, 
        # or else the 'checkout/pull' in next run will not succeed.
        echo "INFO: commit to build machine repository (no push): $repoToPatch"
        pushd $aggDir/$repoToPatch/
        git commit --all -m "temp patch for testing" 
        #checkForErrorExit $? "Error occurred committing patch"
        popd

        # Note: to "simulate" qualifier increases, when needed,
        # the fix/patch must be "committed" (to build repo, not pushed to origin).
        # This requires more effort to "reset" ... say to HEAD~1, or re-clone the repo, 
        # or else the 'checkout/pull' in next run will not succeed.
        #pushd $aggDir/$repoToPatch
        #git commit --all -m "temp patch for testing" 
        #checkForErrorExit $? "Error occurred committing patch"
        #popd

        #repoToPatch=rt.equinox.p2
        #patchFile=p2SourceFix.patch
        #echo "INFO: apply patch file, $patchFile, in repo $repoToPatch"

        #patch -p0  --backup -d $aggDir/$repoToPatch  -i $aggDir/production/tempPatches/$patchFile
        #checkForErrorExit $? "Error occurred applying patch"


    fi 

    # We always make tag commits, if build successful or not, but don't push
    # back to origin if doing N builds or test builds.
    pushd "$aggDir"
    git commit -m "Build input for build $BUILD_ID"
    echo "RC from commit with $BUILD_ID: $?"
    # exits with 1 here ... with warning, if commit already exists?  
    #checkForErrorExit $? "Error occurred during commit of build_id"

    # GIT_PUSH just echos, for N builds and test builds
    $GIT_PUSH origin HEAD
    #checkForErrorExit $? "Error occurred during push of build_id commit"
    popd

    $SCRIPT_PATH/tag-build-input.sh $BUILD_ENV_FILE 2>&1 | tee $TAG_BUILD_INPUT_LOG
    checkForErrorExit $? "Error occurred during tag of build input"

    # At this point, everything should be checked out, updated, and tagged
    # (tagged unless N build or test build) 
    # So is a good point to capture listing of build input to directory.txt file.
    # TODO: hard to find good/easy git commands that work for any repo, 
    # to query actual branches/commits/tags on remote, in a reliable way?
    pushd "$aggDir"
    # = = = = directtory.txt section
    echo "# Build ${BUILD_ID}, ${BUILD_PRETTY_DATE}" > ${buildDirectory}/directory.txt

    echo "# " >> ${buildDirectory}/directory.txt
    echo "# This is simply a listing of repositories.txt. Remember that for N-builds, "  >> ${buildDirectory}/directory.txt
    echo "# 'master' is always used, and N-builds are not tagged."  >> ${buildDirectory}/directory.txt
    echo "# I and M builds are tagged with buildId: ${BUILD_ID} "  >> ${buildDirectory}/directory.txt
    echo "# (when repository is a branch, which it typcally is)."  >> ${buildDirectory}/directory.txt
    echo "# " >> ${buildDirectory}/directory.txt

    if [[ $BUILD_TYPE =~ [IMXYP] ]]
    then
        AGGRCOMMIT=$( git rev-parse HEAD )
        echo "eclipse.platform.releng.aggregator TAGGED: ${BUILD_ID}"  >> ${buildDirectory}/directory.txt
        echo "       http://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/commit/?id=${AGGRCOMMIT}"  >> ${buildDirectory}/directory.txt
    fi

    echo "# " >> ${buildDirectory}/directory.txt
    echo "# .../streams/repositories.txt" >> ${buildDirectory}/directory.txt
    echo "# " >> ${buildDirectory}/directory.txt
    cat $STREAMS_PATH/repositories.txt >> ${buildDirectory}/directory.txt
    echo "# " >> ${buildDirectory}/directory.txt


    # = = = = relengirectory.txt section
    echo "# " >> ${logsDirectory}/relengdirectory.txt
    echo "# Build Input: " >> ${logsDirectory}/relengdirectory.txt
    $SCRIPT_PATH/git-doclog >> ${logsDirectory}/relengdirectory.txt
    git submodule foreach --quiet $SCRIPT_PATH/git-doclog >> ${logsDirectory}/relengdirectory.txt
    echo "# " >> ${logsDirectory}/relengdirectory.txt
    popd


    $SCRIPT_PATH/pom-version-updater.sh $BUILD_ENV_FILE 2>&1 | tee ${POM_VERSION_UPDATE_BUILD_LOG}
    # if file exists, pom update failed
    if [[ -f "${buildDirectory}/buildFailed-pom-version-updater" ]]
    then
        buildrc=1
        /bin/grep "\[ERROR\]" "${POM_VERSION_UPDATE_BUILD_LOG}" >> "${buildDirectory}/buildFailed-pom-version-updater"
        echo "BUILD FAILED. See ${POM_VERSION_UPDATE_BUILD_LOG}." 
        BUILD_FAILED=${POM_VERSION_UPDATE_BUILD_LOG}
        fn-write-property BUILD_FAILED
    else
        # if updater failed, something fairly large is wrong, so no need to compile
        $SCRIPT_PATH/run-maven-build.sh $BUILD_ENV_FILE 2>&1 | tee ${RUN_MAVEN_BUILD_LOG}
        # if file exists, then run maven build failed.
        if [[ -f "${buildDirectory}/buildFailed-run-maven-build" ]]
        then
            buildrc=1
            /bin/grep "\[ERROR\]" "${RUN_MAVEN_BUILD_LOG}" >> "${buildDirectory}/buildFailed-run-maven-build"
            BUILD_FAILED=${RUN_MAVEN_BUILD_LOG}
            fn-write-property BUILD_FAILED
            # TODO: eventually put in more logic to "track" the failure, so
            # proper actions and emails can be sent. For example, we'd still want to 
            # publish what we have, but not start the tests.  
            echo "BUILD FAILED. See ${RUN_MAVEN_BUILD_LOG}." 
        else
            # if build run maven build failed, no need to gather parts
            $SCRIPT_PATH/gather-parts.sh $BUILD_ENV_FILE 2>&1 | tee ${GATHER_PARTS_BUILD_LOG}
            if [[ -f "${buildDirectory}/buildFailed-gather-parts" ]]
            then
                buildrc=1
                /bin/grep -i "ERROR" "${GATHER_PARTS_BUILD_LOG}" >> "${buildDirectory}/buildFailed-gather-parts"
                BUILD_FAILED=${GATHER_PARTS_BUILD_LOG}
                fn-write-property BUILD_FAILED
                echo "BUILD FAILED. See ${GATHER_PARTS_BUILD_LOG}." 
            fi 
        fi 
    fi
fi

$SCRIPT_PATH/publish-eclipse.sh $BUILD_ENV_FILE >$logsDirectory/mb080_publish-eclipse_output.txt
checkForErrorExit $? "Error occurred during publish-eclipse"


# We don't publish repo if there was a build failure, it likely doesn't exist.
if [[ -z "${BUILD_FAILED}" ]] 
then
    $SCRIPT_PATH/publish-repo.sh $BUILD_ENV_FILE >$logsDirectory/mb083_publish-repo_output.txt
    checkForErrorExit $? "Error occurred during publish-repo"
else
    echo "No repo published, since BUILD_FAILED"
fi 


# We don't promote equinox if there was a build failure, and we should not even try to 
# create the site locally, because it depends heavily on having a valid repository to 
# work from. 
if [[ -z "${BUILD_FAILED}" ]] 
then
    $SCRIPT_PATH/publish-equinox.sh $BUILD_ENV_FILE >$logsDirectory/mb085_publish-equinox_output.txt
    checkForErrorExit $? "Error occurred during publish-equinox"
fi 

# if all ended well, put "promotion scripts" in known locations
$SCRIPT_PATH/promote-build.sh $BUILD_KIND $BUILD_ENV_FILE 2>&1 | tee $logsDirectory/mb090_promote-build_output.txt
checkForErrorExit $? "Error occurred during promote-build"

fn-write-property-close

# dump ALL environment variables in case its helpful in documenting or 
# debugging build results or differences between runs, especially on different machines
env 1>$logsDirectory/mb100_all-env-variables_output.txt

echo "Exiting build with RC code of $buildrc"
exit $buildrc
