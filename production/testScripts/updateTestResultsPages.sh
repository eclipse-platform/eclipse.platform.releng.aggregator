#!/usr/bin/env bash

# Utility to invoke eclipse antrunner to update test index pages and
# re-sync dl site.

# this localBuildProperties.shsource file is to ease local builds to override some variables.
# It should not be used for production builds.
source localBuildProperties.shsource 2>/dev/null


function show_time () {
num=$1
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
echo "$day d  $hour h  $min m  $sec s"
}


if (( $# < 3 ))
then
  # usage:
  scriptname=$(basename $0)
  printf "\n\t%s\n" "This script, $scriptname requires three arguments, in order: "
  printf "\t\t%s\t%s\n" "eclipseStream" "(e.g. 4.2.0 or 3.8.0) "
  printf "\t\t%s\t%s\n" "buildId" "(e.g. N20120415-2015) "
  printf "\t\t%s\t%s\n" "jobName" "(e.g. ep4I-unit-lin64) "
  printf "\t\t%s\t%s\n" "jobNumber" "(e.g. 59) "
  printf "\t%s\n" "for example,"
  printf "\t%s\n\n" "./$scriptname 4.2.0 N20120415-2015 ep4I-unit-lin64 59"
  exit 1
fi

eclipseStream=$1
if [ -z "${eclipseStream}" ]
then
  echo "must provide eclipseStream as first argument, for this function $0"
  exit 1
fi


buildId=$2
if [ -z "${buildId}" ]
then
  echo "must provide buildId as second argument, for this function $0"
  exit 1
fi

JOB_NAME=$3
if [ -z "${JOB_NAME}" ]
then
  echo "must provide JOB_NAME as third argument, for this function $0"
  exit 1
fi

JOB_NUMBER=$4
if [ -z "${JOB_NUMBER}" ]
then
  # technically, not required, though later may want to force and error, since
  # probably indicates something is wrong.
  echo -e "\n\tERROR: JOB_NUMBER as fourth argument, not provided to this function $0. Exiting."
  exit 1
fi


eclipseStreamMajor=${eclipseStream:0:1}
buildType=${buildId:0:1}

pathToDL=eclipse/downloads/drops
if (( $eclipseStreamMajor > 3 ))
then
  pathToDL=eclipse/downloads/drops$eclipseStreamMajor
fi

buildRoot=${BUILD_HOME}/${eclipseStreamMajor}${buildType}

siteDir=${buildRoot}/siteDir

fromDir=${siteDir}/${pathToDL}/${buildId}
if [ ! -d "${fromDir}" ]
then
  echo "ERROR: fromDir is not a directory? fromDir: ${fromDir}"
  exit 1
fi


JAVA_7_HOME=${JAVA_7_HOME:-/shared/common/jdk1.7.0-latest}
JAVA_8_HOME=${JAVA_8_HOME:-/shared/common/jdk1.8.0_x64-latest}

export JAVA_HOME=${JAVA_HOME:-${JAVA_8_HOME}}

devJRE=$JAVA_HOME/jre/bin/java

if [[ ! -n ${devJRE} && -x ${devJRE} ]]
then
  echo "ERROR: could not find (or execute) JRE where expected: ${devJRE}"
  exit 1
else
  # display version, just to be able to log it.
  echo "JRE Location and Version: ${devJRE}"
  echo $( $devJRE -version )
fi

# We use a separate basebuilder for each "drop", to make sure it is specific for that drop,
# and they won't interfere with each other.
basebuilderDir=${fromDir}/org.eclipse.releng.basebuilder
aggregatorDir=${fromDir}/eclipse.platform.releng.aggregator
EBuilderDir=${aggregatorDir}/eclipse.platform.releng.tychoeclipsebuilder

if [[ ! -d "${basebuilderDir}" ]]
then
  # WORKSPACE, here, is the "Hudson-type" of workspace, not eclipse.
  ant -f $EBuilderDir/eclipse/getBaseBuilderAndTools.xml -DWORKSPACE=${fromDir}
fi

# remember, the Eclipse install must match the VM used (e.g. both 64 bit, both 32 bit, etc).
ECLIPSE_EXE="${basebuilderDir}/eclipse"
# somehow, seems like this is often not executable ... I guess launcher jar usually used.
chmod -c +x $ECLIPSE_EXE

export SWT_GTK3=2

if [ ! -n ${ECLIPSE_EXE} -a -x ${ECLIPSE_EXE} ]
then
  echo "ERROR: ECLIPSE_EXE is not defined or not executable: ${ECLIPSE_EXE}"
  exit 1
fi

# Normal unit tests
unittestJobPattern="^.*-unit-.*$"

#if [[ $JOB_NAME =~ ${unittestJobPattern} ]]
#then

BUILDFILE=${aggregatorDir}/production/testScripts/genTestIndexes.xml

BUILDFILESTR="-f ${BUILDFILE}"
echo
echo " BUILDFILESTR: $BUILDFILESTR"

# provide blank, to get default
BUILDTARGET=" "

devworkspace="${fromDir}/workspace-updateTestResults"
devArgs="-Xmx512m -Dhudson=true -DbuildHome=${BUILD_HOME} -DeclipseStream=${eclipseStream} -DeclipseStreamMajor=${eclipseStreamMajor} -DbuildId=${buildId} -Djob=$JOB_NAME"

echo
echo " = = Properties in updateTestResultsPages.sh: generate section  = = "
echo "   dev script:   $0"
echo "   BUILD_HOME:   ${BUILD_HOME}"
echo "   devworkspace: $devworkspace"
echo "   devArgs:      $devArgs"
echo "   devJRE:       $devJRE"
echo "   BUILDFILESTR: $BUILDFILESTR"
echo "   job:          $JOB_NAME"
echo

if [ -n ${ECLIPSE_EXE} -a -x ${ECLIPSE_EXE} ]
then

  ${ECLIPSE_EXE}  --launcher.suppressErrors  -nosplash -consolelog -data $devworkspace -application org.eclipse.ant.core.antRunner $BUILDFILESTR  $BUILDTARGET -vm $devJRE -vmargs $devArgs
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo "ERROR: eclipse returned non-zero return code, exiting with RC: $RC."
    exit $RC
  fi
else
  echo "ERROR: ECLIPSE_EXE is not defined to executable eclipse."
  RC=1
  exit $RC
fi

#fi

perfJobPattern="^.*-perf-.*$"
perfBaselineJobPattern="^.*-perf-.*-baseline.*$"

if [[ $JOB_NAME =~ $perfJobPattern && ! $JOB_NAME =~ $perfBaselineJobPattern ]]
then
  # We run the "performance analysis" tools only on "current" build, for jobs that contain -perf- (and do not contain -baseline)

  devworkspace="${fromDir}/workspace-installDerbyCore"
  devArgs="-Xmx512m"


  echo "Collected a performance run result. Doing performance analysis for $JOB_NAME"
  echo
  echo " = = Properties in updateTestResultsPages.sh: update derby section  = = "
  echo "   dev script:   $0"
  echo "   buildRoot:    $buildRoot"
  echo "   BUILD_HOME:   ${BUILD_HOME}"
  echo "   pathToDL:     $pathToDL"
  echo "   siteDir:      $siteDir"
  echo "   fromDir:      $fromDir"
  echo "   devworkspace: $devworkspace"
  echo "   devArgs:      $devArgs"
  echo "   devJRE:       $devJRE"
  echo "   JOB_NAME:     $JOB_NAME"
  echo "   JOB_NUMBER:   $JOB_NUMBER"
  echo
  echo " = = First, installing derby"
  # make sure derby.core is installed in basebuilder
  perfrepoLocation=http://build.eclipse.org/eclipse/buildtools/
  #perfrepoLocation=file:///shared/eclipse/buildtools
  derby=org.apache.derby.core.feature.feature.group
  echo "   perfrepoLocation:   $perfrepoLocation"
  echo "   derby:              $derby"

  ${ECLIPSE_EXE}  --launcher.suppressErrors  -nosplash -consolelog -debug -data $devworkspace -application org.eclipse.equinox.p2.director -repository ${perfrepoLocation} -installIUs ${derby} -vm $devJRE -vmargs $devArgs
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo "ERROR: eclipse returned non-zero return code while installing derby, exiting with RC: $RC."
    exit $RC
  fi

  echo " = = Now run performance.ui app = ="
  devworkspace="${fromDir}/workspace-updatePerfResults"
  eclipse_perf_dbloc_value=${eclipse_perf_dbloc_value:-//172.25.25.57:1527}
  vmargs="-Xmx1G -Declipse.perf.dbloc=${eclipse_perf_dbloc_value}"
  postingDirectory=$fromDir
  perfOutput=$postingDirectory/performance
  # assuming for now the intent is that 'data' is meant to accumulate in common location
  # common location doesn't seem to work, with our multi-run method. So, will
  # make unique, for now. (Might work ok, if we just had "short set" and "long set" locations?
  ROOT_PERF_DATA=/shared/eclipse/perfdataDir

  # experiment with deleting previous .dat files, and regenerate all that are needed.
  # (I believe they are a "performance improvement" for the test analysis itself, but
  # I suspect they make a lot of assumptions that are no longer true.
  rm -fr ${ROOT_PERF_DATA}
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo "Could not remove ${ROOT_PERF_DATA}. Return code was $RC. Exiting."
    exit $RC
  fi
  # re-create
  mkdir -p  ${ROOT_PERF_DATA}
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo "Could not mkdir -p ${ROOT_PERF_DATA}. Return code was $RC. Exiting."
    exit $RC
  fi
  # Will try "just one". Might get some better results, now that bug 481272 has been fixed.
  dataDir=${ROOT_PERF_DATA}
  #  dataDir=${ROOT_PERF_DATA}/${buildId}_${JOB_NAME}_${JOB_NUMBER}
  # make anew
  #  mkdir -p "${dataDir}"
  #  RC=$?
  #  if [[ $RC != 0 ]]
  #  then
  #      echo "Could not mkdir -p $dataDir. Return code was $RC. Exiting."
  #      exit $RC
  #  fi

  # The performance UI function needs a DISPLAY to function, so we'll give it one via xvfb
  # if running on Hudson, be sure "use xvnc" is checked.
  # If not running on Hudson, can use this xvfb-run utility,
  # distributed with xvfb as a "build time only" requirement.
  echo -e "\n\t[DEBUG] RUNNING_ON_HUDSON: ${RUNNING_ON_HUDSON}\n"
  if [[ "${RUNNING_ON_HUDSON}" == "false" ]]
  then
    XVFB_RUN="xvfb-run"
    if [[ ! -w "${TMP_DIR}" ]]
    then
      echo -e "\n\tTMP_DIR not defined, so will create at ${buildRoot}/tmp"
      TMP_DIR="${buildRoot}/tmp"
      mkdir -p "${TMP_DIR}"
    fi
    XVFB_RUN_ARGS="--error-file ${TMP_DIR}/xvfbErrorFile.txt"
    # --server-args -screen 0 1024x768x24"
  else
    echo -e "\n\t[INFO] Running on Hudson, be sure Xvnc is checked."
  fi
  #
  if [[ ${buildType} =~ [INM] ]]
  then
    if [[ "${buildType}" == "M" ]]
    then
      current_prefix=" -current.prefix M "
    else
      current_prefix=" -current.prefix I,N "
    fi
  else
    echo -e "\n\tPROGRAM ERROR: build type did not equal expected value (M or N or I). Exiting."
    exit 1
  fi

  #PERF_OUTFILE="${fromDir}/performance/perfAnalysis_${buildId}_${JOB_NAME}_${JOB_NUMBER}.txt"
  echo "Beginning performance analysis.
  #Results in ${PERF_OUTFILE}."
  mkdir -p "${fromDir}/performance"
  RAW_DATE_START=$( date -u +%s )

  # TODO: avoid this hard coding of baseline value
  baselineCode="R-4.6.3-201703010400"
  # to get time stamp, first remove initial IMN:
  baselineForBuildSuffix=${buildId/[IMN]/}
  #Then remove final '-' in build id
  baselineForBuildSuffix=${baselineForBuildSuffix/-/}
  # then form "final" baseline code with true base line with -timestamp
  baselineForCurrent="${baselineCode}-${baselineForBuildSuffix}"

  #echo -e "\n\tDEBUG RAW Date Start: ${RAW_DATE_START} \n"
  echo -e "\n\tStart Time: $( date  +%Y%m%d%H%M%S -d @${RAW_DATE_START} ) \n" #>${PERF_OUTFILE}
  echo " = = Properties in updateTestResultsPages.sh: performance.ui.resultGenerator section  = = " ##>>${PERF_OUTFILE}
  echo "   dev script:   $0" #>>${PERF_OUTFILE}
  echo
  echo "   buildId:      $buildId"
  echo "   baselineCode: ${baselineCode}"
  echo "   baselineForCurrent: ${baselineForCurrent}"
  echo
  echo "   buildRoot:    $buildRoot" #>>${PERF_OUTFILE}
  echo "   BUILD_HOME:   ${BUILD_HOME}" #>>${PERF_OUTFILE}
  echo "   pathToDL:     $pathToDL" #>>${PERF_OUTFILE}
  echo "   siteDir:      $siteDir" #>>${PERF_OUTFILE}
  echo "   fromDir:      $fromDir" #>>${PERF_OUTFILE}
  echo "   devworkspace: $devworkspace" #>>${PERF_OUTFILE}
  echo "   vmArgs:       $vmArgs" #>>${PERF_OUTFILE}
  echo "   devJRE:       $devJRE" #>>${PERF_OUTFILE}
  echo "   BUILDFILESTR: $BUILDFILESTR" #>> ${PERF_OUTFILE}
  echo "   JOB_NAME:     $JOB_NAME" #>> ${PERF_OUTFILE}
  echo "   JOB_NUMBER:   $JOB_NUMBER" #>> ${PERF_OUTFILE}
  echo "   XVFB_RUN_ARGS $XVFB_RUN_ARGS" #>> ${PERF_OUTFILE}
  echo "   current_prefix ${current_prefix}" #>> ${PERF_OUTFILE}
  echo #>> ${PERF_OUTFILE}

  ${XVFB_RUN} ${XVFB_RUN_ARGS} ${ECLIPSE_EXE} --launcher.suppressErrors  -nosplash -consolelog -debug -data $devworkspace -application org.eclipse.test.performance.ui.resultGenerator -baseline ${baselineForCurrent} -current ${buildId} -jvm 8.0 -config linux.gtk.x86_64 -config.properties "linux.gtk.x86_64,SUSE Linux Enterprise Server 11 (x86_64)" -output $perfOutput -dataDir ${dataDir} ${current_prefix} -print -vm ${devJRE}  -vmargs ${vmargs}  #>> ${PERF_OUTFILE}
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo "ERROR: eclipse returned non-zero return code from invoking performance.ui app, exiting with RC: $RC."
    exit $RC
  fi
  RAW_DATE_END=$( date -u +%s )

  #echo -e "\n\tRAW Date End: ${RAW_DATE_END} \n"
  echo -e "\n\tEnd Time: $( date  +%Y%m%d%H%M%S -d @${RAW_DATE_END} )"  #>> ${PERF_OUTFILE}

  ELAPSED_SECONDS=$(( ${RAW_DATE_END} - ${RAW_DATE_START} ))
  # echo -e "\n\tDEBUG: RAW_DATE_END: ${RAW_DATE_END} RAW_DATE_START ${RAW_DATE_START} ELAPSED_SECONDS ${ELAPSED_SECONDS}" #>> ${PERF_OUTFILE}
  ELAPSED_TIME=$( show_time ${ELAPSED_SECONDS} )
  echo -e "\n\tElapsed Time: ${ELAPSED_TIME}" #>> ${PERF_OUTFILE}
fi

