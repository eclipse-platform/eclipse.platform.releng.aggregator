#!/usr/bin/env bash

SCRIPTDIR=$( dirname $0 )
echo "SCRIPTDIR: ${SCRIPTDIR}"
source ${SCRIPTDIR}/syncUpdateUtils.shsource

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

  # CBI is "normal" one. Other clauses can be added in future for special cases.
  if [[ "${BUILD_KIND}" == "CBI" ]]
  then
    dropsuffix=""
  else
    dropsuffix="pdebased"
  fi

  pathToDL=eclipse/downloads/drops
  if (( $eclipseStreamMajor > 3 ))
  then
    pathToDL=$pathToDL$eclipseStreamMajor
  fi

  pathToDL=$pathToDL$dropsuffix

  echo $pathToDL
}


# update index on build machine with test results
function updatePages()
{
  eclipseStream=$1
  buildId=$2
  BUILD_KIND=$3
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
  echo "BUILD_KIND: $BUILD_KIND"
  echo "EBUILDER_HASH: $EBUILDER_HASH"

  # compute dirctiory on build machine
  dropFromBuildDir=$( dropFromBuildDir "$eclipseStream" "$buildId" "$BUILD_KIND" )
  echo "dropFromBuildDir: $dropFromBuildDir"

  if [[ "${BUILD_KIND}" == "CBI" ]]
  then
    eclipsebuilder=eclipse.platform.releng.aggregator
    ebuilderDropDir="${dropFromBuildDir}/${eclipsebuilder}/production/testScripts"
  elif [[ "${BUILD_KIND}" == "PDE" ]]
  then
    eclipsebuilder=org.eclipse.releng.eclipsebuilder
    ebuilderDropDir="${builderDir}/testScripts"
  else
    echo "ERROR: Unexpected value of BUILD_KIND, $BUILD_KIND"
  fi
  echo "DEBUG: ebuilderDropDir: ${ebuilderDropDir}"

  ${ebuilderDropDir}/updateTestResultsPages.sh  $eclipseStream $buildId $BUILD_KIND
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
#    BUILD_KIND    (CBI or special case)
#    EBUILDER_HASH (SHA1 HASH or branch of eclipse builder to used

if [[ $# != 4 ]]
then
  # usage:
  scriptname=$(basename $0)
  printf "\n\t%s\n" "This script, $scriptname requires four arguments, in order: "
  printf "\t\t%s\t%s\n" "eclipseStream" "(e.g. 4.2.2 or 3.8.2) "
  printf "\t\t%s\t%s\n" "buildId" "(e.g. N20120415-2015) "
  printf "\t\t%s\t%s\n" "BUILD_KIND" "(e.g. CBI or special case) "
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

BUILD_KIND=$3
if [[ -z "${BUILD_KIND}" ]]
then
  printf "\n\n\t%s\n\n" "ERROR: Must provide BUILD_KIND as third argumnet, for this function $(basename $0)"
  exit 1
fi
echo "BUILD_KIND: $BUILD_KIND"

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

# = = = = = = = = =
# compute dirctiory on build machine
dropFromBuildDir=$( dropFromBuildDir "$eclipseStream" "$buildId" "$BUILD_KIND" )
echo "dropFromBuildDir: $dropFromBuildDir"

if [[ "${dropFromBuildDir}" == "1" ]]
then
  echo "dropDir did not complete normally, returned '1'."
  exit 1
fi

if [[ ! -d "${dropFromBuildDir}" ]]
then
  echo "ERROR: expected toDir (drop directory) did not exist"
  echo "       drop directory: ${dropFromBuildDir}"
  exit 1
fi
SCRIPTDIR=$( dirname $0 )
${SCRIPTDIR}/getEBuilder.sh "${BUILD_KIND}" "${EBUILDER_HASH}" "${dropFromBuildDir}"

updatePages $eclipseStream $buildId $BUILD_KIND "${EBUILDER_HASH}"
rccode=$?
if [ $rccode -ne 0 ]
then
  echo "ERROR occurred during promotion to download serve: rccode: $rccode."
  exit $rccode
fi

syncDropLocation "$eclipseStream" "$buildId" "$BUILD_KIND" "${EBUILDER_HASH}"
rccode=$?
if [ $rccode -ne 0 ]
then
  echo "ERROR occurred during promotion to download serve: rccode: $rccode."
  exit $rccode
fi

exit 0


