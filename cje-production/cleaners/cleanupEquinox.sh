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
#     David Williams - initial API and implementation
#*******************************************************************************

remoteBase=/home/data/httpd/download.eclipse.org

equinoxBuilds=$remoteBase/equinox/drops

clean() {
  dir=$1
  prefix=$2
  pushd $dir > /dev/null

  builds=$( ls --format=single-column -d $prefix* | sort | head -n-3 )

  if [[ ! -z $builds ]]; then
    for f in $builds; do
      echo -e "\tDeleting: $f\n"
      rm -rf $f
    done
  fi
  popd > /dev/null
}


echo -e "\n\tCurrent date: $(TZ="America/New_York" date +%Y\ %m%d\ %H:%M)"
echo -e "\tRemoving drops from downloads server at ${equinoxBuilds}\n"
clean $equinoxBuilds I
