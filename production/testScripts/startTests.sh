#!/usr/bin/env bash

# this buildeclipse.shsource file is to ease local builds to override some variables. 
# It should not be used for production builds.
source buildeclipse.shsource 2>/dev/null
export BUILD_HOME=${BUILD_HOME:-/shared/eclipse/builds}

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

# compute main (left part) of download site
function dlpath()
{
    eclipseStream=$1
    if [[ -z "${eclipseStream}" ]]
    then
        printf "\n\n\t%s\n\n" "ERROR: Must provide eclipseStream as first argumnet, for this function $(basename $0)"
        return 1;
    fi
    
    
    buildId=$2
    if [[ -z "${buildId}" ]]
    then
         printf "\n\n\t%s\n\n" "ERROR: Must provide buildId as second argumnet, for this function $(basename $0)"
         return 1;
    fi
    
    BUILD_KIND=$3
    if [[ -z "${BUILD_KIND}" ]]
    then
         printf "\n\n\t%s\n\n" "ERROR: Must provide BUILD_KIND as third argumnet, for this function $(basename $0)"
        return 1;
    fi
    

    
    eclipseStreamMajor=${eclipseStream:0:1}
    buildType=${buildId:0:1}

    #CBI is "normal" one and can add clauses in future for special cases
    if [[ "${BUILD_KIND}" == 'CBI' ]]
    then 
        dropsuffix=""
    else
        dropsuffix="pdebased"
    fi

    pathToDL=eclipse/downloads/drops
    if [[ $eclipseStreamMajor > 3 ]]
    then
        pathToDL=$pathToDL$eclipseStreamMajor
    fi

    pathToDL=$pathToDL$dropsuffix
    
    echo $pathToDL
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



echo "values in ${0}"
echo "eclipseStream: $eclipseStream"
echo "eclipseStreamMajor: $eclipseStreamMajor"
echo "eclipseStreamMinor: $eclipseStreamMinor"
echo "eclipseStreamService: $eclipseStreamService"
echo "buildType: $buildType"
echo "buildId: $buildId"
echo "BUILD_KIND: $BUILD_KIND"
echo "EBUILDER_HASH: $EBUILDER_HASH"
echo "BUILD_HOME: ${BUILD_HOME}"


    if [[ "${BUILD_KIND}" == 'CBI' ]]
    then 
       buildRoot=${BUILD_HOME}/${eclipseStreamMajor}${buildType}
       eclipsebuilder=eclipse.platform.releng.aggregator/production/testScripts
       dlPath=$( dlpath $eclipseStream $buildId $BUILD_KIND )
       echo "DEBUG dlPath: $dlPath"
       buildDropDir=${buildRoot}/siteDir/$dlPath/${buildId}
       echo "DEBGUG buildDropDir: $buildDropDir"
       builderDropDir=${buildDropDir}/${eclipsebuilder}
       echo "DEBUG: builderDropDir: ${builderDropDir}"
    else
        buildRoot=/shared/eclipse/eclipse${eclipseStreamMajor}${buildType}
        # we don't really use this file for PDE build tests. 
        # if we did, we'd need to fix this up.
        #buildDir=${buildRoot}/build
        #supportDir=${buildDir}/supportDir
        #eclipsebuilder=org.eclipse.releng.eclipsebuilder
        #builderDir=${supportDir}/$eclipsebuilder
        #$buildRoot=/shared/eclipse/eclipse${eclipseStreamMajor}${buildType}
        #$buildDir=${buildRoot}/build
        #$supportDir=${buildDir}/supportDir
        #$eclipsebuilder=org.eclipse.releng.eclipsebuilder
        #$builderDir=${supportDir}/$eclipsebuilder

        # should buildDirectory be set at "main" one from actual build?
        #$buildDirectory=${supportDir}/src

        # note, to be consistent, I changed json xml file so it adds buildId to postingDirectory
        #$siteDir=${buildRoot}/siteDir
        #$postingDirectory=${siteDir}/eclipse/downloads/drops
        #$if [[ "${eclipseStreamMajor}" > 3 ]]
        #$then
            # $postingDirectory=${siteDir}/eclipse/downloads/drops${eclipseStreamMajor}
        #$fi
  fi

echo "DEBUG: invoking test scripts on Hudson"

HUDSON_TOKEN=windows2012tests ant \
    -DbuildId=${buildId} \
    -DeclipseStream=${eclipseStream} \
    -DBUILD_KIND=${BUILD_KIND} \
    -DEBUILDER_HASH=${EBUILDER_HASH} \
        -f ${builderDropDir}/invokeTestsJSON.xml

