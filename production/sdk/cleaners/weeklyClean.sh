#!/usr/bin/env bash

# Utility to clean download machine
echo -e "\n\tWeekly clean of ${HOSTNAME} download server on $(date )\n"

# clean 4.x M builds every 30 days.
find /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4 -maxdepth 1 -type d -ctime +30 -name "M*" -ls -exec rm -fr '{}' \;

source /shared/eclipse/sdk/updateIndexFilesFunction.shsource >/dev/null
updateIndex > /dev/null



