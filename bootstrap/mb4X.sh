#!/usr/bin/env bash

# Simple utility to run as cronjob to run Eclipse Platform builds
# Normally resides in $BUILD_HOME

# Start with minimal path for consistency across machines
# plus, cron jobs do not inherit an environment
# care is needed not have anything in ${HOME}/bin that would effect the build
# unintentionally, but is required to make use of "source localBuildProperties.shsource" on
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

function usage() {
printf "\n\tSimple script start a build of a certain stream." >&2
printf "\n\tUsage: %s [[-h] | [-t]] " $(basename $0) >&2
printf "\n\t\t%s\n" "where h==help, t==test build " >&2
}
# all optional
# normally, when ran from crobjob, none should be specified
while getopts 'ht' OPTION
do
  case $OPTION in
    h)    usage
      exit
      ;;
    t)    export testbuildonly=true
      ;;
#        ?)    usage
#        exit 2
#        ;;
  esac
done

# this localBuildProperties.shsource file is to ease local builds to override some variables.
# It should not be used for production builds.
source localBuildProperties.shsource 2>/dev/null
export BUILD_HOME=${BUILD_HOME:-/shared/eclipse/builds}

SCRIPT_NAME=$0
LOG_BASE_NAME=${SCRIPT_NAME##*/}
LOG_OUT_NAME=${BUILD_HOME}/${LOG_BASE_NAME%.*}.out.log
LOG_ERR_NAME=${BUILD_HOME}/${LOG_BASE_NAME%.*}.err.log

echo "Starting $SCRIPT_NAME at $( date +%Y%m%d-%H%M ) " 1>$LOG_OUT_NAME 2>$LOG_ERR_NAME



export BRANCH=david_williams/macapp
export BUILD_TYPE=X
export STREAM=4.5.0

eclipseStreamMajor=${STREAM:0:1}

# unique short name for stream and build type
BUILDSTREAMTYPEDIR=${eclipseStreamMajor}$BUILD_TYPE

export BUILD_ROOT=${BUILD_HOME}/${BUILDSTREAMTYPEDIR}

export PRODUCTION_SCRIPTS_DIR=production

source $BUILD_HOME/bootstrap.shsource

# run rest in "back ground"
${BUILD_ROOT}/${PRODUCTION_SCRIPTS_DIR}/master-build.sh "${BUILD_ROOT}/${PRODUCTION_SCRIPTS_DIR}/build_eclipse_org.shsource" 1>>$LOG_OUT_NAME 2>>$LOG_ERR_NAME &
