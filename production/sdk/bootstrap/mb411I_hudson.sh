#!/usr/bin/env bash

# This job is just like its cronjob counter part, except it 
# turns off verbose debugging (else Hudson logs would be 300 MB)
# and does not pipe output to separate files, but lets it all go 
# to Hudson't "console". 
# Normally resides in $BUILD_HOME

function usage()
{
  printf "\n\tSimple script start a build of a certain stream." >&2
  printf "\n\tUsage: %s [[-h] | [-t]] " $(basename $0) >&2
  printf "\n\t\t%s\n" "where h==help, t==test build " >&2
}

# Start with minimal path for consistency across machines
# plus, cron jobs do not inherit an environment
# care is needed not have anything in ${HOME}/bin that would effect the build
# unintentionally, but is required to make use of "source localBuildProperties.shsource" on
# local machines.
# Likely only a "release engineer" would be interested, such as to override "SIGNING" (setting it
# to false) for a test I-build on a remote machine.
export PATH=/usr/local/bin:/usr/bin:/bin:${HOME}/bin
# unset common variables (some defined for genie.releng) which we don't want (or, set ourselves)
unset JAVA_HOME
unset JAVA_ROOT
unset JAVA_JRE
unset CLASSPATH
unset JAVA_BINDIR
unset JRE_HOME

# 0002 is often the default for shell users, but it is not when ran from
# a cron job, so we set it explicitly, so releng group has write access to anything
# we create.
NEWUMASK="0002"
oldumask=$(umask)
umask $NEWUMASK

echo "ulimit (file handles): $( ulimit -n ) "
ulimit -n 4096
echo "ulimit (file handles): $( ulimit -n ) "

echo "locale charmap: $(locale charmap)"
echo "LC_ALL: $LC_ALL"
echo "LANG: $LANG"
echo "LANGUAGE: $LANGUAGE"
export LC_ALL=en_US.UTF-8
export LANG=en_US.UTF-8
export LANGUAGE=en_US.UTF-8
echo "LC_ALL: $LC_ALL"
echo "LANG: $LANG"
echo "LANGUAGE: $LANGUAGE"
echo "locale charmap: $(locale charmap)"

# all optional
# normally, when ran from crobjob, none should be specified
while getopts 'hti' OPTION
do
  case $OPTION in
    h)    usage
      exit
      ;;
    t)    export testbuildonly=true
      ;;
    i)    export invisibleBuild=true
      ;;
  esac
done

# this localBuildProperties.shsource file is to ease local builds to override some variables.
# It should not be used for production builds.
source localBuildProperties.shsource 2>/dev/null

# BUILD_HOME defines the "top" of the build area (for all types of builds)
export BUILD_HOME=${BUILD_HOME:-/shared/eclipse/builds}

SCRIPT_NAME=$0

echo "Starting $SCRIPT_NAME at $( date +%Y%m%d-%H%M ) "

echo "umask explicitly set to $NEWUMASK, old value was $oldumask"

export BRANCH=master
export BUILD_TYPE=I
export STREAM=4.11.0

eclipseStreamMajor=${STREAM:0:1}

# unique short name for stream and build type
BUILDSTREAMTYPEDIR=${eclipseStreamMajor}$BUILD_TYPE

export BUILD_ROOT=${BUILD_HOME}/${BUILDSTREAMTYPEDIR}

# These values for proxies come from the configuration files of the Releng HIPP instance. 
# They are normally defined in "ANT_OPTS" and similar environment variables, but 
# the JavaDoc program requires them is this special -Jflag form. 
export JAVA_DOC_PROXIES=${JAVA_DOC_PROXIES:-"-J-Dhttps.proxyHost=proxy.eclipse.org -J-Dhttps.proxyPort=9898 -J-Dhttps.nonProxyHosts=\"172.30.206.*\""}

# These definitions are primarily for Curl. (Wget and other programs use different env variables or parameters
export NO_PROXY=${NO_PROXY:-eclipse.org,build.eclipse.org,download.eclipse.org,archive.eclipse.org,dev.eclipes.org,git.eclipse.org}
export ALL_PROXY=${ALL_PROXY:-proxy.eclipse.org:9898}

# default (later) is set to 'true'. 
# set to false here for less output.
# setting to false until  bug 495750 is fixed, else too much output.
export MVN_DEBUG=false


export PRODUCTION_SCRIPTS_DIR=production
if [[ -z "${WORKSPACE}" ]]
then
   export RUNNING_ON_HUDSON=false
else
   export RUNNING_ON_HUDSON=true
fi
echo -e "\n\t[INFO]RUNNING_ON_HUDSON: $RUNNING_ON_HUDSON"

# To allow this cron job to work from hudson, or traditional crontab
if [[ -z "${WORKSPACE}" ]]
then
  export UTILITIES_HOME=/shared/eclipse
  source $BUILD_HOME/bootstrap.shsource
  makeProductionDirectoryOnBuildMachine
  # build_eclipse_org.shsource should come from branch 
  # though ideally  the rest of "production" directory would be identical between branches.
${BUILD_ROOT}/${PRODUCTION_SCRIPTS_DIR}/master-build.sh "${BUILD_ROOT}/${PRODUCTION_SCRIPTS_DIR}/build_eclipse_org.shsource" 
else
  export UTILITIES_HOME=${WORKSPACE}/utilities/production
  source $UTILITIES_HOME/sdk/bootstrap/bootstrap.shsource
  $UTILITIES_HOME/master-build.sh $UTILITIES_HOME/build_eclipse_org.shsource
fi

