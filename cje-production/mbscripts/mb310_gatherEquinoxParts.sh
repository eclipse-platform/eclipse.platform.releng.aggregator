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

# gather Equinox Starter Kit
REPO_DIR=$CJE_ROOT/$AGG_DIR/eclipse.platform.releng.tychoeclipsebuilder/equinox.starterkit.product/target/products
  
if [ -d $REPO_DIR ]; then
  pushd $REPO_DIR
  mkdir -p $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID
  cp org.eclipse.rt.osgistarterkit.product-linux.gtk.x86_64.tar.gz $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID/EclipseRT-OSGi-StarterKit-$BUILD_ID-linux-gtk-x86_64.tar.gz
  cp org.eclipse.rt.osgistarterkit.product-macosx.cocoa.x86_64.tar.gz $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID/EclipseRT-OSGi-StarterKit-$BUILD_ID-macosx-cocoa-x86_64.tar.gz
  cp org.eclipse.rt.osgistarterkit.product-macosx.cocoa.x86_64.dmg $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID/EclipseRT-OSGi-StarterKit-$BUILD_ID-macosx-cocoa-x86_64.dmg
  cp org.eclipse.rt.osgistarterkit.product-win32.win32.x86_64.zip $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID/EclipseRT-OSGi-StarterKit-$BUILD_ID-win32-win32-x86_64.zip
  popd
fi

# gather Equinox SDK
REPO_DIR=$CJE_ROOT/$AGG_DIR/eclipse.platform.releng.tychoeclipsebuilder/equinox-sdk/target
  
if [ -d $REPO_DIR ]; then
  pushd $REPO_DIR
  cp equinox-sdk-*-SNAPSHOT.zip $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID/equinox-SDK-$BUILD_ID.zip

  pushd $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID
  unzip -o -j equinox-SDK-$BUILD_ID.zip plugins/*.jar -x plugins/*.source_*

  popd
  popd
fi

# publish Equinox
pushd $CJE_ROOT
mkdir -p $CJE_ROOT/$AGG_DIR/eclipse.platform.releng.tychoeclipsebuilder/equinox/$TMP_DIR
REPO_DIR=$CJE_ROOT/$AGG_DIR/eclipse.platform.releng.tychoeclipsebuilder/eclipse.platform.repository/target/repository
LAUNCHER_JAR=$(find $CJE_ROOT/$BASEBUILDER_DIR -name org.eclipse.equinox.launcher_*.jar | tail -1)
ANT_SCRIPT=$CJE_ROOT/$AGG_DIR/eclipse.platform.releng.tychoeclipsebuilder/equinox/helper.xml
java -jar $LAUNCHER_JAR \
  -application org.eclipse.ant.core.antRunner \
  -buildfile $ANT_SCRIPT \
  -data $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID/workspace-publishEquinox \
  -DEBuilderDir=$CJE_ROOT/$AGG_DIR/eclipse.platform.releng.tychoeclipsebuilder \
  -DbuildDir=$BUILD_ID \
  -DbuildDirectory=$CJE_ROOT/$EQUINOX_DROP_DIR \
  -DbuildId=$BUILD_ID \
  -DbuildRepo=$REPO_DIR \
  -DbuildType=$BUILD_TYPE \
  -DpostingDirectory=$CJE_ROOT/$DROP_DIR/$BUILD_ID \
  -DequinoxPostingDirectory=$CJE_ROOT/$EQUINOX_DROP_DIR \
  -DeqpublishingContent=$CJE_ROOT/$AGG_DIR/eclipse.platform.releng.tychoeclipsebuilder/equinox/publishingFiles \
  -DdropTemplateFileName=$CJE_ROOT/$AGG_DIR/eclipse.platform.releng.tychoeclipsebuilder/eclipse/publishingFiles/templateFiles/index.template_$PATCH_OR_BRANCH_LABEL.php \
  -DindexFileName=index.php \
  -DeclipseStream=$STREAM \
  -Dequinox.build.configs=$CJE_ROOT/$AGG_DIR/eclipse.platform.releng.tychoeclipsebuilder/equinox/buildConfigs \
  -Dbase.builder=$CJE_ROOT/$BASEBUILDER_DIR \
  -Djava.io.tmpdir=$CJE_ROOT/$TMP_DIR \
  -v \
  publish
popd
