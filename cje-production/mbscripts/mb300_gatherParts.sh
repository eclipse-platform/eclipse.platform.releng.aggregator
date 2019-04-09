#!/bin/bash -x

#*******************************************************************************
# Copyright (c) 2019 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Kit Lo - initial API and implementation
#*******************************************************************************

if [ $# -ne 1 ]; then
  echo USAGE: $0 env_file
  exit 1
fi

source $CJE_ROOT/scripts/common-functions.shsource
source $1

mkdir -p $CJE_ROOT/$DROP_DIR/$BUILD_ID/repository

# gather repo
echo $PATCH_BUILD
if [ -z $PATCH_BUILD ]; then
  REPO_DIR=$CJE_ROOT/$AGG_DIR/eclipse.platform.releng.tychoeclipsebuilder/eclipse.platform.repository/target/repository
else
  PATCH_BUILD_GENERIC=java12patch
  REPO_DIR=$CJE_ROOT/$AGG_DIR/eclipse.platform.releng.tychoeclipsebuilder/$PATCH_BUILD/eclipse.releng.repository.$PATCH_BUILD_GENERIC/target/repository
fi
  
if [ -d $REPO_DIR ]; then
  pushd $REPO_DIR
  cp -r * $CJE_ROOT/$DROP_DIR/$BUILD_ID/repository
  popd
fi

if [ -z $PATCH_BUILD ]; then
  # gather sdk
  TARGET_PRODUCTS_DIR=$CJE_ROOT/$AGG_DIR/eclipse.platform.releng.tychoeclipsebuilder/sdk/target/products
  if [ -d $TARGET_PRODUCTS_DIR ]; then
    pushd $TARGET_PRODUCTS_DIR
    cp org.eclipse.sdk.ide-linux.gtk.ppc64le.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-linux-gtk-ppc64le.tar.gz
    cp org.eclipse.sdk.ide-linux.gtk.x86_64.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-linux-gtk-x86_64.tar.gz
    cp org.eclipse.sdk.ide-macosx.cocoa.x86_64.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-macosx-cocoa-x86_64.tar.gz
    cp org.eclipse.sdk.ide-macosx.cocoa.x86_64.dmg $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-macosx-cocoa-x86_64.dmg
    cp org.eclipse.sdk.ide-win32.win32.x86_64.zip $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-win32-x86_64.zip
    popd
  fi

  # gather platform
  TARGET_PRODUCTS_DIR=$CJE_ROOT/$AGG_DIR/eclipse.platform.releng.tychoeclipsebuilder/platform/target/products
  if [ -d $TARGET_PRODUCTS_DIR ]; then
    pushd $TARGET_PRODUCTS_DIR
    cp org.eclipse.platform.ide-linux.gtk.ppc64le.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-linux-gtk-ppc64le.tar.gz
    cp org.eclipse.platform.ide-linux.gtk.x86_64.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-linux-gtk-x86_64.tar.gz
    cp org.eclipse.platform.ide-macosx.cocoa.x86_64.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-macosx-cocoa-x86_64.tar.gz
    cp org.eclipse.platform.ide-macosx.cocoa.x86_64.dmg $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-macosx-cocoa-x86_64.dmg
    cp org.eclipse.platform.ide-win32.win32.x86_64.zip $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-win32-x86_64.zip
    popd
  fi

  # gather platform sources
  TARBALL_DIR=$CJE_ROOT/$AGG_DIR/eclipse-platform-sources/target/
  if [ -d $TARBALL_DIR ]; then
    pushd $TARBALL_DIR
    cp eclipse-platform-sources-*.tar.xz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-sources-$BUILD_ID.tar.xz
    popd
  fi

  # gather swt zips
  SWT_BUNDLES_DIR=$CJE_ROOT/$AGG_DIR/eclipse.platform.swt.binaries/bundles
  if [ -d $SWT_BUNDLES_DIR ]; then
    pushd $SWT_BUNDLES_DIR
    cp */target/*.zip $CJE_ROOT/$DROP_DIR/$BUILD_ID
    popd
  fi

  # gather test zips
  TEST_ZIP_DIR=$CJE_ROOT/$AGG_DIR/eclipse.platform.releng.tychoeclipsebuilder/eclipse-junit-tests/target
  if [ -d $TEST_ZIP_DIR ]; then
    pushd $TEST_ZIP_DIR
    cp eclipse-junit-tests-bundle.zip $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-Automated-Tests-$BUILD_ID.zip
    popd
  fi

  # gather test framework
  TEST_FRAMEWORK_DIR=$TEST_ZIP_DIR/eclipse-test-framework
  if [ -d $TEST_FRAMEWORK_DIR ]; then
    pushd $TEST_FRAMEWORK_DIR
    zip -r $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-test-framework-$BUILD_ID.zip *
    popd
  fi

  # slice repos
  LAUNCHER_JAR=$(find $CJE_ROOT/$BASEBUILDER_DIR -name org.eclipse.equinox.launcher_*.jar | tail -1)
  ANT_SCRIPT=$CJE_ROOT/$AGG_DIR/eclipse.platform.releng.tychoeclipsebuilder/repos/buildAll.xml
  PLATFORM_REPO_DIR=$CJE_ROOT/$AGG_DIR/eclipse.platform.releng.tychoeclipsebuilder/eclipse.platform.repository/target/repository
  if [ -d $PLATFORM_REPO_DIR ]; then
    pushd $PLATFORM_REPO_DIR
    java -jar $LAUNCHER_JAR \
      -application org.eclipse.ant.core.antRunner \
      -buildfile $ANT_SCRIPT \
      -data $CJE_ROOT/$DROP_DIR/$BUILD_ID/workspace-buildrepos \
      -Declipse.build.configs=$CJE_ROOT/$AGG_DIR/eclipse.platform.releng.tychoeclipsebuilder \
      -DbuildId=$BUILD_ID \
      -DbuildLabel=$BUILD_ID \
      -DbuildRepo=$PLATFORM_REPO_DIR \
      -DbuildDirectory=$CJE_ROOT/$DROP_DIR/$BUILD_ID \
      -DpostingDirectory=$CJE_ROOT/$DROP_DIR \
      -DequinoxPostingDirectory=$CJE_ROOT/siteDir/equinox/drops \
      -Djava.io.tmpdir=$CJE_ROOT/$TMP_DIR
    popd
  fi
fi

# gather ecj jars
ECJ_JAR_DIR=$CJE_ROOT/$AGG_DIR/eclipse.jdt.core/org.eclipse.jdt.core/target
if [ -d $ECJ_JAR_DIR ]; then
  pushd $ECJ_JAR_DIR
  cp org.eclipse.jdt.core-*-SNAPSHOT-batch-compiler.jar $CJE_ROOT/$DROP_DIR/$BUILD_ID/ecj-$BUILD_ID.jar
  cp org.eclipse.jdt.core-*-SNAPSHOT-batch-compiler-src.jar $CJE_ROOT/$DROP_DIR/$BUILD_ID/ecjsrc-$BUILD_ID.jar
  popd
fi

# gather buildnotes
if [ -d $CJE_ROOT/$AGG_DIR ]; then
  pushd $CJE_ROOT/$AGG_DIR
  buildnotesDir=$CJE_ROOT/$DROP_DIR/$BUILD_ID/buildnotes
  mkdir -p $buildnotesDir
  find . -name buildnotes_*.html -exec rsync '{}' $buildnotesDir \;
  popd
fi

# gather artifactcomparisons
if [ -d $CJE_ROOT/$AGG_DIR ]; then
  pushd $CJE_ROOT/$AGG_DIR
  comparatorlogsDir=$CJE_ROOT/$DROP_DIR/$BUILD_ID/buildlogs/comparatorlogs
  mkdir -p $comparatorlogsDir
  find . -regex .*target/artifactcomparison -type d -exec zip -r $comparatorlogsDir/artifactcomparisons.zip '{}' \;
  popd
fi
