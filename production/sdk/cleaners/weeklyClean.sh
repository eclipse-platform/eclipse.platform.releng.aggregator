#!/usr/bin/env bash
#*******************************************************************************
# Copyright (c) 2016 IBM Corporation and others.
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

# To allow this cron job to work from hudson, or traditional crontab
if [[ -z "${WORKSPACE}" ]]
then
  export UTILITIES_HOME=/shared/eclipse
else
  export UTILITIES_HOME=${WORKSPACE}/utilities/production
fi


# Utility to clean download machine
echo -e "\n\tWeekly clean of ${HOSTNAME} download server on $(date )\n"

# clean 4.x M builds every 30 days.
find /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4 -maxdepth 1 -type d -ctime +30 -name "M20*" -ls -execdir rm -fr '{}' \;

source ${UTILITIES_HOME}/sdk/updateIndexFilesFunction.shsource >/dev/null
updateIndex > /dev/null



