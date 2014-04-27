#!/usr/bin/env bash

# Utility to invoke eclipse antrunner to update test index pages and
# re-sync dl site.


if (( $# < 2 ))
then
    # usage:
    scriptname=$(basename $0)
    printf "\n\t%s\n" "This script, $scriptname requires three arguments, in order: "
    printf "\t\t%s\t%s\n" "eclipseStream" "(e.g. 4.2.0 or 3.8.0) "
    printf "\t\t%s\t%s\n" "buildId" "(e.g. N20120415-2015) "
    printf "\t\t%s\t%s\n" "BUILD_KIND" "(e.g. CBI or special case) "
    printf "\t%s\n" "for example,"
    printf "\t%s\n\n" "./$scriptname 4.2.0 N20120415-2015 CBI"
    exit 1
fi

eclipseStream=$1
if [ -z "${eclipseStream}" ]
then
    echo "must provide eclipseStream as first argumnet, for this function $0"
    exit 1
fi


buildId=$2
if [ -z "${buildId}" ]
then
    echo "must provide buildId as second argumnet, for this function $0"
    exit 1
fi

# For now only used by CBI
BUILD_KIND=$3
if [[ -z "${BUILD_KIND}" ]]
then
    BUILD_KIND=CBI
fi

eclipseStreamMajor=${eclipseStream:0:1}
buildType=${buildId:0:1}

pathToDL=eclipse/downloads/drops
if (( $eclipseStreamMajor > 3 ))
then
    pathToDL=eclipse/downloads/drops$eclipseStreamMajor
fi

if [[ "$BUILD_KIND" == "PDE" ]]
then
    pathToDL="${pathToDL}pdebased"
fi  

if [[ "$BUILD_KIND" == "CBI" ]]
then
    buildRoot=${BUILD_HOME}/${eclipseStreamMajor}${buildType}
elif [[ "$BUILD_KIND" == "PDE" ]]
then
    buildRoot=/shared/eclipse/eclipse${eclipseStreamMajor}${buildType}
else
    echo "ERROR: BUILD_KIND was not recognized."
    exit 1
fi

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

# We use a seperate basebuilder for each "drop", to make sure it is specific for that drop, 
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

BUILDFILE=${aggregatorDir}/production/testScripts/genTestIndexes.xml

BUILDFILESTR="-f ${BUILDFILE}"
echo
echo " BUILDFILESTR: $BUILDFILESTR"

# provide blank, to get default
BUILDTARGET=" "

devworkspace="${fromDir}/workspace-updateTestResults"
devArgs="-Xmx256m -Dhudson=true -DeclipseStream=${eclipseStream} -DeclipseStreamMajor=${eclipseStreamMajor} -DbuildId=${buildId} -DBUILD_KIND=${BUILD_KIND}"

echo
echo "   dev script:   $0"
echo "   devworkspace: $devworkspace"
echo "   devArgs:      $devArgs"
echo "   devJRE:       $devJRE"
echo "   BUILDFILESTR: $BUILDFILESTR"
echo

if [ -n ${ECLIPSE_EXE} -a -x ${ECLIPSE_EXE} ]
then

    ${ECLIPSE_EXE}  --launcher.suppressErrors  -nosplash -console -data $devworkspace -application org.eclipse.ant.core.antRunner $BUILDFILESTR  $BUILDTARGET -vm $devJRE -vmargs $devArgs
    RC=$?
else
    echo "ERROR: ECLIPSE_EXE is not defined to executable eclipse"
    RC=1
fi
exit $RC
