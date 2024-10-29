#!/usr/bin/env bash

echo "command line as passed into $(basename ${0}): ${*}"
echo "command line (quoted) as passed into $(basename ${0}): ${@}"

# This file, localBuildProperties.shsource, should never exist or be needed for production machine,
# but allows an easy way for a "local user" to provide this file
# somewhere on the search path ($HOME/bin is common),
# and it will be included here, thus can provide "override values"
# to those defined by defaults for production machine.,
# such as for jvm
source localBuildProperties.shsource 2>/dev/null

if [[ -z "${propertyFile}" ]]; then
   echo "expect 'propertyFile' as environment variable for production runs"
   exit 1
fi
if [[ -z "${jvm}" ]]; then
   echo "expect 'jvm' as environment variable for production runs"
   exit 1
fi
if [[ -z "${testedPlatform}" ]]; then
   echo "expect 'testedPlatform' as environment variable for production runs"
   exit 1
fi

echo "PWD: $PWD"
# production machine is x86_64, but some local setups may be 32 bit and will need to provide
# this value in localBuildProperties.shsource. (
eclipseArch=${eclipseArch:-x86_64}

echo "=== properties in testAll.sh"
echo "    DOWNLOAD_HOST: ${DOWNLOAD_HOST}"
echo "    jvm in testAll: ${jvm}"
echo "    extdir in testAll (if any): ${extdir}"
echo "    propertyFile in testAll: ${propertyFile}"
echo "    buildId in testAll: ${buildId}"
echo "    testedPlatform: ${testedPlatform}"
echo "    ANT_OPTS: ${ANT_OPTS}"
echo "    contents of propertyFile:"
cat ${propertyFile}

#execute command to run tests
/bin/chmod 755 runtests.sh
/bin/mkdir -p results/consolelogs

if [[ -n "${extdir}" ]]
then
  ./runtests.sh -os linux -ws gtk -arch $eclipseArch -extdirprop "${extdir}" -vm "${jvm}"  -properties ${propertyFile} "${@}"  > results/consolelogs/${testedPlatform}_consolelog.txt
else
  ./runtests.sh -os linux -ws gtk -arch $eclipseArch -vm "${jvm}" -properties ${propertyFile} "${@}" > results/consolelogs/${testedPlatform}_consolelog.txt
fi

