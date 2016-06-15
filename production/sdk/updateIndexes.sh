#!/usr/bin/env bash

# Utility to update both 4.x index pages, for all build technologies

# To allow this cron job to work from hudson, or traditional crontab
if [[ -z "${WORKSPACE}" ]]
then
  export UTILITIES_HOME=/shared/eclipse
  export WORKSPACE=/shared/eclipse
else
  export UTILITIES_HOME=/${WORKSPACE}/utilities/production
fi

# for testing, may not be in "production" location

if [[ -f /shared/eclipse/sdk/updateIndexFilesFunction.shsource ]]
then
  source /shared/eclipse/sdk/updateIndexFilesFunction.shsource
else
  source updateIndexFilesFunction.shsource
fi
updateIndex


