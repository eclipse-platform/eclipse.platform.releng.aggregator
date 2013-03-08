#!/usr/bin/env bash

# Simple utility to run as cronjob to run Eclipse Platform builds
# Normally resides in $BUILD_HOME

buildId=$1
if [[ -z "${buildId}" ]]
then
    echo "This script requires previous (local) build Id as input."
    echo "    For example, $( basename $0 ) I20130306-0033"
    exit 1
fi

#export testbuildonly=true

SCRIPT_NAME=$0
MB_LOG_DIR=/shared/eclipse/builds
LOG_BASE_NAME=${SCRIPT_NAME##*/} 
LOG_OUT_NAME=${MB_LOG_DIR}/${LOG_BASE_NAME%.*}.out.log
LOG_ERR_NAME=${MB_LOG_DIR}/${LOG_BASE_NAME%.*}.err.log

echo "Starting $SCRIPT_NAME at $( date +%Y%m%d-%H%M ) " 1>$LOG_OUT_NAME 2>$LOG_ERR_NAME

# Start with minimal path for consistency across machines
# plus, cron jobs do not inherit an environment
# care is needed not have anything in ${HOME}/bin that would effect the build 
# unintentionally, but is required to make use of "source buildeclipse.shsource" on 
# local machines.  
# Likely only a "release engineer" would be interested, such as to override "SIGNING" (setting it 
# to false) for a test I-build on a remote machine. 
export PATH=/usr/local/bin:/usr/bin:/bin:${HOME}/bin
# unset common variables (some defined for e4Build) which we don't want (or, set ourselves)
unset JAVA_HOME
unset JAVA_ROOT
unset JAVA_JRE
unset CLASSPATH
unset JAVA_BINDIR
unset JRE_HOME

# 0002 is often the default for shell users, but it is not when ran from
# a cron job, so we set it explicitly, so releng group has write access to anything
# we create.
oldumask=`umask`
umask 0002
echo "umask explicitly set to 0002, old value was $oldumask" 1>>$LOG_OUT_NAME 2>>$LOG_ERR_NAME

# this buildeclipse.shsource file is to ease local builds to override some variables. 
# It should not be used for production builds.
source buildeclipse.shsource 2>/dev/null

export BUILD_HOME=${BUILD_HOME:-/shared/eclipse/builds}
# we should not need the following here in boot strap, for now, but might in future
#export JAVA_HOME=${JAVA_HOME:-/shared/common/jdk1.7.0_11}
#export ANT_HOME=${ANT_HOME:-/shared/common/apache-ant-1.8.4}
#export ANT_OPTS=${ANT_OPTS:-"-Dbuild.sysclasspath=ignore -Dincludeantruntime=false"}
#export MAVEN_PATH=${MAVEN_PATH:-/shared/common/apache-maven-3.0.4/bin}

# no override for minimal $PATH
#export PATH=$JAVA_HOME/bin:$MAVEN_PATH:$ANT_HOME/bin:$PATH

export BRANCH=master
export BUILD_TYPE=I
export STREAM=4.3.0

eclipseStreamMajor=${STREAM:0:1}

# unique short name for stream and build type
BUILDSTREAMTYPEDIR=${eclipseStreamMajor}$BUILD_TYPE

export BUILD_ROOT=${BUILD_HOME}/${BUILDSTREAMTYPEDIR}

buildParametersInput=${BUILD_ROOT}/siteDir/eclipse/downloads/drops4/${buildId}/buildproperties.shsource
if [[ ! -f "${buildParametersInput}" ]]
then
    echo "ERROR: The build input parameters file did not exist? "
    echo "    buildParametersInput: ${buildParametersInput}"
    exit 1
fi

export PRODUCTION_SCRIPTS_DIR=production


$BUILD_HOME/bootstrap.sh $BRANCH $BUILD_TYPE $STREAM 1>>$LOG_OUT_NAME 2>>$LOG_ERR_NAME

#BOOTSTRAPENVFILE=$BUILD_ROOT/env${BUILDSTREAMTYPEDIR}.txt
#timestamp=$( date +%Y%m%d%H%M )
#echo "Environment at time of starting build at ${timestamp}." > $BOOTSTRAPENVFILE
#env >> $BOOTSTRAPENVFILE
#echo "= = = = = " >> $BOOTSTRAPENVFILE
#java -version  >> $BOOTSTRAPENVFILE 2>&1
#ant -version >> $BOOTSTRAPENVFILE
#mvn -version >> $BOOTSTRAPENVFILE
#echo "= = = = = " >> $BOOTSTRAPENVFILE

${BUILD_ROOT}/${PRODUCTION_SCRIPTS_DIR}/re-master-build.sh ${buildParametersInput} 1>>$LOG_OUT_NAME 2>>$LOG_ERR_NAME &

rc=$?
if [[ $rc != 0 ]]
then 
    echo "BUILD FAILED. See run-maven-build-ouptut.txt." >&2
fi
