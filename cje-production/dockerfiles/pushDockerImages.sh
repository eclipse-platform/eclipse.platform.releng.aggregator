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

docker push eclipse/platformreleng-centos-gtk3-metacity:8
docker push eclipse/platformreleng-centos-gtk3-metacity:7
docker push eclipse/platformreleng-centos-swt-build:7
docker push eclipse/platformreleng-ubuntu-gtk3-metacity:18.04
docker push eclipse/platformreleng-ubuntu-gtk3-metacity:20.04
