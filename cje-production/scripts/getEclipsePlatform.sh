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
#     Sravan Kumar Lakkimsetti - initial API and implementation
#*******************************************************************************

# Gets latest release version of eclipse platform product

downloadPath=/home/data/httpd/download.eclipse.org/eclipse/downloads/drops4

#get latest release folder
epRelDir=$(ls -d --format=single-column ${downloadPath}/R-*|sort|tail -1)

#get eclipse platform product
cp ${epRelDir}/eclipse-platform-*-linux-gtk-x86_64.tar.gz .

tar xzf eclipse-platform-*-linux-gtk-x86_64.tar.gz
