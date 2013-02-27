#!/usr/bin/env bash

SCRIPTDIR=$( dirname $0 )
echo "SCRIPTDIR: ${SCRIPTDIR}"
source ${SCRIPTDIR}/synchUpdateUtils.shshource

# compute build machine drop directory
function dropDir()
{

    
    eclipseStream=$1
    if [ -z "${eclipseStream}" ]
    then
        echo "must provide EclipseStream as first argumnet, for this function $0"
        return 1;
    fi


    buildId=$2
    if [[ -z "${buildId}" ]]
    then
        printf "\n\n\t%s\n\n" "ERROR: Must provide buildId as second argumnet, for this function $(basename $0)"
        return 1;
    fi


    BUILD_TECH=$3
    if [[ -z "${BUILD_TECH}" ]]
    then
        printf "\n\n\t%s\n\n" "ERROR: Must provide BUILD_TECH as third argumnet, for this function $(basename $0)"
        return 1;
    fi

    
    pathToDL=$( dlpath "$eclipseStream" "$buildId" "$BUILD_TECH" )

    if [[ "$pathToDL" == 1 ]]
    then
        printf "\n\n\t%s\n\n" "ERROR: dlpath could not be computed."
        return 1
    fi

    eclipseStreamMajor=${eclipseStream:0:1}
    buildType=${buildId:0:1}

    if [[ "${BUILD_TECH}" == "CBI" ]]
    then 
        buildRoot=/shared/eclipse/builds/${eclipseStreamMajor}${buildType}
    else
        buildRoot=/shared/eclipse/eclipse${eclipseStreamMajor}${buildType}
    fi

    siteDir=${buildRoot}/siteDir

    dropDir=${siteDir}/${pathToDL}/${buildId}
    if [[ -d "${dropDir}" ]]
    then
        echo "${dropDir}"
    else
        echo "ERROR: dropDir is not a directory? dropDir: ${dropDir}"
        return 1
    fi
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

    BUILD_TECH=$3
    if [[ -z "${BUILD_TECH}" ]]
    then
        printf "\n\n\t%s\n\n" "ERROR: Must provide BUILD_TECH as third argumnet, for this function $(basename $0)"
        return 1;
    fi



    eclipseStreamMajor=${eclipseStream:0:1}
    buildType=${buildId:0:1}

    #TODO: eventual switch so CBI is "normal" one and PDE is marked one
    if [[ "${BUILD_TECH}" == "CBI" ]]
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

# compute main (left part) of download site
function dlFrompath()
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

    BUILD_TECH=$3
    if [[ -z "${BUILD_TECH}" ]]
    then
        printf "\n\n\t%s\n\n" "ERROR: Must provide BUILD_TECH as third argumnet, for this function $(basename $0)"
        return 1;
    fi



    eclipseStreamMajor=${eclipseStream:0:1}
    buildType=${buildId:0:1}

    pathToDL=eclipse/downloads/drops
    if [[ $eclipseStreamMajor > 3 ]]
    then
        pathToDL=$pathToDL$eclipseStreamMajor
    fi


    echo $pathFromDL
}



# update index on build machine with test results
function updatePages()
{
    eclipseStream=$1
    buildId=$2
    BUILD_TECH=$3
    EBUILDER_HASH=$4
    if [[ -z "${EBUILDER_HASH}" ]]
    then
        printf "\n\n\t%s\n\n" "ERROR: Must provide builder (or aggregator) hash as fourth argumnet, for this function $(basename $0)"
        return 1;
    fi


    eclipseStreamMajor=${eclipseStream:0:1}
    buildType=${buildId:0:1}


    echo "eclipseStreamMajor: $eclipseStreamMajor"
    echo "buildType: $buildType"
    echo "eclipseStream: $eclipseStream"
    echo "buildId: $buildId"
    echo "BUILD_TECH: $BUILD_TECH"
    echo "EBUILDER_HASH: $EBUILDER_HASH"
    if [[ "${BUILD_TECH}" == "CBI" ]]
    then 
        buildRoot=/shared/eclipse/builds/${eclipseStreamMajor}${buildType}
        eclipsebuilder=eclipse.platform.releng.aggregator
        dlPath=$( dlpath $eclipseStream $buildId $BUILD_TECH )
        echo "DEBUG dlPath: $dlPath"
        buildDropDir=${buildRoot}/siteDir/$dlPath/${buildId}
        echo "DEBGUG buildDropDir: $buildDropDir"
        ebuilderDropDir="${buildDropDir}/${eclipsebuilder}/production/testScripts"
        echo "DEBUG: ebuilderDropDir: ${ebuilderDropDir}"
    else
        buildRoot=/shared/eclipse/eclipse${eclipseStreamMajor}${buildType}
        buildDir=${buildRoot}/build
        supportDir=${buildDir}/supportDir
        eclipsebuilder=org.eclipse.releng.eclipsebuilder
        builderDir=${supportDir}/$eclipsebuilder
        ebuilderDropDir="${builderDir}/testScripts"
    fi

    ${ebuilderDropDir}/updateTestResultsPages.sh  $eclipseStream $buildId $BUILD_TECH
     rccode=$?
     if [[ $rccode != 0 ]]
     then
         printf "\n\n\t%s\n\n" "ERROR occurred while updating test pages."
         exit 1
     fi
    
}

# this is the single script to call that "does it all" update DL page 
# with test results, and updates index.php summaries.
# it requires four arguments
#    eclipseStream (e.g. 4.2 or 3.8)
#    buildId       (e.g. N20120415-2015)
#    BUILD_TECH    (CBI or PDE)
#    EBUILDER_HASH (SHA1 HASH or branch of eclipse builder to used

if [[ $# != 4 ]]
then
    # usage:
    scriptname=$(basename $0)
    printf "\n\t%s\n" "This script, $scriptname requires four arguments, in order: "
    printf "\t\t%s\t%s\n" "eclipseStream" "(e.g. 4.2.2 or 3.8.2) "
    printf "\t\t%s\t%s\n" "buildId" "(e.g. N20120415-2015) "
    printf "\t\t%s\t%s\n" "BUILD_TECH" "(e.g. PDE or CBI) "
    printf "\t\t%s\t%s\n" "EBUILDER_HASH" "(SHA1 HASH for eclipe builder used) "
    printf "\t%s\n" "for example,"
    printf "\t%s\n\n" "./$scriptname 4.2 N N20120415-2015 CBI master"
    exit 1
fi

echo "Staring $0"

eclipseStream=$1
if [[ -z "${eclipseStream}" ]]
then
    printf "\n\n\t%s\n\n" "ERROR: Must provide eclipseStream as first argumnet, for this function $(basename $0)"
    exit 1
fi
echo "eclipseStream: $eclipseStream"

buildId=$2
if [[ -z "${buildId}" ]]
then
    printf "\n\n\t%s\n\n" "ERROR: Must provide buildId as second argumnet, for this function $(basename $0)"
    exit 1
fi
echo "buildId: $buildId"

BUILD_TECH=$3
if [[ -z "${BUILD_TECH}" ]]
  then
    printf "\n\n\t%s\n\n" "ERROR: Must provide BUILD_TECH as third argumnet, for this function $(basename $0)"
    exit 1
fi
echo "BUILD_TECH: $BUILD_TECH"

EBUILDER_HASH=$4
if [[ -z "${EBUILDER_HASH}" ]]
then
    printf "\n\n\t%s\n\n" "ERROR: Must provide builder (or aggregator) hash as fourth argumnet, for this function $(basename $0)"
    exit 1;
fi
echo "EBUILDER_HASH: $EBUILDER_HASH"

eclipseStreamMajor=${eclipseStream:0:1}
buildType=${buildId:0:1}
echo "buildType: $buildType"

# call generic fetcher (it checks if it already exists)
toDir=$( dropDir "$eclipseStream" "$buildId" "$BUILD_TECH" )
if [[ "${toDir}" == "1" ]]
then
    echo "dropDir did not complete normally, returned '1'."
    exit 1
fi

if [[ ! -d "${toDir}" ]]
then 
    echo "ERROR: expected toDir (drop directory) did not exist"
    echo "       drop directory: ${toDir}"
    exit 1
fi
SCRIPTDIR=$( dirname $0 )
${SCRIPTDIR}/getEBuilder.sh "${BUILD_TECH}" "${EBUILDER_HASH}" "${toDir}"

updatePages $eclipseStream $buildId $BUILD_TECH "${EBUILDER_HASH}"
rccode=$?
if [ $rccode -ne 0 ]
then
    echo "ERROR occurred during promotion to download serve: rccode: $rccode."
    exit $rccode
fi

syncDropLocation "$eclipseStream" "$buildId" "$BUILD_TECH" 
rccode=$?
if [ $rccode -ne 0 ]
then
    echo "ERROR occurred during promotion to download serve: rccode: $rccode."
    exit $rccode
fi

exit 0


