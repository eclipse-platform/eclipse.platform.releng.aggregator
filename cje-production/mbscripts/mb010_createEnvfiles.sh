#!/bin/bash -x

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

echo "#!/bin/bash" >> $BUILD_ENV_FILE

# We set RAWDATE first thing here to make the "start of build" timestamp more accurate.
# Note that a roundup is added to compensate the occasional delay.
RAWDATE=$(date +%s)
REMAINDER=$((RAWDATE % 600))
RAWDATE_TRUNC=$((RAWDATE - REMAINDER))
export RAWDATE
fn-addToPropFiles TIMESTAMP "\"$(date +%Y%m%d-%H%M --date='@'$RAWDATE_TRUNC)\""

while read propLine
do
	if [[ ${propLine:0:1} == "#" ]]
	then 
		continue
	else
		key=$(echo $propLine|cut -d= -f1)
		value=$(echo $propLine|cut -d= -f2-)
		if [[ -z $key ]]
		then 
			continue
		fi
		fn-addToPropFiles $key "$value"
	fi
done < ../buildproperties.txt

source $BUILD_ENV_FILE
fn-addToPropFiles BUILD_ID "\"$BUILD_TYPE$TIMESTAMP\""