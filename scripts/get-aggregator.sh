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

. $SCRIPT_PATH/build-functions.sh

. "$1"


cd $BUILD_ROOT

# derived values
gitCache=$( fn-git-cache "$BUILD_ROOT" "$BRANCH" )
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )

if [ -r "$aggDir" ]; then
	fn-git-clean-aggregator "$aggDir" "$BRANCH"
	pushd "$aggDir"
	fn-git-pull
	fn-git-submodule-update
	popd
else
	fn-git-clone-aggregator "$gitCache" \
	  $(fn-local-repo "$AGGREGATOR_REPO") "$BRANCH" 
fi

