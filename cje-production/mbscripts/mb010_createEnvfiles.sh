#!/bin/bash -x

#*******************************************************************************
# Copyright (c) 2018 IBM Corporation and others.
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

source "../common/common-functions.shsource"

shEnvFile=$(basename $1)
buildDir=$(dirname $1)
baseEnvFile=$(echo $shEnvFile |cut -d. -f1)
phpEnvFile=$(echo $baseEnvFile.php)
propEnvFile=$(echo $baseEnvFile.properties)

BUILD_ENV_FILE=${buildDir}/${shEnvFile}
BUILD_ENV_FILE_PHP=${buildDir}/${phpEnvFile}
BUILD_ENV_FILE_PROP=${buildDir}/${propEnvFile}

fn-addToPropFiles ()
{
	echo "export $1=$2" >> $BUILD_ENV_FILE
	echo "\$$1 = $2;" >> $BUILD_ENV_FILE_PHP
	echo "$1 = $2" >> $BUILD_ENV_FILE_PROP
}
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
		fn-addToPropFiles $key $value
	fi
done < ../buildproperties.txt
