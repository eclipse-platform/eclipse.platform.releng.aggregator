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
gitCache=$( fn-git-cache "$BUILD_ROOT" "${BRANCH}" )
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )
signingDir=$( fn-git-dir "$gitCache" "$SIGNING_REPO" )

if [ -r "$aggDir" ]; then
	fn-git-clean-aggregator "$aggDir" "${BRANCH}"
	pushd "$aggDir"
	fn-git-pull
	fn-git-submodule-update
	popd
else
	fn-git-clone-aggregator "$gitCache" \
	  $(fn-local-repo "$AGGREGATOR_REPO") "${BRANCH}" 
fi

pushd "$aggDir"
# save current hash tag value for documenting build (e.g. to reproduce, run tests, etc.)
EBUILDER_HASH=$( git show-ref --hash --verify refs/remotes/origin/${BRANCH} )
checkForErrorExit $? "git show-ref --hash failed for refs/remotes/origin/${BRANCH}. Not valid ref?"
# remember, literal name as argument ... its defrefernced in function
fn-write-property EBUILDER_HASH
# write to "directory.txt", as "the new map file" 
# TODO: add "tag" later? Or, write this once tag is known? 
# In particular, this "early one" is the "starting point". 
# By the time we do a build and commit submodules, there would 
# be a different one. I'm thinking its the latter that would be needed 
# to "reproduce a build".
buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BRANCH" "$BUILD_ID" "$STREAM" )
echo "$AGGREGATOR_REPO $BRANCH $EBUILDER_HASH" >> ${buildDirectory}/directory.txt
popd

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
