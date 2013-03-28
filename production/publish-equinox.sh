#!/usr/bin/env bash
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

SCRIPT_PATH=${SCRIPT_PATH:-$(pwd)}

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
    buildType=${BUILD_ID:0:1}
    dropDirSegment=${eclipseStreamMajor}${buildType}/siteDir/equinox/drops3
    if [[ $eclipseStreamMajor > 3 ]] 
    then
        dropDirSegment=${eclipseStreamMajor}${buildType}/siteDir/equinox/drops
    fi
    echo $ROOT/$dropDirSegment/$BUILD_ID
}

# USAGE: fn-eq-gather-starterkit BUILD_ID REPO_DIR BUILD_DIR
#   BUILD_ID: I20121116-0700
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   BUILD_DIR: /shared/eclipse/builds/R4_2_maintenance/dirs/M20121120-1747
fn-eq-gather-starterkit () 
{
    if [[ $# != 3 ]]
    then
        echo "PROGRAM ERROR: this function, fn-eq-gather-starterkit, requires 3 arguments"
        return 1
    fi
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    DROP_DIR="$1"; shift
    TARGET_PRODUCTS="$REPO_DIR"/eclipse.platform.releng.tychoeclipsebuilder/equinox.starterkit.product/target/products
    echo "Starting fn-eq-gather-starterkit"
    echo "BUILD_ID: $BUILD_ID"
    echo "REPO_DIR: $REPO_DIR"
    echo "DROP_DIR: $DROP_DIR"
    if [[ ! -d $DROP_DIR ]]
    then
        echo "Making DROP_DIR at $DROP_DIR"
        mkdir -p $DROP_DIR
    fi
    if [[ -d "$TARGET_PRODUCTS" ]]
    then
        pushd "$TARGET_PRODUCTS"

        cp -v org.eclipse.rt.osgistarterkit.product-aix.gtk.ppc64.zip "$DROP_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-aix-gtk-ppc64.zip 
        cp -v org.eclipse.rt.osgistarterkit.product-aix.gtk.ppc.zip "$DROP_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-aix-gtk-ppc.zip 
        cp -v org.eclipse.rt.osgistarterkit.product-hpux.gtk.ia64.zip "$DROP_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-hpux-gtk-ia64.zip 
        cp -v org.eclipse.rt.osgistarterkit.product-linux.gtk.ppc64.tar.gz "$DROP_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-linux-gtk-ppc64.tar.gz 
        cp -v org.eclipse.rt.osgistarterkit.product-linux.gtk.ppc.tar.gz "$DROP_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-linux-gtk-ppc.tar.gz 
        cp -v org.eclipse.rt.osgistarterkit.product-linux.gtk.s390.tar.gz "$DROP_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-linux-gtk-s390.tar.gz 
        cp -v org.eclipse.rt.osgistarterkit.product-linux.gtk.s390x.tar.gz "$DROP_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-linux-gtk-s390x.tar.gz 
        cp -v org.eclipse.rt.osgistarterkit.product-linux.gtk.x86_64.tar.gz "$DROP_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-linux-gtk-x86_64.tar.gz 
        cp -v org.eclipse.rt.osgistarterkit.product-linux.gtk.x86.tar.gz "$DROP_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-linux-gtk-x86.tar.gz 
        cp -v org.eclipse.rt.osgistarterkit.product-macosx.cocoa.x86_64.tar.gz "$DROP_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-macosx-cocoa-x86_64.tar.gz 
        cp -v org.eclipse.rt.osgistarterkit.product-macosx.cocoa.x86.tar.gz "$DROP_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-macosx-cocoa-x86.tar.gz 
        cp -v org.eclipse.rt.osgistarterkit.product-solaris.gtk.sparc.zip "$DROP_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-solaris-gtk-sparc.zip 
        cp -v org.eclipse.rt.osgistarterkit.product-solaris.gtk.x86.zip "$DROP_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-solaris-gtk-x86.zip 
        cp -v org.eclipse.rt.osgistarterkit.product-win32.win32.x86_64.zip "$DROP_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-win32-win32-x86_64.zip 
        cp -v org.eclipse.rt.osgistarterkit.product-win32.win32.x86.zip "$DROP_DIR"/EclipseRT-OSGi-StarterKit-${BUILD_ID}-win32-win32-x86.zip 

        popd
    else
        echo "   ERROR: $TARGET_PRODUCTS did not exist in fn-eq-gather-starterkit"
        return 1
    fi
    echo "Ending fn-eq-gather-starterkit"
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
    BUILD_MACHINE_ROOT=/shared/eclipse/builds
    BUILD_MACHINE_DROP_DIR=$(fn-eq-build-dir "$BUILD_MACHINE_ROOT" "$BUILD_ID" "$BUILD_STREAM")
    BUILD_MACHINE_DROP_DIR_PARENT=$(dirname $BUILD_MACHINE_DROP_DIR)
    fn-eq-gather-starterkit $BUILD_ID $REPO_DIR $BUILD_MACHINE_DROP_DIR
    pushd "$BUILD_DIR"
    java -jar "$BASEBUILDER_LAUNCHER" \
        -application org.eclipse.ant.core.antRunner \
        -v \
        -buildfile "$REPO_DIR"/eclipse.platform.releng.tychoeclipsebuilder/equinox/helper.xml \
        -Dequinox.build.configs="$REPO_DIR"/eclipse.platform.releng.tychoeclipsebuilder/equinox/buildConfigs \
        -DbuildId="$BUILD_ID" \
        -DbuildRepo="$REPO_DIR"/eclipse.platform.repository/target/repository \
        -DpostingDirectory=$BUILD_DIR \
        -DequinoxPostingDirectory=$BUILD_MACHINE_DROP_DIR_PARENT \
        -DeqpublishingContent="$REPO_DIR"/eclipse.platform.releng.tychoeclipsebuilder/equinox/publishingFiles \
        -DbuildLabel="$BUILD_ID" \
        -DEBuilderDir="$REPO_DIR"/eclipse.platform.releng.tychoeclipsebuilder \
        -DeclipseStream=$BUILD_STREAM \
        -DbuildType="$BUILD_TYPE" \
        -Dbase.builder=$(dirname $(dirname "$BASEBUILDER_LAUNCHER" ) ) \
        -DbuildDirectory=$BUILD_MACHINE_DROP_DIR_PARENT \
        publish
    popd
}

cd $BUILD_ROOT

# derived values
gitCache=$( fn-git-cache "$BUILD_ROOT" "$BRANCH" )
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )

if [ -z "$BUILD_ID" ]; then
    BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
fi

buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BUILD_ID" "$STREAM" )
basebuilderDir=$( fn-basebuilder-dir "$BUILD_ROOT" "$BUILD_ID" "$STREAM" )

fn-checkout-basebuilder "$basebuilderDir" "$BASEBUILDER_TAG"

launcherJar=$( fn-basebuilder-launcher "$basebuilderDir" )

    fn-publish-equinox "$BUILD_TYPE" "$STREAM" "$BUILD_ID" "$aggDir" "$buildDirectory" "$launcherJar"
    RC=$?
    if [[ $RC != 0 ]] 
    then
        echo "ERROR: Somethign went wrong publishing Equinox. RC: $RC"
        exit $RC
    fi
