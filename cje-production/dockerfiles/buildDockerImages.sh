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
#     Sravan Kumar Lakkimsetti - initial API and implementation
#*******************************************************************************

set -e

pushd centos-gtk3-metacity/7-gtk3
echo "Building Centos 7 docker image"
docker build -t sravankumarl/centos-gtk3-metacity:7 .
popd

pushd centos-gtk3-metacity/7-swtBuild
echo "Building Centos 7 docker image"
docker build -t sravankumarl/centos-swt-build:7 .
popd

pushd centos-gtk3-metacity/8-gtk3
echo "Building Centos 8 docker image"
docker build -t sravankumarl/centos-gtk3-metacity:8 .
popd

pushd ubuntu-gtk3-metacity/18.04-gtk3
echo "Building Ubuntu 18.04 docker image"
docker build -t sravankumarl/ubuntu-gtk3-metacity:18.04 .
popd

pushd ubuntu-gtk3-metacity/20.04-gtk3
echo "Building Ubuntu 20.04 docker image"
docker build -t sravankumarl/ubuntu-gtk3-metacity:20.04 .
popd
