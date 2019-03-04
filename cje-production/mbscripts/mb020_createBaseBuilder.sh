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
#     Sravan Lakkimsetti - initial API and implementation
#*******************************************************************************

if [ $# -ne 1 ]; then
  echo USAGE: $0 env_file
  exit 1
fi

source $CJE_ROOT/scripts/common-functions.shsource
source $1

qualifiedBaseBuilder="$CJE_ROOT/$BASEBUILDER_DIR"
TMP="$CJE_ROOT/$BASEBUILDER_DIR/tmp"
mkdir -p $TMP
pushd $TMP
wget -O eclipsePlatform.tar.gz https://$DOWNLOAD_HOST/eclipse/downloads/drops4/$PREVIOUS_RELEASE_ID/eclipse-platform-${PREVIOUS_RELEASE_VER}-linux-gtk-x86_64.tar.gz&r=1
tar zxf eclipsePlatform.tar.gz
popd

${TMP}/eclipse/eclipse -nosplash \
      -debug -consolelog -data ${TMP}/workspace-toolsinstall \
      -application org.eclipse.equinox.p2.director \
      -repository \
      ${ECLIPSE_RUN_REPO},${BUILDTOOLS_REPO},${WEBTOOLS_REPO} \
      -installIU \
      org.eclipse.platform.ide,org.eclipse.pde.api.tools,org.eclipse.releng.build.tools.feature.feature.group,org.eclipse.wtp.releng.tools.feature.feature.group/${WEBTOOLS_VER},org.apache.derby.core.feature.feature.group \
      -destination \
      ${qualifiedBaseBuilder} \
      -profile \
      SDKProfile
