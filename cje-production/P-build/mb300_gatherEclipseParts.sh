#!/bin/bash

#*******************************************************************************
# Copyright (c) 2019, 2021 IBM Corporation and others.
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

mkdir -p $CJE_ROOT/$UPDATES_DIR/$BUILD_ID

JavaCMD=${JAVA_HOME}/bin/java

# gather maven properties
cp $CJE_ROOT/$AGG_DIR/eclipse-platform-parent/target/mavenproperties.properties  $CJE_ROOT/$DROP_DIR/$BUILD_ID/mavenproperties.properties

# gather repo
echo $PATCH_BUILD
PATCH_BUILD_GENERIC=java17patch
REPO_DIR=$ECLIPSE_BUILDER_DIR/$PATCH_BUILD/eclipse.releng.repository.$PATCH_BUILD_GENERIC/target/repository
  
if [ -d $REPO_DIR ]; then
  pushd $REPO_DIR
  cp -r * $CJE_ROOT/$UPDATES_DIR/$BUILD_ID
  popd
fi
