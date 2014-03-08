#!/usr/bin/env bash

# Small utility to start unit tests (or re-run them) after a build
# and after upload to downloads server is complete.

# need to be running Java 6 and Ant 1.8 for <sript> to work in invokeTestsJSON
# and, default on current build system is Ant 1.7 ... so ...
export ANT_HOME=/shared/common/apache-ant-1.9.2

function usage ()
{
    printf "\t\t%s\n" "usage: "
    printf "\t\t%s\n" "$( basename $0 ) eclipseStream buildId"
    printf "\t\t\t%s\n" "where "
    printf "\t\t\t%s\n" "eclipseStream == 4.3.0, 3.8.2, etc. "
    printf "\t\t\t%s\n" "buildId == M20120705-1200, IM20121005-0800, etc. "
    printf "\t\t\t\t%s\n" "or, provide those parameters in buildParams.shshource on search path"
}

# This file, buildParams.shsource, normally does not exist on build system,
# but can be provided if running "by hand" as an easy way to provide the
# parameters required. For example, the contents might be
# eclipseStream=4.2.1
# buildId=M20120705-1200
#
source buildParams.shsource 2>/dev/null

# can provide eclipseStream and buildId as first to arts to this script
# which is how invoke from "promote script"
eclipseStream=${eclipseStream:-${1}}
buildId=${buildId:-${2}}
BUILD_KIND=${BUILD_KIND:-${3}}
EBUILDER_HASH=${EBUILDER_HASH:-${4}}

if [[ -z ${eclipseStream} || -z ${buildId} ]]
then
    printf "\n\t%s\n" "ERROR: missing required parameters."
    usage
    exit 1
fi

if [[ -z "${BUILD_KIND}" ]]
then
    BUILD_KIND=CBI
fi

if [[ -z "${EBUILDER_HASH}" ]]
then
    EBUILDER_HASH=master
fi

# contrary to intuition (and previous behavior, bash 3.1) do NOT use quotes around right side of expression.
if [[ "${eclipseStream}" =~ ^([[:digit:]]+)\.([[:digit:]]+)\.([[:digit:]]+)$ ]]
then
    eclipseStreamMajor=${BASH_REMATCH[1]}
    eclipseStreamMinor=${BASH_REMATCH[2]}
    eclipseStreamService=${BASH_REMATCH[3]}
else
    printf "\n\t%s\n" "ERROR: eclipseStream, $eclipseStream, must contain major, minor, and service versions."
    usage
    exit 1
fi

if [[ "${buildId}" =~ ([MNIXYP]+)([[:digit:]]*)\-([[:digit:]]*) ]]
then
    # old, simpler way, if we don't do regex and input checkinging
    #buildType=${buildId:0:1}
    buildType=${BASH_REMATCH[1]}
else
    printf "\n\t%s\n" "ERROR: buildId, $buildId, did not match expected pattern."
    usage
    exit 1
fi

source buildeclipse.shsource 2>/dev/null

echo "values in ${0}"
echo "eclipseStream: $eclipseStream"
echo "eclipseStreamMajor: $eclipseStreamMajor"
echo "eclipseStreamMinor: $eclipseStreamMinor"
echo "eclipseStreamService: $eclipseStreamService"
echo "buildType: $buildType"
echo "buildId: $buildId"
echo "BUILD_KIND: $BUILD_KIND"
echo "EBUILDER_HASH: $EBUILDER_HASH"


buildRoot=/shared/eclipse/eclipse${eclipseStreamMajor}${buildType}
buildDir=${buildRoot}/build
supportDir=${buildDir}/supportDir
eclipsebuilder=org.eclipse.releng.eclipsebuilder
builderDir=${supportDir}/$eclipsebuilder

# should buildDirectory be set at "main" one from actual build?
buildDirectory=${supportDir}/src

# note, to be consistent, I changed json xml file so it adds buildId to postingDirectory
siteDir=${buildRoot}/siteDir
postingDirectory=${siteDir}/eclipse/downloads/drops
if [[ "${eclipseStreamMajor}" > 3 ]]
then
    postingDirectory=${siteDir}/eclipse/downloads/drops${eclipseStreamMajor}
fi

HUDSON_TOKEN=windows2012tests ant \
    -DbuildDirectory=${buildDirectory} \
    -DpostingDirectory=${postingDirectory} \
    -DbuildId=${buildId} \
    -DeclipseStream=${eclipseStream} \
    -DBUILD_KIND=${BUILD_KIND} \
    -DEBUILDER_HASH=${EBUILDER_HASH} \
    -f ${builderDir}/invokeTestsJSON.xml

