#!/bin/bash -e

#*******************************************************************************
# Copyright (c) 2019, 2025 IBM Corporation and others.
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

mkdir -p $CJE_ROOT/$DROP_DIR/$BUILD_ID/testresults/consolelogs

# gather repo
REPO_ZIP=$PLATFORM_TARGET_DIR/eclipse.platform.repository-${STREAMMajor}.${STREAMMinor}.${STREAMService}-SNAPSHOT.zip
  
if [ -f $REPO_ZIP ]; then
  cp $REPO_ZIP $CJE_ROOT/$DROP_DIR/$BUILD_ID/repository-$BUILD_ID.zip
fi


  # gather sdk
  if [ -d $PLATFORM_PRODUCTS_DIR ]; then
    pushd $PLATFORM_PRODUCTS_DIR
    # sdk
    cp org.eclipse.sdk.ide-linux.gtk.aarch64.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-linux-gtk-aarch64.tar.gz
    cp org.eclipse.sdk.ide-linux.gtk.ppc64le.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-linux-gtk-ppc64le.tar.gz
    cp org.eclipse.sdk.ide-linux.gtk.riscv64.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-linux-gtk-riscv64.tar.gz
    cp org.eclipse.sdk.ide-linux.gtk.x86_64.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-linux-gtk-x86_64.tar.gz
    cp org.eclipse.sdk.ide-macosx.cocoa.x86_64.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-macosx-cocoa-x86_64.tar.gz
    cp org.eclipse.sdk.ide-macosx.cocoa.x86_64.dmg $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-macosx-cocoa-x86_64.dmg
    cp org.eclipse.sdk.ide-macosx.cocoa.aarch64.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-macosx-cocoa-aarch64.tar.gz
    cp org.eclipse.sdk.ide-macosx.cocoa.aarch64.dmg $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-macosx-cocoa-aarch64.dmg
    cp org.eclipse.sdk.ide-win32.win32.x86_64.zip $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-win32-x86_64.zip
    cp org.eclipse.sdk.ide-win32.win32.aarch64.zip $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-SDK-$BUILD_ID-win32-aarch64.zip
    # platform
    cp org.eclipse.platform.ide-linux.gtk.aarch64.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-linux-gtk-aarch64.tar.gz
    cp org.eclipse.platform.ide-linux.gtk.ppc64le.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-linux-gtk-ppc64le.tar.gz
    cp org.eclipse.platform.ide-linux.gtk.riscv64.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-linux-gtk-riscv64.tar.gz
    cp org.eclipse.platform.ide-linux.gtk.x86_64.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-linux-gtk-x86_64.tar.gz
    cp org.eclipse.platform.ide-macosx.cocoa.x86_64.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-macosx-cocoa-x86_64.tar.gz
    cp org.eclipse.platform.ide-macosx.cocoa.x86_64.dmg $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-macosx-cocoa-x86_64.dmg
    cp org.eclipse.platform.ide-macosx.cocoa.aarch64.tar.gz $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-macosx-cocoa-aarch64.tar.gz
    cp org.eclipse.platform.ide-macosx.cocoa.aarch64.dmg $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-macosx-cocoa-aarch64.dmg
    cp org.eclipse.platform.ide-win32.win32.x86_64.zip $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-win32-x86_64.zip
    cp org.eclipse.platform.ide-win32.win32.aarch64.zip $CJE_ROOT/$DROP_DIR/$BUILD_ID/eclipse-platform-$BUILD_ID-win32-aarch64.zip
    popd
    chmod +x $CJE_ROOT/scripts/notarizeMacApp.sh
    NOTARIZE_LOG_DIR=$CJE_ROOT/notarizeLog
    mkdir -p $NOTARIZE_LOG_DIR
    (/bin/bash $CJE_ROOT/scripts/notarizeMacApp.sh "$CJE_ROOT/$DROP_DIR/$BUILD_ID" eclipse-SDK-${BUILD_ID}-macosx-cocoa-aarch64.dmg > $NOTARIZE_LOG_DIR/sdkAarch64.log 2>&1)&
    sleep 18s
    (/bin/bash $CJE_ROOT/scripts/notarizeMacApp.sh "$CJE_ROOT/$DROP_DIR/$BUILD_ID" eclipse-SDK-${BUILD_ID}-macosx-cocoa-x86_64.dmg > $NOTARIZE_LOG_DIR/sdkX64.log 2>&1)&
    sleep 18s
    (/bin/bash $CJE_ROOT/scripts/notarizeMacApp.sh "$CJE_ROOT/$DROP_DIR/$BUILD_ID" eclipse-platform-${BUILD_ID}-macosx-cocoa-aarch64.dmg > $NOTARIZE_LOG_DIR/platformAarch64.log 2>&1)&
    sleep 18s
    (/bin/bash $CJE_ROOT/scripts/notarizeMacApp.sh "$CJE_ROOT/$DROP_DIR/$BUILD_ID" eclipse-platform-${BUILD_ID}-macosx-cocoa-x86_64.dmg > $NOTARIZE_LOG_DIR/platformX64.log 2>&1)&
  fi


  # gather swt zips
  SWT_BUNDLES_DIR=$CJE_ROOT/$AGG_DIR/eclipse.platform.swt/binaries
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

  set -x
  # slice repos
  ANT_SCRIPT=$ECLIPSE_BUILDER_DIR/repos/platformrepo.xml
  if [ -d $PLATFORM_REPO_DIR ]; then
    pushd $PLATFORM_REPO_DIR
    $BASE_BUILDER_ECLIPSE_EXE \
      -application org.eclipse.ant.core.antRunner \
      -buildfile $ANT_SCRIPT \
      -data $CJE_ROOT/$TMP_DIR/workspace-buildrepos \
      -Declipse.build.configs=$ECLIPSE_BUILDER_DIR \
      -DbuildId=$BUILD_ID \
      -DbuildLabel=$BUILD_ID \
      -DbuildRepo=$PLATFORM_REPO_DIR \
      -DbuildDirectory=$CJE_ROOT/$DROP_DIR/$BUILD_ID \
      -DpostingDirectory=$CJE_ROOT/$DROP_DIR \
      -Djava.io.tmpdir=$CJE_ROOT/$TMP_DIR \
      -v
    popd
  fi
  set +x

# gather ecj jars
ECJ_JAR_DIR=$CJE_ROOT/$AGG_DIR/eclipse.jdt.core/org.eclipse.jdt.core.compiler.batch/target
if [ -d $ECJ_JAR_DIR ]; then
  pushd $ECJ_JAR_DIR
  cp org.eclipse.jdt.core.compiler.batch-*-SNAPSHOT.jar $CJE_ROOT/$DROP_DIR/$BUILD_ID/ecj-$BUILD_ID.jar
  cp org.eclipse.jdt.core.compiler.batch-*-SNAPSHOT-sources.jar $CJE_ROOT/$DROP_DIR/$BUILD_ID/ecjsrc-$BUILD_ID.jar
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
$BASE_BUILDER_ECLIPSE_EXE \
  -application org.eclipse.ant.core.antRunner \
  -buildfile $ANT_SCRIPT \
  -data $CJE_ROOT/$TMP_DIR/workspace-verifyCompile \
  -DcjeDir=$CJE_ROOT \
  -DEBuilderDir=$ECLIPSE_BUILDER_DIR \
  -DbuildDirectory=$CJE_ROOT/$DROP_DIR/$BUILD_ID \
  -DbuildLabel=$BUILD_ID \
  -Djava.io.tmpdir=$CJE_ROOT/$TMP_DIR \
  -v \
  verifyCompile
popd

#wait for notarization before checksums and pages got generated.
wait
if [ -d $NOTARIZE_LOG_DIR ]; then
  pushd $NOTARIZE_LOG_DIR
  for i in $(ls *.log)
  do
    echo $i
    cat $i
  done
fi

# publish Eclipse
pushd $CJE_ROOT
ANT_SCRIPT=$ECLIPSE_BUILDER_DIR/eclipse/helper.xml
$BASE_BUILDER_ECLIPSE_EXE \
  -application org.eclipse.ant.core.antRunner \
  -buildfile $ANT_SCRIPT \
  -data $CJE_ROOT/$TMP_DIR/workspace-publish \
  -DAGGR_DIR=$CJE_ROOT/$AGG_DIR \
  -DcjeDir=$CJE_ROOT \
  -DEBuilderDir=$ECLIPSE_BUILDER_DIR \
  -DbuildDirectory=$CJE_ROOT/$DROP_DIR/$BUILD_ID \
  -DbuildLabel=$BUILD_ID \
  -DbuildDir=$BUILD_ID \
  -DbuildRepo=$PLATFORM_REPO_DIR \
  -DbuildType=$BUILD_TYPE \
  -DpublishingContent=$ECLIPSE_BUILDER_DIR/eclipse/publishingFiles \
  -DindexFileName=index.php \
  -Djava.io.tmpdir=$CJE_ROOT/$TMP_DIR \
  -v \
  publish
popd
