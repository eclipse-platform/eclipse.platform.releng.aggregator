#!/bin/bash -e

#*******************************************************************************
# Copyright (c) 2019, 2025 IBM Corporation and others.
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

epUpdateDir=/home/data/httpd/download.eclipse.org/eclipse/updates
dropsPath=${epUpdateDir}/${STREAMMajor}.${STREAMMinor}-${BUILD_TYPE}-builds

pushd $CJE_ROOT/$UPDATES_DIR
scp -r ${BUILD_ID} genie.releng@projects-storage.eclipse.org:${dropsPath}/.
popd
