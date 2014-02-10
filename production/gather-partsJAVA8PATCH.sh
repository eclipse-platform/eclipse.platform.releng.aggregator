#!/usr/bin/env bash
#

# USAGE: fn-gather-repo BUILD_ID REPO_DIR BUILD_DIR
#   BUILD_ID: I20121116-0700
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   BUILD_DIR: /shared/eclipse/builds/R4_2_maintenance/dirs/M20121120-1747
fn-gather-repo-patch () 
{
    checkNArgs $# 3
    if [[ $? != 0 ]]; then return 1; fi
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    BUILD_DIR="$1"; shift
    
    REPOPATH=eclipse.platform.releng.tychoeclipsebuilder/JAVA8PATCH/eclipse.releng.repository.patch/target
    REPO_DIR_REPOSITORY=$REPO_DIR/$REPOPATH/repository
    if [[ -d "$REPO_DIR_REPOSITORY" ]]
    then
        pushd "$REPO_DIR"
        # This will be the http: accessible version, from build machine's BUILD_DIR
        cp -r $REPOPATH/repository $BUILD_DIR
        popd
        #TODO: fix up here, in BUILD_DIR/repository? remove extra ius, add checksums? 
        # And then create (new) zip from that?
        cp $REPO_DIR/$REPOPATH/*SNAPSHOT*zip $BUILD_DIR/
        rename -v s/SNAPSHOT/${BUILD_ID}/ $BUILD_DIR/* $BUILD_DIR/*
    else
        echo "   ERROR: $REPO_DIR_REPOSITORY did not exist in fn-gather-repo"
    fi
}


if [ $# -ne 1 ]; then
    echo USAGE: $0 env_file
    exit 1
fi

if [ ! -r "$1" ]; then
    echo "$1" cannot be read
    echo USAGE: $0 env_file
    exit 1
fi

SCRIPT_PATH=${SCRIPT_PATH:-$(pwd)}

source $SCRIPT_PATH/build-functions.shsource

source "$1"

cd $BUILD_ROOT

# derived values
gitCache=$( fn-git-cache "$BUILD_ROOT" "$BRANCH" )
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )

if [ -z "$BUILD_ID" ]; then
    BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
fi

buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BUILD_ID" "$STREAM")
basebuilderDir=$( fn-basebuilder-dir "$BUILD_ROOT" "$BUILD_ID" "$STREAM" )

# copy "mvn.properties" created/saved by parent pom to buildDirectory, 
# so can more easily be used by other scripts, reports, php page?
# Note: likely need to "fixup" some variables to be usable by PHP. 
cp "${aggDir}/eclipse-platform-parent/target/resources/mavenproperties.properties" "${buildDirectory}/mavenproperties.properties"

fn-gather-repo-patch "$BUILD_ID" "$aggDir" "$buildDirectory"
fn-gather-ecj-jars "$BUILD_ID" "$aggDir" "$buildDirectory"


exit 0

