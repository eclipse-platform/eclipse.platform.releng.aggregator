#!/usr/bin/env bash

SCRIPTDIR=$( dirname $0 )
echo "SCRIPTDIR: ${SCRIPTDIR}"
source ${SCRIPTDIR}/syncUpdateUtils.shsource

function show_hours_minutes () {
    num=$1
    # we are given "seconds.milliseconds" so we truncate milliseconds
    num=${num%.*}
    min=0
    hour=0
    day=0
    if((num>59));then
        ((sec=num%60))
        ((num=num/60))
        if((num>59));then
            ((min=num%60))
            ((num=num/60))
            if((num>23));then
                ((hour=num%24))
                ((day=num/24))
            else
                ((hour=num))
            fi
        else
            ((min=num))
        fi
    else
        ((sec=num))
    fi
    echo "$hour h  $min m"
}


# compute main (left part) of download site
function dlpath()
{
  eclipseStream=$1
  if [[ -z "${eclipseStream}" ]]
  then
    printf "\n\n\t%s\n\n" "ERROR: Must provide eclipseStream as first argument, for this function $(basename $0)"
    return 1;
  fi


  buildId=$2
  if [[ -z "${buildId}" ]]
  then
    printf "\n\n\t%s\n\n" "ERROR: Must provide buildId as second argument, for this function $(basename $0)"
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
    printf "\n\n\t%s\n\n" "ERROR: Must provide builder (or aggregator) hash as third argument, for this function $(basename $0)"
    return 1;
  fi

  JOB_NAME=$4
  if [[ -z "${JOB_NAME}" ]]
  then
    printf "\n\n\t%s\n\n" "ERROR: Must provide JOB_NAME as fourth argument, for this function $(basename $0)"
    return 1;
  fi
  JOB_NUMBER=$5
  if [[ -z "${JOB_NUMBER}" ]]
  then
    # technically, may not be needed, just effects some directory names, but will require, for now. 
    printf "\n\n\t%s\n\n" "ERROR: JOB_NUMBER as fifth argument was not provided, for this function $(basename $0). Exiting."
    exit 1
  fi
  eclipseStreamMajor=${eclipseStream:0:1}
  buildType=${buildId:0:1}

  echo "-- properties in updateDropLocation.sh function updatePages --"
  echo "eclipseStreamMajor: $eclipseStreamMajor"
  echo "buildType: $buildType"
  echo "eclipseStream: $eclipseStream"
  echo "buildId: $buildId"
  echo "EBUILDER_HASH: $EBUILDER_HASH"
  echo "JOB_NAME: $JOB_NAME"
  echo "JOB_NUMBER: $JOB_NUMBER"

  # compute directory on build machine
  dropFromBuildDir=$( dropFromBuildDir "$eclipseStream" "$buildId" )
  echo "dropFromBuildDir: $dropFromBuildDir"


  eclipsebuilder=eclipse.platform.releng.aggregator
  ebuilderDropDir="${dropFromBuildDir}/${eclipsebuilder}/production/testScripts"
  echo "DEBUG: ebuilderDropDir: ${ebuilderDropDir}"

  ${ebuilderDropDir}/updateTestResultsPages.sh  $eclipseStream $buildId $JOB_NAME $JOB_NUMBER
  rccode=$?
  if [[ $rccode != 0 ]]
  then
    printf "\n\n\t%s\n\n" "ERROR occurred while updating test pages."
    return $rccode
  fi

}

function sendTestResultsMail ()
{

  SITE_HOST=${SITE_HOST:-download.eclipse.org}

  echo "     Starting sendTestResultsMail"
  eclipseStream=$1
  if [[ -z "${eclipseStream}" ]]
  then
    printf "\n\n\t%s\n\n" "ERROR: Must provide eclipseStream as first argument, for this function $(basename $0)"
    return 1;
  fi
  echo "     eclipseStream: ${eclipseStream}"

  buildId=$2
  if [[ -z "${buildId}" ]]
  then
    printf "\n\n\t%s\n\n" "ERROR: Must provide buildId as second argument, for this function $(basename $0)"
    return 1;
  fi
  echo "     buildId: ${buildId}"

  JOB_NAME=$3
  if [[ -z "${JOB_NAME}" ]]
  then
    printf "\n\n\t%s\n\n" "ERROR: Must provide JOB_NAME as third argument, for this function $(basename $0)"
    return 1;
  fi
  echo "     JOB_NAME: ${JOB_NAME}"

  JOB_NUMBER=$4
  if [[ -z "${JOB_NUMBER}" ]]
  then
    printf "\n\n\t%s\n\n" "ERROR: Must provide JOB_NUMBER as fourth argument, for this function $(basename $0)"
    return 1;
  fi
  echo "     JOB_NUMBER: ${JOB_NUMBER}"

  buildType=${buildId:0:1}
  echo "     buildType: ${buildType}"


  fsDocRoot="/home/data/httpd/download.eclipse.org"

  mainPath=$( dlToPath "$eclipseStream" "$buildId")
  echo "     mainPath: $mainPath"
  if [[ "$mainPath" == 1 ]]
  then
    printf "\n\n\t%s\n\n" "ERROR: mainPath could not be computed."
    return 1
  fi

  downloadURL="http://${SITE_HOST}/${mainPath}/${buildId}/"
  fsDownloadSitePath="${fsDocRoot}/${mainPath}/${buildId}"

  export BUILD_HOME=${BUILD_HOME:-/shared/eclipse/builds}
  buildRoot=${BUILD_HOME}/${eclipseStreamMajor}${buildType}
  testsSummary="eclipse/downloads/drops4/${buildId}/testresults/${JOB_NAME}-${JOB_NUMBER}.xml"
  eclipseSiteTestFile="${buildRoot}/siteDir/${testsSummary}"
  echo -e "\n\tDEBUG: eclipseSiteTestFile: ${eclipseSiteTestFile}"
  if [[ ! -e "${eclipseSiteTestFile}" ]] 
  then
    echo -e "\nProgramming error. The test summary file was not found where expected:"
    echo -e "\t${eclipseSiteTestFile}"
    return 1
  else
    # Had trouble reading this file in a while loop. Perhaps because it is one line, 
    # with no EOL character?
    read -r line <  "${eclipseSiteTestFile}"
    echo -e "\n\tDEBUG: Text read from test summary file: ${line}"
    pattern="^.*<duration>(.*)</duration><failCount>(.*)</failCount><passCount>(.*)</passCount>.*$"
    if [[ "${line}" =~ ${pattern} ]]
    then 
      testsDuration=${BASH_REMATCH[1]}
      testsFailed=${BASH_REMATCH[2]}
      testsPassed=${BASH_REMATCH[3]}
    else 
      echo -e "\n\tProgramming error. We should always match!?"
      echo -e "\n\tDEBUG: line: ${line}"
    fi

    EXTRA_SUBJECT_STRING="Failures: ${testsFailed}"

    # 4.3.0 Build: I20120411-2034
    SUBJECT="Test Results available from ${JOB_NAME} for ${buildId} $EXTRA_SUBJECT_STRING"

    # override in localBuildProperties.shsource if doing local tests
    TO=${TO:-"platform-releng-dev@eclipse.org"}

    # for initial testing, only to me -- change as desired after initial testing.
    if [[ "${buildType}" =~ [PYX] ]]
    then
      TO="david_williams@us.ibm.com"
      SUBJECT="Experimental: ${SUBJECT}"
    fi

    FROM=${FROM:-"e4Builder@eclipse.org"}

    # repeat subject in message
    message1="<p>${SUBJECT}</p>\n"
    link=$(linkURL ${downloadURL}testResults.php)
    message1="${message1}<p>&nbsp;&nbsp;&nbsp;Build logs and test results: <br />\n&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${link}</p>\n"
    message1="${message1}<p>&nbsp;&nbsp;&nbsp;Tests Passed: ${testsPassed} &nbsp;&nbsp;&nbsp; Total Number of Tests: $(( testsFailed + testsPassed )) &nbsp;&nbsp;&nbsp; Tests Elapsed Time: $(show_hours_minutes ${testsDuration})</p>\n"

    sendEclipseMail "${TO}" "${FROM}" "${SUBJECT}" "${message1}"

    echo "INFO: test results mail sent for ${eclipseStream} ${buildType}-build ${buildId}"

    return 0
  fi
}

# this is the single script to call that "does it all" update DL page
# with test results, and updates index.php summaries.
# it requires four arguments
#    eclipseStream (e.g. 4.2 or 3.8)
#    buildId       (e.g. N20120415-2015)
#    EBUILDER_HASH (SHA1 HASH or branch of eclipse builder to used

if [[ "${#}" -lt "4" ]]
then
  # usage:
  scriptname=$(basename $0)
  printf "\n\t%s\n" "PROGRAM ERROR: This script, $scriptname requires four arguments, in order: "
  printf "\t\t%s\t%s\n" "eclipseStream" "(e.g. 4.2.2 or 3.8.2) "
  printf "\t\t%s\t%s\n" "buildId" "(e.g. N20120415-2015) "
  printf "\t\t%s\t%s\n" "EBUILDER_HASH" "(SHA1 HASH for eclipse builder used) "
  printf "\t\t%s\t%s\n" "JOB_NAME" "job name from Hudson"
  printf "\t%s\n" "for example,"
  printf "\t%s\n\n" "./$scriptname 4.5.1 N20120415-2015 master ep4I-unit-lin64"
  exit 1
fi

echo "Starting $0"

eclipseStream=$1
if [[ -z "${eclipseStream}" ]]
then
  printf "\n\n\t%s\n\n" "ERROR: Must provide eclipseStream as first argument, for this function $(basename $0)"
  exit 1
fi
echo "eclipseStream: $eclipseStream"

buildId=$2
if [[ -z "${buildId}" ]]
then
  printf "\n\n\t%s\n\n" "ERROR: Must provide buildId as second argument, for this function $(basename $0)"
  exit 1
fi
echo "buildId: $buildId"

EBUILDER_HASH=$3
if [[ -z "${EBUILDER_HASH}" ]]
then
  printf "\n\n\t%s\n\n" "ERROR: Must provide builder (or aggregator) hash as third argument, for this function $(basename $0)"
  exit 1;
fi
echo "EBUILDER_HASH: $EBUILDER_HASH"

JOB_NAME=$4
if [[ -z "${JOB_NAME}" ]]
then
  printf "\n\n\t%s\n\n" "ERROR: Must provide job (JOB_NAME) as fourth argument, for this function $(basename $0)"
  exit 1;
fi
echo "JOB_NAME: $JOB_NAME"

JOB_NUMBER=$5
if [[ -z "${JOB_NUMBER}" ]]
then
  printf "\n\n\t%s\n\n" "ERROR: Must provide job number (JOB_NUMBER) as fifth argument, for this function $(basename $0)"
  exit 1;
fi
echo "JOB_NUMBER: $JOB_NUMBER"

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

updatePages $eclipseStream $buildId "${EBUILDER_HASH}" $JOB_NAME $JOB_NUMBER
rccode=$?
if [ $rccode -ne 0 ]
then
  echo "ERROR occurred during promotion to download server: rccode: $rccode."
  exit $rccode
fi

syncDropLocation "$eclipseStream" "$buildId" "${EBUILDER_HASH}"
rccode=$?
if [ $rccode -ne 0 ]
then
  echo "ERROR occurred during promotion to download server: rccode: $rccode."
  exit $rccode
fi



# do not send for performance tests, for now
if [[ ! "${JOB_NAME}" =~ .*-perf-.* ]]
then
  sendTestResultsMail "$eclipseStream" "$buildId" "${JOB_NAME}" "${JOB_NUMBER}"
fi

exit 0

