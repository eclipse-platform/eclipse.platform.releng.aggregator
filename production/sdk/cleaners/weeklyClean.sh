#!/usr/bin/env bash
#*******************************************************************************
# Copyright (c) 2016 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     David Williams - initial API and implementation
#*******************************************************************************

# Utility to clean download machine
echo -e "\n\tWeekly clean of ${HOSTNAME} download server on $(date )\n"

# clean 4.x M builds every 30 days.
find /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4 -maxdepth 1 -type d -ctime +30 -name "M20*" -ls -execdir rm -fr '{}' \;

source /shared/eclipse/sdk/updateIndexFilesFunction.shsource >/dev/null
updateIndex > /dev/null



