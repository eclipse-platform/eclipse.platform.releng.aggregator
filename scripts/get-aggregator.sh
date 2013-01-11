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
signingDir=$( fn-git-dir "$gitCache" "$SIGNING_REPO" )

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

echo "signingDir: $signingDir"

if [ -r "$signingDir" ]; then
	pushd "$signingDir"
	fn-git-clean
	fn-git-reset
	fn-git-checkout "$SIGNING_BRANCH"
	popd
else
	pushd "$gitCache"
	fn-git-clone $(fn-local-repo "$SIGNING_REPO") "$SIGNING_BRANCH"
	popd
	pushd "$signingDir"
	fn-git-checkout "$SIGNING_BRANCH"
	popd
fi
