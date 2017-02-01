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

# Utility to promote the build and trigger the unit tests on Hudson.

function usage ()
{
  printf "\n\n\t%s\n" "promote-build.sh env_file"
}

source "$1" 2>/dev/null
# To allow this cron job to work from hudson, or traditional crontab
if [[ -z "${WORKSPACE}" ]]
then
  export UTILITIES_HOME=/shared/eclipse
else
  export UTILITIES_HOME=${WORKSPACE}/utilities/production
fi

#TODO: Should we make use of "UTILITIES_HOME" here?
if [[ -z ${SCRIPT_PATH} ]]
then
  SCRIPT_PATH=${PWD}
fi
echo -e "\n\t[DEBUG] SCRIPT_PATH in promote-build.sh: $SCRIPT_PATH"
source $SCRIPT_PATH/build-functions.shsource

if [[ -z ${STREAM} || -z ${BUILD_ID} ]]
then
  echo "ERROR: This script requires STREAM and BUILD_ID"
  exit 1
fi

if [[ "${testbuildonly}" == "true" ]]
then
  echo "Did not promote build since testbuildonly is true."
  exit 0
fi

# if EBUILDER_HASH is not defined, assume master, so order of following parameters are maintained.
if [[ -z "${EBUILDER_HASH}" ]]
then
  EBUILDER_HASH=master
fi

# Here is command for promotion:

${UTILITIES_HOME}/sdk/promotion/syncDropLocation.sh $STREAM $BUILD_ID $EBUILDER_HASH $BUILD_ENV_FILE

# we do not promote equinox, if BUILD_FAILED since no need.
# we also do not promote if Patch build or Y-build or experimental (since, to date, those are not "for" equinox). 
if [[ -z "${BUILD_FAILED}" &&  $BUILD_TYPE =~ [IMN] ]]
then

  equinoxPostingDirectory="$BUILD_ROOT/siteDir/equinox/drops"
  eqFromDir=${equinoxPostingDirectory}/${BUILD_ID}
  eqToDir="/home/data/httpd/download.eclipse.org/equinox/drops/"

  # Note: for proper mirroring at Eclipse, we probably do not want or need to
  # maintain "times" on build machine, but let them take times at time of copying.
  # If it turns out to be important to maintain times (such as ran more than once,
  # to pick up a "more" output, such as test results, then add -t to rsync
  # Similarly, if download server is set up right, it will end up with the
  # correct permissions, but if not, we may need to set some permissions first,
  # then use -p on rsync

  # Here is promotion command
  rsync --times --omit-dir-times --recursive "${eqFromDir}" "${eqToDir}"

else
  echo "Did not promote equinox since BUILD_FAILED"
fi

echo "normal exit from promote phase of $(basename $0)"

exit 0

