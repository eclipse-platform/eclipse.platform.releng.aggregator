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

pushd $CJE_ROOT/$TMP_DIR
if [[ -z "${WORKSPACE}" ]]
then
	wget --recursive --no-parent --no-verbose https://$BUILD_TO_COMPARE_SITE/$PREVIOUS_RELEASE_VER/$PREVIOUS_RELEASE_ID &
else
	mkdir -p $CJE_ROOT/$TMP_DIR/$BUILD_TO_COMPARE_SITE/$PREVIOUS_RELEASE_VER
	epDownloadDir=/home/data/httpd/download.eclipse.org/eclipse
	p2RepoPath=${epDownloadDir}/updates
	scp -r genie.releng@projects-storage.eclipse.org:$p2RepoPath/$PREVIOUS_RELEASE_VER/$PREVIOUS_RELEASE_ID $CJE_ROOT/$TMP_DIR/$BUILD_TO_COMPARE_SITE/$PREVIOUS_RELEASE_VER/.
fi
popd
