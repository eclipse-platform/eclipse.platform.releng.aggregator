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
signingDir=$( fn-git-dir "$gitCache" "$SIGNING_REPO" )


if [ -z "$BUILD_ID" ]; then
    BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
fi

if $SIGNING; then
    fn-maven-signer-install "$signingDir" "$LOCAL_REPO"
fi

fn-maven-cbi-install "$aggDir" "$LOCAL_REPO"
fn-maven-parent-install "$aggDir" "$LOCAL_REPO"

exitCode=$( fn-maven-build-aggregator "$BUILD_ID" "$aggDir" "$LOCAL_REPO" $COMPARATOR $SIGNING $UPDATE_BRANDING $MAVEN_BREE )

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
else
    echo "exitode was not numeric, so will force to 1"
    exitrc=1
fi  

exit $exitrc
