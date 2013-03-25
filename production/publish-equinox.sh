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

# USAGE: fn-publish-equinox BUILD_TYPE BUILD_STREAM BUILD_ID REPO_DIR BUILD_DIR BASEBUILDER_LAUNCHER
#   BUILD_TYPE: I
#   BUILD_STREAM: 4.2.2
#   BUILD_ID: I20121116-0700
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   BUILD_DIR: /shared/eclipse/builds/R4_2_maintenance/dirs/M20121120-1747
#   BASEBUILDER_LAUNCHER: /shared/eclipse/builds/R4_2_maintenance/org.eclipse.releng.basebuilder_R3_7/plugins/org.eclipse.equinox.launcher_1.2.0.v20110502.jar
fn-publish-equinox () 
{
    BUILD_TYPE="$1"; shift
    BUILD_STREAM="$1"; shift
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    BUILD_DIR="$1"; shift
    BASEBUILDER_LAUNCHER="$1"; shift
    pushd "$BUILD_DIR"
    java -jar "$BASEBUILDER_LAUNCHER" \
        -application org.eclipse.ant.core.antRunner \
        -v \
        -buildfile "$REPO_DIR"/eclipse.platform.releng.tychoeclipsebuilder/equinox/helper.xml \
        -Dequinox.build.configs="$REPO_DIR"/eclipse.platform.releng.tychoeclipsebuilder/equinox/buildConfigs \
        -DbuildId="$BUILD_ID" \
        -DbuildRepo="$REPO_DIR"/eclipse.platform.repository/target/repository \
        -DpostingDirectory=$(dirname "$BUILD_DIR") \
        -DequinoxPostingDirectory="$BUILD_ROOT/siteDir/equinox/drops" \
        -DeqpublishingContent="$REPO_DIR"/eclipse.platform.releng.tychoeclipsebuilder/equinox/publishingFiles \
        -DbuildLabel="$BUILD_ID" \
        -Dhudson=true \
        -DeclipseStream=$BUILD_STREAM \
        -DbuildType="$BUILD_TYPE" \
        -Dbase.builder=$(dirname $(dirname "$BASEBUILDER_LAUNCHER" ) ) \
        -DbuildDirectory=$(dirname "$BUILD_DIR") \
        publish
    popd
}



cd $BUILD_ROOT

# derived values
gitCache=$( fn-git-cache "$BUILD_ROOT" "$BRANCH" )
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )
repositories=$( echo $STREAMS_PATH/repositories.txt )


if [ -z "$BUILD_ID" ]; then
    BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
fi

buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BRANCH" "$BUILD_ID" "$STREAM" )
basebuilderDir=$( fn-basebuilder-dir "$BUILD_ROOT" "$BRANCH" "$BASEBUILDER_TAG" )

fn-checkout-basebuilder "$basebuilderDir" "$BASEBUILDER_TAG"

launcherJar=$( fn-basebuilder-launcher "$basebuilderDir" )

#fn-gather-compile-logs "$BUILD_ID" "$aggDir" "$buildDirectory"
#fn-parse-compile-logs "$BUILD_ID" \
    #    "$aggDir"/eclipse.platform.releng.tychoeclipsebuilder/eclipse/helper.xml \
    #"$buildDirectory" "$launcherJar"

#fn-publish-eclipse "$BUILD_TYPE" "$STREAM" "$BUILD_ID" "$aggDir" "$buildDirectory" "$launcherJar"
#RC=$?
#if [[ $RC != 0 ]] 
#then
    #    echo "ERROR: Somethign went wrong publishing Eclipse. RC: $RC"
    #exit $RC
#else
    fn-publish-equinox "$BUILD_TYPE" "$STREAM" "$BUILD_ID" "$aggDir" "$buildDirectory" "$launcherJar"
    RC=$?
    if [[ $RC != 0 ]] 
    then
        echo "ERROR: Somethign went wrong publishing Equinox. RC: $RC"
        exit $RC
    fi
#fi
