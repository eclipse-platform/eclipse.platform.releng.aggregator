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

  eclipseStreamMajor=${eclipseStream:0:1}
  buildType=${buildId:0:1}

  pathToDL=eclipse/downloads/drops
  if (( $eclipseStreamMajor > 3 ))
  then
    pathToDL=$pathToDL$eclipseStreamMajor
  fi

  echo $pathToDL
}


# update index on build machine with test results
function updatePages()
{
  eclipseStream=$1
  buildId=$2
  EBUILDER_HASH=$3
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
  echo "EBUILDER_HASH: $EBUILDER_HASH"

  # compute dirctiory on build machine
  dropFromBuildDir=$( dropFromBuildDir "$eclipseStream" "$buildId" )
  echo "dropFromBuildDir: $dropFromBuildDir"


    eclipsebuilder=eclipse.platform.releng.aggregator
    ebuilderDropDir="${dropFromBuildDir}/${eclipsebuilder}/production/testScripts"
  echo "DEBUG: ebuilderDropDir: ${ebuilderDropDir}"

  ${ebuilderDropDir}/updateTestResultsPages.sh  $eclipseStream $buildId
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
#    EBUILDER_HASH (SHA1 HASH or branch of eclipse builder to used

if [[ "${#}" != "3" ]]
then
  # usage:
  scriptname=$(basename $0)
  printf "\n\t%s\n" "This script, $scriptname requires three arguments, in order: "
  printf "\t\t%s\t%s\n" "eclipseStream" "(e.g. 4.2.2 or 3.8.2) "
  printf "\t\t%s\t%s\n" "buildId" "(e.g. N20120415-2015) "
  printf "\t\t%s\t%s\n" "EBUILDER_HASH" "(SHA1 HASH for eclipe builder used) "
  printf "\t%s\n" "for example,"
  printf "\t%s\n\n" "./$scriptname 4.2 N20120415-2015 master"
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

EBUILDER_HASH=$3
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
# compute directory on build machine
dropFromBuildDir=$( dropFromBuildDir "$eclipseStream" "$buildId" )
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
${SCRIPTDIR}/getEBuilder.sh "${EBUILDER_HASH}" "${dropFromBuildDir}"

updatePages $eclipseStream $buildId "${EBUILDER_HASH}"
rccode=$?
if [ $rccode -ne 0 ]
then
  echo "ERROR occurred during promotion to download serve: rccode: $rccode."
  exit $rccode
fi

syncDropLocation "$eclipseStream" "$buildId" "${EBUILDER_HASH}"
rccode=$?
if [ $rccode -ne 0 ]
then
  echo "ERROR occurred during promotion to download serve: rccode: $rccode."
  exit $rccode
fi

exit 0


