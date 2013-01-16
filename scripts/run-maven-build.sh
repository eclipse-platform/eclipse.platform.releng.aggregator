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
localRepo=$gitCache/localMavenRepo


if [ -z "$BUILD_ID" ]; then
	BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
fi

export JAVA_HOME="$JAVA_HOME"
export MAVEN_OPTS="$MAVEN_OPTS"
export PATH=${JAVA_HOME}/bin:${MAVEN_PATH}:$PATH

if $SIGNING; then
	fn-maven-signer-install "$signingDir" "$localRepo"
fi

fn-maven-parent-install "$aggDir" "$localRepo"

fn-maven-cbi-install "$aggDir" "$localRepo"

fn-maven-build-aggregator "$BUILD_ID" "$aggDir" "$localRepo" $COMPARATOR $SIGNING $UPDATE_BRANDING $MAVEN_BREE
