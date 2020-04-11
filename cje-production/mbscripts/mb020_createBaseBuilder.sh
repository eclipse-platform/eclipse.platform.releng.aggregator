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
#     Sravan Lakkimsetti - initial API and implementation
#*******************************************************************************
set -e

if [ $# -ne 1 ]; then
  echo USAGE: $0 env_file
  exit 1
fi

source $CJE_ROOT/scripts/common-functions.shsource
source $1

mkdir -p $CJE_ROOT/$TMP_DIR
pushd $CJE_ROOT/$TMP_DIR
wget -O eclipsePlatform.tar.gz https://$DOWNLOAD_HOST/eclipse/downloads/drops4/$PREVIOUS_RELEASE_ID/eclipse-platform-${PREVIOUS_RELEASE_VER}-linux-gtk-x86_64.tar.gz
tar zxf eclipsePlatform.tar.gz
popd

$CJE_ROOT/$TMP_DIR/eclipse/eclipse -nosplash \
  -debug -consolelog -data $CJE_ROOT/$TMP_DIR/workspace-toolsinstall \
  -application org.eclipse.equinox.p2.director \
  -repository ${ECLIPSE_RUN_REPO},${BUILDTOOLS_REPO},${WEBTOOLS_REPO} \
  -installIU org.eclipse.platform.ide,org.eclipse.pde.api.tools,org.eclipse.releng.build.tools.feature.feature.group,org.eclipse.wtp.releng.tools.feature.feature.group/${WEBTOOLS_VER} \
  -destination $CJE_ROOT/$BASEBUILDER_DIR \
  -profile SDKProfile -vm ${JAVA_HOME}/bin/java

fn-write-property LAUNCHER_JAR \"$(find $CJE_ROOT/$BASEBUILDER_DIR -name org.eclipse.equinox.launcher_*.jar | tail -1)\"
