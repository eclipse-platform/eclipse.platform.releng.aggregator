#!/usr/bin/env bash

# Simple utility to run as cronjob to run Eclipse Platform builds
# Normally resides in $BUILD_HOME

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

# 0002 is often the default for shell users, but it is not when ran from
# a cron job, so we set it explicitly, so releng group has write access to anything
# we create.
oldumask=`umask`
umask 0002
echo "umask explicitly set to 0002, old value was $oldumask"

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
export BUILD_TYPE=N
export STREAM=4.3.0

eclipseStreamMajor=${STREAM:0:1}

# unique short name for stream and build type
BUILDSTREAMTYPEDIR=${eclipseStreamMajor}$BUILD_TYPE

export BUILD_ROOT=${BUILD_HOME}/${BUILDSTREAMTYPEDIR}

export PRODUCTION_SCRIPTS_DIR=production


$BUILD_HOME/bootstrap.sh $BRANCH $BUILD_TYPE $STREAM

#BOOTSTRAPENVFILE=$BUILD_ROOT/env${BUILDSTREAMTYPEDIR}.txt
#timestamp=$( date +%Y%m%d%H%M )
#echo "Environment at time of starting build at ${timestamp}." > $BOOTSTRAPENVFILE
#env >> $BOOTSTRAPENVFILE
#echo "= = = = = " >> $BOOTSTRAPENVFILE
#java -version  >> $BOOTSTRAPENVFILE 2>&1
#ant -version >> $BOOTSTRAPENVFILE
#mvn -version >> $BOOTSTRAPENVFILE
#echo "= = = = = " >> $BOOTSTRAPENVFILE

${BUILD_ROOT}/${PRODUCTION_SCRIPTS_DIR}/master-build.sh ${BUILD_ROOT}/${PRODUCTION_SCRIPTS_DIR}/build_eclipse_org.env

