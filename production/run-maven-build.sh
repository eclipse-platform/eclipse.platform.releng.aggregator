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
SCRIPT_PATH=${SCRIPT_PATH:-$(pwd)}
popd >/dev/null

source $SCRIPT_PATH/build-functions.sh

source "$1"


cd $BUILD_ROOT

# derived values
gitCache=$( fn-git-cache "$BUILD_ROOT" "$BRANCH" )
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )

if [ -z "$BUILD_ID" ]; then
    BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
fi

fn-maven-build-aggregator "$BUILD_ID" "$aggDir" "$LOCAL_REPO" $COMPARATOR $SIGNING $UPDATE_BRANDING $MAVEN_BREE
exitCode=$?

# first make sure exit code is well formed
if [[ -z "${exitCode}" ]]
then 
    echo "exitcode was empty"
    exitrc=0
elif [[ "${exitCode}" =~ [0] ]]
then
    echo "exitcode was zero"
    exitrc=0
elif [[ "${exitCode}" =~ ^-?[0-9]+$ ]]  
then
    echo "exitcode was a legal, non-zero numeric return code"
    exitrc=$exitCode
    buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BRANCH" "$BUILD_ID" "$STREAM" )
    # create as "indicator file" ... gets filled in more once there is a log to grep
    touch  "${buildDirectory}/buildFailed-run-maven-build"
else
    echo "exitode was not numeric, so will force to 1"
    exitrc=1
fi  

echo "$( basename $0) exiting with exitrc: $exitrc"
exit $exitrc
