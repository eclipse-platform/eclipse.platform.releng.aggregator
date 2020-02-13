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
#     Sravan Lakkimsetti - initial API and implementation
#*******************************************************************************
set -e

if [ $# -ne 1 ]; then
  echo USAGE: $0 env_file
  exit 1
fi

source $CJE_ROOT/scripts/common-functions.shsource
source $1

pushd $CJE_ROOT/$DROP_DIR/
if [[ $COMPARATOR_ERRORS == "true" ]]
then
	touch ${BUILD_ID}/buildUnstable
	echo "<p>This build has been marked unstable due to <a href='https://download.eclipse.org/eclipse/downloads/drops4/${BUILD_ID}/buildlogs/comparatorlogs/buildtimeComparatorUnanticipated.log.txt'>unanticipated comparator errors</a></p>">> ${BUILD_ID}/buildUnstable
fi
epDownloadDir=/home/data/httpd/download.eclipse.org/eclipse
dropsPath=${epDownloadDir}/downloads/drops4
scp -r ${BUILD_ID} genie.releng@projects-storage.eclipse.org:${dropsPath}/.
popd
