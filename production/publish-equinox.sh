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

# USAGE: fn-eq-build-dir ROOT BUILD_ID STREAM
#   ROOT: /shared/eclipse/builds
#   BUILD_ID: M20121119-1900
#   STREAM: 4.3.0
fn-eq-build-dir () 
{
    ROOT="$1"; shift
    BUILD_ID="$1"; shift
    STREAM="$1"; shift
    eclipseStreamMajor=${STREAM:0:1}
    dropDirSegment=siteDir/equinox/drops3
    if [[ $eclipseStreamMajor > 3 ]] 
    then
        dropDirSegment=siteDir/equinox/drops
    fi
    echo $ROOT/$dropDirSegment/$BUILD_ID
}

# USAGE: fn-eq-gather-starterkit BUILD_ID REPO_DIR BUILD_DIR
#   BUILD_ID: I20121116-0700
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   BUILD_DIR: /shared/eclipse/builds/R4_2_maintenance/dirs/M20121120-1747
fn-eq-gather-starterkit () 
{
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    BUILD_DIR="$1"; shift
    TARGET_PRODUCTS="$REPO_DIR"/eclipse.platform.releng.tychoeclipsebuilder/equinox.starterkit.product/target/products
    if [[ -d "$TARGET_PRODUCTS" ]]
    then
        pushd "$TARGET_PRODUCTS"

        cp org.eclipse.rt.osgistarterkit.product-aix.gtk.ppc64.zip "$BUILD_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-aix.gtk.ppc64.zip 
        cp org.eclipse.rt.osgistarterkit.product-aix.gtk.ppc.zip "$BUILD_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-aix.gtk.ppc.zip 
        cp org.eclipse.rt.osgistarterkit.product-hpux.gtk.ia64.zip "$BUILD_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-hpux.gtk.ia64.zip 
        cp org.eclipse.rt.osgistarterkit.product-linux.gtk.ppc64.tar.gz "$BUILD_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-linux.gtk.ppc64.tar.gz 
        cp org.eclipse.rt.osgistarterkit.product-linux.gtk.ppc.tar.gz "$BUILD_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-linux.gtk.ppc.tar.gz 
        cp org.eclipse.rt.osgistarterkit.product-linux.gtk.s390.tar.gz "$BUILD_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-linux.gtk.s390.tar.gz 
        cp org.eclipse.rt.osgistarterkit.product-linux.gtk.s390x.tar.gz "$BUILD_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-linux.gtk.s390x.tar.gz 
        cp org.eclipse.rt.osgistarterkit.product-linux.gtk.x86_64.tar.gz "$BUILD_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-linux.gtk.x86_64.tar.gz 
        cp org.eclipse.rt.osgistarterkit.product-linux.gtk.x86.tar.gz "$BUILD_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-linux.gtk.x86.tar.gz 
        cp org.eclipse.rt.osgistarterkit.product-macosx.cocoa.x86_64.tar.gz "$BUILD_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-macosx.cocoa.x86_64.tar.gz 
        cp org.eclipse.rt.osgistarterkit.product-macosx.cocoa.x86.tar.gz "$BUILD_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-macosx.cocoa.x86.tar.gz 
        cp org.eclipse.rt.osgistarterkit.product-solaris.gtk.sparc.zip "$BUILD_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-solaris.gtk.sparc.zip 
        cp org.eclipse.rt.osgistarterkit.product-solaris.gtk.x86.zip "$BUILD_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-solaris.gtk.x86.zip 
        cp org.eclipse.rt.osgistarterkit.product-win32.win32.x86_64.zip "$BUILD_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-win32.win32.x86_64.zip 
        cp org.eclipse.rt.osgistarterkit.product-win32.win32.x86.zip "$BUILD_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-win32.win32.x86.zip 

        popd
    else
        echo "   ERROR: $TARGET_PRODUCTS did not exist in $0"
        return 1
    fi
    return 0
}



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
    fn-eq-gather-starterkit $BUILD_ID $REPO_DIR $BUILD_DIR
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
