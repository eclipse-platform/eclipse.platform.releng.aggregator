#!/usr/bin/env bash

# Utility to invoke eclipse antrunner to update test index pages and
# re-sync dl site.

# this localBuildProperties.shsource file is to ease local builds to override some variables.
# It should not be used for production builds.
source localBuildProperties.shsource 2>/dev/null

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

export JAVA_HOME=${JAVA_HOME:-${JAVA_7_HOME}}

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
  devArgs="-Xmx256m -Dhudson=true -DbuildHome=${BUILD_HOME} -DeclipseStream=${eclipseStream} -DeclipseStreamMajor=${eclipseStreamMajor} -DbuildId=${buildId} -Djob=$JOB_NAME"

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
  devArgs="-Xmx256m"
  BUILDFILESTR="-f ${BUILDFILE}"

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
  echo "   BUILDFILESTR: $BUILDFILESTR"
  echo "   JOB_NAME:     $JOB_NAME"
  echo "   JOB_NUMBER:   $JOB_NUMBER"
  echo
  echo " = = First, installing derby"
  # make sure derby.core is installed in basebuilder
  perfrepoLocation=http://build.eclipse.org/eclipse/buildtools
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
  vmargs="-Xmx256m -Declipse.perf.dbloc=${eclipse_perf_dbloc_value}"
  postingDirectory=$fromDir
  perfOutput=$postingDirectory/performance
  # assuming for now the intent is that 'data' is meant to accumulate in common location
  # common location doesn't seem to work, with our multi-run method. So, will 
  # make unique, for now. (Might work ok, if we just had "short set" and "long set" locations? 
  ROOT_PERF_DATA=/shared/eclipse/perfdataDir
  mkdir -p  ${ROOT_PERF_DATA}
  RC=$?
  if [[ $RC != 0 ]] 
  then 
      echo "Could not mkdir -p ${ROOT_PERF_DATA}. Return code was $RC. Exiting."
      exit $RC
  fi
  dataDir=${ROOT_PERF_DATA}/${buildId}_${JOB_NAME}_${JOB_NUMBER}
  # make anew
  mkdir -p "${dataDir}"
  RC=$?
  if [[ $RC != 0 ]] 
  then 
      echo "Could not mkdir -p $dataDir. Return code was $RC. Exiting."
      exit $RC
  fi
  # The performance UI function needs a DISPLAY to function, so we'll give it one via xvfb
  XVFB_RUN="xvfb-run"
  XVFB_RUN_ARGS="--error-file /shared/eclipse/sdk/testjobdata/xvfb-log.txt"
  # --server-args -screen 0 1024x768x24"
  # 
  if [[ "M" == ${buildType}} ]] 
  then
     current_prefix=" -current.prefix M "
  elif [[ ${buildType}} =~ [NI] ]] 
     current_prefix=" -current.prefix N,I "
  else 
     echo -e "\n\tPROGRAM ERROR: build type did not equal expected value (M or N or I). Exiting.
     exit 1
  fi
  
  
  echo " = = Properties in updateTestResultsPages.sh: performance.ui.resultGenerator section  = = "
  echo "   dev script:   $0"
  echo "   buildRoot:    $buildRoot"
  echo "   BUILD_HOME:   ${BUILD_HOME}"
  echo "   pathToDL:     $pathToDL"
  echo "   siteDir:      $siteDir"
  echo "   fromDir:      $fromDir"
  echo "   devworkspace: $devworkspace"
  echo "   devArgs:      $devArgs"
  echo "   devJRE:       $devJRE"
  echo "   BUILDFILESTR: $BUILDFILESTR"
  echo "   JOB_NAME:     $JOB_NAME"
  echo "   JOB_NUMBER:   $JOB_NUMBER"
  echo "   XVFB_RUN_ARGS $XVFB_RUN_ARGS"
  echo "   current_prefix ${current_prefix}"
  echo
  
  ${XVFB_RUN} ${XVFB_RUN_ARGS} ${ECLIPSE_EXE} --launcher.suppressErrors  -nosplash -consolelog -debug -data $devworkspace -application org.eclipse.test.performance.ui.resultGenerator -baseline R-4.4-201406061215 -current ${buildId} -jvm 8.0 -config linux.gtk.x86_64 -config.properties "linux.gtk.x86_64,SUSE Linux Enterprise Server 11 (x86_64)" -output $perfOutput -dataDir ${dataDir} ${current_prefix} -print -vm ${devJRE}  -vmargs ${vmargs}
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo "ERROR: eclipse returned non-zero return code while using xvfb to invoke performance.ui app, exiting with RC: $RC."
    exit $RC
  fi
fi

