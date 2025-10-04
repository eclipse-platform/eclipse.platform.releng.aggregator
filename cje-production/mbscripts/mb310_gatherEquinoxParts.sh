#!/bin/bash

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
set -e

if [ $# -ne 1 ]; then
  echo USAGE: $0 env_file
  exit 1
fi

source $CJE_ROOT/scripts/common-functions.shsource
source $1

# gather Equinox Starter Kit
REPO_DIR=$ECLIPSE_BUILDER_DIR/equinox.starterkit.product/target/products
  
if [ -d $REPO_DIR ]; then
  pushd $REPO_DIR
  mkdir -p $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID
  cp org.eclipse.rt.osgistarterkit.product-linux.gtk.x86_64.tar.gz $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID/EclipseRT-OSGi-StarterKit-$BUILD_ID-linux-gtk-x86_64.tar.gz
  cp org.eclipse.rt.osgistarterkit.product-macosx.cocoa.x86_64.tar.gz $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID/EclipseRT-OSGi-StarterKit-$BUILD_ID-macosx-cocoa-x86_64.tar.gz
  cp org.eclipse.rt.osgistarterkit.product-macosx.cocoa.x86_64.dmg $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID/EclipseRT-OSGi-StarterKit-$BUILD_ID-macosx-cocoa-x86_64.dmg
  cp org.eclipse.rt.osgistarterkit.product-macosx.cocoa.aarch64.dmg $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID/EclipseRT-OSGi-StarterKit-$BUILD_ID-macosx-cocoa-aarch64.dmg
  cp org.eclipse.rt.osgistarterkit.product-win32.win32.x86_64.zip $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID/EclipseRT-OSGi-StarterKit-$BUILD_ID-win32-win32-x86_64.zip
  popd
  chmod +x $CJE_ROOT/scripts/notarizeMacApp.sh
  NOTARIZE_LOG_DIR=$CJE_ROOT/notarizeEqLog
  mkdir -p $NOTARIZE_LOG_DIR
  (/bin/bash $CJE_ROOT/scripts/notarizeMacApp.sh "$CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID" EclipseRT-OSGi-StarterKit-$BUILD_ID-macosx-cocoa-x86_64.dmg > $NOTARIZE_LOG_DIR/equinoxX64.log 2>&1)&
fi

# gather Equinox SDK
REPO_DIR=$ECLIPSE_BUILDER_DIR/equinox-sdk/target
  
if [ -d $REPO_DIR ]; then
  pushd $REPO_DIR
  cp equinox-sdk-*-SNAPSHOT.zip $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID/equinox-SDK-$BUILD_ID.zip

  pushd $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID
  unzip -o -j equinox-SDK-$BUILD_ID.zip plugins/*.jar -x plugins/*.source_*

  popd
  popd
fi

#wait for notarization to complete
wait
if [ -d $NOTARIZE_LOG_DIR ]; then
  pushd $NOTARIZE_LOG_DIR
  for i in $(ls *.log)
  do
    echo $i
    cat $i
  done
fi

# publish Equinox
pushd $CJE_ROOT
mkdir -p $ECLIPSE_BUILDER_DIR/equinox/$TMP_DIR
ANT_SCRIPT=$ECLIPSE_BUILDER_DIR/equinox/helper.xml
${JAVA_HOME}/bin/java -jar $LAUNCHER_JAR \
  -application org.eclipse.ant.core.antRunner \
  -buildfile $ANT_SCRIPT \
  -data $CJE_ROOT/$TMP_DIR/workspace-publishEquinox \
  -DEBuilderDir=$ECLIPSE_BUILDER_DIR \
  -DbuildDir=$BUILD_ID \
  -DbuildDirectory=$CJE_ROOT/$EQUINOX_DROP_DIR \
  -DbuildId=$BUILD_ID \
  -DbuildRepo=$PLATFORM_REPO_DIR \
  -DbuildType=$BUILD_TYPE \
  -DpostingDirectory=$CJE_ROOT/$DROP_DIR/$BUILD_ID \
  -DequinoxPostingDirectory=$CJE_ROOT/$EQUINOX_DROP_DIR \
  -DeqpublishingContent=$ECLIPSE_BUILDER_DIR/equinox/publishingFiles \
  -DindexFileName=index.php \
  -Dequinox.build.configs=$ECLIPSE_BUILDER_DIR/equinox/buildConfigs \
  -Dbase.builder=$CJE_ROOT/$BASEBUILDER_DIR \
  -Djava.io.tmpdir=$CJE_ROOT/$TMP_DIR \
  -v \
  publish
popd

