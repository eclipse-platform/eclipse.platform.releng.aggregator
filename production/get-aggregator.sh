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

pushd $( dirname $0 ) >/dev/null
SCRIPT_PATH=${SCRIPT_PATH:-$(pwd)}
popd >/dev/null

source $SCRIPT_PATH/build-functions.sh

source "$1"


cd $BUILD_ROOT

# derived values
gitCache=$( fn-git-cache "$BUILD_ROOT" "${BRANCH}" )
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )

if [[ -r "$aggDir" ]]
then
    fn-git-clean-aggregator "$aggDir" "${BRANCH}"
    RC=$?
    if [[ $RC == 0 ]]
    then
        pushd "$aggDir"
        fn-git-pull
        RC=$?
        if [[ $RC == 0 ]]
        then
            fn-git-submodule-update
            RC=$?
            popd
        fi
    fi
else
    fn-git-clone-aggregator "$gitCache" \
        $(fn-local-repo "$AGGREGATOR_REPO") "${BRANCH}" 
    RC=$?
fi

buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BUILD_ID" "$STREAM" )

if [[ $RC != 0 ]]
then
    # create as "indicator file" ... gets filled in more once there is a log to grep
    touch  "${buildDirectory}/buildFailed-get-aggregator"
    echo "ERROR: get-aggregator returned non-zero return code: $RC"
    echo "       assuming 'master' for EBUILDER_HASH (for later use), since could not reliably get aggregator."
    EBUILDER_HASH=master
    fn-write-property EBUILDER_HASH
    exit $RC
fi


pushd "$aggDir"
# save current hash tag value for documenting build (e.g. to reproduce, run tests, etc.)
EBUILDER_HASH=$( git show-ref --hash --verify refs/remotes/origin/${BRANCH} )
checkForErrorExit $? "git show-ref --hash failed for refs/remotes/origin/${BRANCH}. Not valid ref?"
# remember, literal name as argument ... its defrefernced in function
fn-write-property EBUILDER_HASH
# write to "directory.txt", as "the new map file" 
# TODO: add this "hash tag" later? Or, write this once tag is known? 
# In particular, this "early one" is the "starting point". 
# By the time we do a build and commit submodules, there would 
# be a different one that is tagged with buildId. I'm thinking it is the latter that would be needed 
# to "reproduce a build" but this early one may be important to debug what went wrong with a build.
buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BUILD_ID" "$STREAM" )
echo "$AGGREGATOR_REPO $BRANCH $EBUILDER_HASH" >> ${buildDirectory}/directory.txt
popd

