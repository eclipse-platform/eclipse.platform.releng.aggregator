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
