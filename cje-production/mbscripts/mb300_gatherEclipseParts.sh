#!/bin/bash

#*******************************************************************************
# Copyright (c) 2019, 2020 IBM Corporation and others.
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
set -e

if [ $# -ne 1 ]; then
  echo USAGE: $0 env_file
  exit 1
fi

source $CJE_ROOT/scripts/common-functions.shsource
source $1

mkdir -p $CJE_ROOT/$DROP_DIR/$BUILD_ID
mkdir -p $CJE_ROOT/$UPDATES_DIR/$BUILD_ID
mkdir -p $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID
mkdir -p $CJE_ROOT/$DROP_DIR/$BUILD_ID/testresults/consolelogs

JavaCMD=${JAVA_HOME}/bin/java

# gather maven properties
cp $CJE_ROOT/$AGG_DIR/eclipse-platform-parent/target/mavenproperties.properties  $CJE_ROOT/$DROP_DIR/$BUILD_ID/mavenproperties.properties

# gather repo
echo $PATCH_BUILD
if [ -z $PATCH_BUILD ]; then
  REPO_DIR=$PLATFORM_REPO_DIR
else
  PATCH_BUILD_GENERIC=java14patch
  REPO_DIR=$ECLIPSE_BUILDER_DIR/$PATCH_BUILD/eclipse.releng.repository.$PATCH_BUILD_GENERIC/target/repository
fi
  
if [ -d $REPO_DIR ]; then
  pushd $REPO_DIR
  cp -r * $CJE_ROOT/$UPDATES_DIR/$BUILD_ID
  popd
fi

if [ -z $PATCH_BUILD ]; then
  # gather sdk
  TARGET_PRODUCTS_DIR=$ECLIPSE_BUILDER_DIR/sdk/target/products
  if [ -d $TARGET_PRODUCTS_DIR ]; then
    pushd $TARGET_PRODUCTS_DIR
    cp org.eclipse.sdk.ide-linux.gtk.ppc64le.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-linux-gtk-ppc64le.tar.gz
    cp org.eclipse.sdk.ide-linux.gtk.x86_64.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-linux-gtk-x86_64.tar.gz
    cp org.eclipse.sdk.ide-macosx.cocoa.x86_64.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-macosx-cocoa-x86_64.tar.gz
    cp org.eclipse.sdk.ide-macosx.cocoa.x86_64.dmg $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-macosx-cocoa-x86_64.dmg
    cp org.eclipse.sdk.ide-win32.win32.x86_64.zip $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-win32-x86_64.zip
    popd
    fn-notarize-macbuild "$CJE_ROOT/$DROP_DIR/$BUILD_ID" eclipse-SDK-${BUILD_ID}-macosx-cocoa-x86_64.dmg
  fi

  # gather platform
  TARGET_PRODUCTS_DIR=$ECLIPSE_BUILDER_DIR/platform/target/products
  if [ -d $TARGET_PRODUCTS_DIR ]; then
    pushd $TARGET_PRODUCTS_DIR
    cp org.eclipse.platform.ide-linux.gtk.ppc64le.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-linux-gtk-ppc64le.tar.gz
    cp org.eclipse.platform.ide-linux.gtk.x86_64.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-linux-gtk-x86_64.tar.gz
    cp org.eclipse.platform.ide-macosx.cocoa.x86_64.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-macosx-cocoa-x86_64.tar.gz
    cp org.eclipse.platform.ide-macosx.cocoa.x86_64.dmg $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-macosx-cocoa-x86_64.dmg
    cp org.eclipse.platform.ide-win32.win32.x86_64.zip $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-win32-x86_64.zip
    popd
    fn-notarize-macbuild "$CJE_ROOT/$DROP_DIR/$BUILD_ID" eclipse-platform-${BUILD_ID}-macosx-cocoa-x86_64.dmg
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
  TEST_ZIP_DIR=$ECLIPSE_BUILDER_DIR/eclipse-junit-tests/target
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
  ANT_SCRIPT=$ECLIPSE_BUILDER_DIR/repos/buildAll.xml
  if [ -d $PLATFORM_REPO_DIR ]; then
    pushd $PLATFORM_REPO_DIR
    java -jar $LAUNCHER_JAR \
      -application org.eclipse.ant.core.antRunner \
      -buildfile $ANT_SCRIPT \
      -data $CJE_ROOT/$TMP_DIR/workspace-buildrepos \
      -Declipse.build.configs=$ECLIPSE_BUILDER_DIR \
      -DbuildId=$BUILD_ID \
      -DbuildLabel=$BUILD_ID \
      -DbuildRepo=$PLATFORM_REPO_DIR \
      -DbuildDirectory=$CJE_ROOT/$DROP_DIR/$BUILD_ID \
      -DpostingDirectory=$CJE_ROOT/$DROP_DIR \
      -DequinoxPostingDirectory=$CJE_ROOT/$EQUINOX_DROP_DIR \
      -Djava.io.tmpdir=$CJE_ROOT/$TMP_DIR \
      -v
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

# verify comparatorlogs
#
# Note: copy mb220_buildSdkPatch.sh.log as mb060_run-maven-build_output.txt for now to avoid changing eclipse_compare.xml
# To-do: modify eclipse_compare.xml to use mb220_buildSdkPatch.sh.log after CJE migration
cp $CJE_ROOT/buildlogs/mb220_buildSdkPatch.sh.log $CJE_ROOT/$DROP_DIR/$BUILD_ID/buildlogs/mb060_run-maven-build_output.txt
#
pushd $CJE_ROOT/$DROP_DIR/$BUILD_ID
ANT_SCRIPT=$ECLIPSE_BUILDER_DIR/eclipse/buildScripts/eclipse_compare.xml
$JavaCMD -jar $LAUNCHER_JAR \
  -application org.eclipse.ant.core.antRunner \
  -buildfile $ANT_SCRIPT \
  -data $CJE_ROOT/$TMP_DIR/workspace-comparatorLogs \
  -DEBuilderDir=$ECLIPSE_BUILDER_DIR \
  -DbuildDirectory=$CJE_ROOT/$DROP_DIR/$BUILD_ID \
  -DbuildId=$BUILD_ID \
  -DbuildLabel=$BUILD_ID \
  -Djava.io.tmpdir=$CJE_ROOT/$TMP_DIR \
  -v \
  compare
popd

# gather compilelogs
if [ -d $CJE_ROOT/$AGG_DIR ]; then
  pushd $CJE_ROOT/$AGG_DIR
  compilelogsDir=$CJE_ROOT/$DROP_DIR/$BUILD_ID/compilelogs/plugins
  for log in $( find $CJE_ROOT/$AGG_DIR -name "compilelogs" -type d ); do
    targetDir=$( dirname $log )
    if [ ! -r $targetDir/MANIFEST.MF ]; then
      echo "** Failed to process $log in $targetDir. Likely compile error. Will backup to source MANIFEST.MF in directory containing target."
      targetDir=$( dirname $targetDir )
      if [ ! -r $targetDir/META-INF/MANIFEST.MF ]; then
        echo "**Failed to process $log in $targetDir."
      else
        bundleId=$( grep Bundle-SymbolicName $targetDir/META-INF/MANIFEST.MF | cut -f2 -d" " | cut -f1 -d\; | tr -d '\f\r\n\t' )
        bundleVersion=$( grep Bundle-Version $targetDir/META-INF/MANIFEST.MF | cut -f2 -d" " | tr -d '\f\r\n\t' )
        mkdir -p $compilelogsDir/${bundleId}_${bundleVersion}
        rsync -vr $log/ $compilelogsDir/${bundleId}_${bundleVersion}/
      fi
    else
      bundleId=$( grep Bundle-SymbolicName $targetDir/MANIFEST.MF | cut -f2 -d" " | cut -f1 -d\; | tr -d '\f\r\n\t' )
      bundleVersion=$( grep Bundle-Version $targetDir/MANIFEST.MF | cut -f2 -d" " | tr -d '\f\r\n\t' )
      mkdir -p $compilelogsDir/${bundleId}_${bundleVersion}
      rsync -vr $log/ $compilelogsDir/${bundleId}_${bundleVersion}/
    fi
  done
  popd
fi

# verify compilelog
pushd $CJE_ROOT/$DROP_DIR/$BUILD_ID
ANT_SCRIPT=$ECLIPSE_BUILDER_DIR/eclipse/helper.xml
$JavaCMD -jar $LAUNCHER_JAR \
  -application org.eclipse.ant.core.antRunner \
  -buildfile $ANT_SCRIPT \
  -data $CJE_ROOT/$TMP_DIR/workspace-verifyCompile \
  -DEBuilderDir=$ECLIPSE_BUILDER_DIR \
  -DbuildDirectory=$CJE_ROOT/$DROP_DIR/$BUILD_ID \
  -DbuildId=$BUILD_ID \
  -DbuildLabel=$BUILD_ID \
  -DpostingDirectory=$CJE_ROOT/$DROP_DIR/$BUILD_ID \
  -Djava.io.tmpdir=$CJE_ROOT/$TMP_DIR \
  -v \
  verifyCompile
popd

# publish Eclipse
pushd $CJE_ROOT
ANT_SCRIPT=$ECLIPSE_BUILDER_DIR/eclipse/helper.xml
$JavaCMD -jar $LAUNCHER_JAR \
  -application org.eclipse.ant.core.antRunner \
  -buildfile $ANT_SCRIPT \
  -data $CJE_ROOT/$TMP_DIR/workspace-publish \
  -DAGGR_DIR=$CJE_ROOT/$AGG_DIR \
  -DEBuilderDir=$ECLIPSE_BUILDER_DIR \
  -DbuildDirectory=$CJE_ROOT/$DROP_DIR/$BUILD_ID \
  -DbuildId=$BUILD_ID \
  -DbuildLabel=$BUILD_ID \
  -DbuildDir=$BUILD_ID \
  -DbuildRepo=$PLATFORM_REPO_DIR \
  -DbuildType=$BUILD_TYPE \
  -DpostingDirectory=$CJE_ROOT/$DROP_DIR \
  -DequinoxPostingDirectory=$BUILD_ROOT/$EQUINOX_DROP_DIR \
  -DpublishingContent=$ECLIPSE_BUILDER_DIR/eclipse/publishingFiles \
  -DdropTemplateFileName=$ECLIPSE_BUILDER_DIR/eclipse/publishingFiles/templateFiles/index.template_$PATCH_OR_BRANCH_LABEL.php \
  -DindexFileName=index.php \
  -DeclipseStream=$STREAM \
  -Dbase.builder=$CJE_ROOT/$BASEBUILDER_DIR \
  -Djava.io.tmpdir=$CJE_ROOT/$TMP_DIR \
  -v \
  publish
popd

comparatorLogMinimumSize=350
comparatorLog=$CJE_ROOT/$DROP_DIR/$BUILD_ID/buildlogs/comparatorlogs/buildtimeComparatorUnanticipated.log.txt

logSize=0
if [[ -e ${comparatorLog} ]]
then
  logSize=$(stat -c '%s' ${comparatorLog} )
  echo -e "DEBUG: comparatorLog found at\n\t${comparatorLog}\n\tWith size of $logSize bytes"
else
  echo -e "DEBUG: comparatorLog was surprisingly not found at:\n\t${comparatorLog}"
fi

if [[ $logSize -gt  ${comparatorLogMinimumSize} ]]
then
  echo -e "DEBUG: found logsize greater an minimum. preparing message using ${link}"
  fn-write-property COMPARATOR_ERRORS "true"
else
  echo -e "DEBUG: comparator logSize of $logSize was not greater than comparatorLogMinimumSize of ${comparatorLogMinimumSize}"
fi

