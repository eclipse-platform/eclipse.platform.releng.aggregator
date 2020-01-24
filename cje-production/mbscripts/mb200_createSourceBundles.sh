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

cd $CJE_ROOT/gitCache/eclipse.platform.releng.aggregator
(mvn clean verify -f eclipse-platform-sources/pom.xml -DbuildId=$BUILD_ID)&

#creating ebuilder zip for tests use
EBUILDER=eclipse.platform.releng.aggregator
BUILD_DIR=$CJE_ROOT/$DROP_DIR/$BUILD_ID
pushd ${CJE_ROOT}/gitCache
zip -r "${BUILD_DIR}/${EBUILDER}-${EBUILDER_HASH}.zip"  "eclipse.platform.releng.aggregator/production/testScripts"
popd

wait
