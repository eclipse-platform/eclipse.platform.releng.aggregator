#!/bin/bash
#*******************************************************************************
# Copyright (c) 2017 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     Sravan Kumar Lakkimsetti - initial API and implementation
#*******************************************************************************

# Utility to generate jdeps -jdkinternals report
# Requires 2 parameters
#    - repo location
#    - output filename (full path)

# Uses JAVA_HOME environment variable if set. else defaults to /shared/common/jdk1.8.0_x64-latest

JAVA_8_HOME=/shared/common/jdk1.8.0_x64-latest
export JAVA_HOME=${JAVA_HOME:-${JAVA_8_HOME}}

if [ $# -ne 2 ]
then
  echo "USAGE: $0 <Repository Location> <report file name with full path>"
  exit 1
fi

repo=$1
outputFile=$2
outputDir=$(dirname ${outputFile})

if [ ! -d "${repo}" ]
then
  echo "${repo} does not exist. Exiting ...."
  exit 1
fi

if [ ! -d "${JAVA_HOME}" ]
then
  echo "${JAVA_HOME} does not exist. Please set JAVA_HOME and run again ...."
  exit 1
fi

mkdir -p ${outputDir}

echo -e "\n\n\n#jdeps -jdkinternals output:" > ${outputFile}
find ${repo} -name "*.jar" -print > ${outputFile}.tmp
for i in $(cat ${outputFile}.tmp)
do
  ${JAVA_HOME}/bin/jdeps -jdkinternals ${i} > ${outputFile}.interim
  fileSize=$(stat -c %s ${outputFile}.interim)
  if [ $fileSize -gt 0 ]
  then
    echo -e "\n###### Java internal API usage report for $(basename ${i}) \n">> ${outputFile}
    cat ${outputFile}.interim >> ${outputFile}
    echo -e "\n\n" >> ${outputFile}
    rm ${outputFile}.interim
  fi
done
echo "# " >> ${outputFile}

#Cleanup
rm ${outputFile}.tmp
