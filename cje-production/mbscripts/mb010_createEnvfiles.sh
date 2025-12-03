#!/bin/bash

#*******************************************************************************
# Copyright (c) 2019, 2024 IBM Corporation and others.
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
#     Hannes Wellmann - Unify declaration of build-properties for I-, Y- and P-builds
#*******************************************************************************
set -e

if [ $# -ne 1 ]; then
  echo USAGE: $0 env_file
  exit 1
fi

source $CJE_ROOT/scripts/common-functions.shsource

shEnvFile=$(basename $1)
buildDir=$(dirname $1)
baseEnvFile=$(echo $shEnvFile |cut -d. -f1)
phpEnvFile=$(echo $baseEnvFile.php)
propEnvFile=$(echo $baseEnvFile.properties)

BUILD_ENV_FILE=$CJE_ROOT/${shEnvFile}
BUILD_ENV_FILE_PHP=$CJE_ROOT/${phpEnvFile}
BUILD_ENV_FILE_PROP=$CJE_ROOT/${propEnvFile}

fn-addToPropFiles ()
{
  echo "export $1=$2" >> $BUILD_ENV_FILE
  echo "\$$1 = $2;" >> $BUILD_ENV_FILE_PHP
  echo "$1 = $2" >> $BUILD_ENV_FILE_PROP
}

echo "#!/bin/bash" > $BUILD_ENV_FILE
echo "<?php " > $BUILD_ENV_FILE_PHP

# We set RAWDATE first thing here to make the "start of build" timestamp more accurate.
# Note that a roundup is added to compensate the occasional delay.
RAWDATE=$(TZ="America/New_York" date +%s)
REMAINDER=$((RAWDATE % 600))
RAWDATE_TRUNC=$((RAWDATE - REMAINDER))
export RAWDATE
fn-addToPropFiles TIMESTAMP "\"$(TZ="America/New_York" date +%Y%m%d-%H%M --date='@'$RAWDATE_TRUNC)\""
fn-addToPropFiles BUILD_PRETTY_DATE "\"$(TZ="America/New_York" date --date='@'$RAWDATE_TRUNC)\""

while read propLine
do
  if [[ ${propLine:0:1} == "#" ]]; then 
    continue
  else
    key=$(echo $propLine|cut -d= -f1)
    value=$(echo $propLine|cut -d= -f2-)
    if [[ -z $key ]]; then 
      continue
    fi
    fn-addToPropFiles $key "$value"
  fi
done < ${CJE_ROOT}/buildproperties.txt

source $BUILD_ENV_FILE
# add BUILD_ENV_FILE* variables to prop files before using fn-write-property in common-functions.shsource
fn-addToPropFiles BUILD_ENV_FILE "\"$BUILD_ENV_FILE\""
fn-addToPropFiles BUILD_ENV_FILE_PHP "\"$BUILD_ENV_FILE_PHP\""
fn-addToPropFiles BUILD_ENV_FILE_PROP "\"$BUILD_ENV_FILE_PROP\""
# variables in buildproperties.txt are now defined, add other commonly used variables to prop files
fn-addToPropFiles BUILD_TYPE "\"${BUILD_TYPE}\""
fn-addToPropFiles BUILD_TYPE_NAME "\"${BUILD_TYPE_NAME}\""
fn-addToPropFiles BUILD_ID "\"$BUILD_TYPE$TIMESTAMP\""
fn-addToPropFiles BUILD_DIR_SEG "\"$BUILD_TYPE$TIMESTAMP\""
fn-addToPropFiles EQ_BUILD_DIR_SEG "\"$BUILD_TYPE$TIMESTAMP\""
fn-addToPropFiles ECLIPSE_BUILDER_DIR "\"$AGG_DIR/eclipse.platform.releng.tychoeclipsebuilder\""
fn-addToPropFiles TEST_CONFIGURATIONS_EXPECTED "\"${TEST_CONFIGURATIONS_EXPECTED}\""
