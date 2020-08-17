#!/bin/bash -x
#*******************************************************************************
# Copyright (c) 2020 IBM Corporation and others.
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

pushd centos-gtk3-metacity/8-gtk3
echo "Building Centos 8 docker image"
docker build --pull -t eclipse/platformreleng-centos-gtk3-metacity:8 .
popd
