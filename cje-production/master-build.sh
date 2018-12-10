#!/bin/bash -x
#*******************************************************************************
# Copyright (c) 2018 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Sravan Kumar Lakkimsetti - initial API and implementation
#*******************************************************************************

source common/common-functions.shsource

buildDirectory=$(pwd)/siteDir
BUILD_ENV_FILE=${buildDirectory}/buildproperties.shsource
BUILD_ENV_FILE_PHP=${buildDirectory}/buildproperties.php
BUILD_ENV_FILE_PROP=${buildDirectory}/buildproperties.properties

pushd mbscripts
for i in $(ls |sort)
do
  fn-run-command ./$i $BUILD_ENV_FILE
done
popd
