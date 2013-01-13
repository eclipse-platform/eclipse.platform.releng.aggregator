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
SCRIPT_PATH=$(pwd)
popd >/dev/null

echo "DEBUG: SCRIPT_PATH: ${SCRIPT_PATH}"

. $SCRIPT_PATH/build-functions.sh

. "$1"


cd $BUILD_ROOT

# derived values
gitCache=$( fn-git-cache "$BUILD_ROOT" "$BRANCH" )
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )
repositories=$( echo $SCRIPT_PATH/repositories.txt )
repoScript=$( echo $SCRIPT_PATH/git-submodule-checkout.sh )

echo "DEBUG: gitCache: ${gitCache}"
echo "DEBUG: aggDir: ${aggDir}"
echo "DEBUG: repositories: ${repositories}"
echo "DEBUG: repoScript: ${repoScript}"


if [ -z "$BUILD_ID" ]; then
	BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
fi


fn-submodule-checkout "$BUILD_ID" "$aggDir" "$repoScript" "$repositories"
fn-add-submodule-updates "$aggDir"
