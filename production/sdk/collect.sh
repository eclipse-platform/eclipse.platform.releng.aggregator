#!/usr/bin/env bash

# Utility to be called from test data collection cron job,
# to invoke the main code. expected to be called with piped
# input, such as
# ./collect.sh < testjobs/testjobdata201210220811.txt

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
echo "umask explicitly set to 0002, old value was $oldumask"

# this localBuildProperties.shsource file is to ease local builds to override some variables.
# It should not be used for production builds.
source localBuildProperties.shsource 2>/dev/null
export BUILD_HOME=${BUILD_HOME:-/shared/eclipse/builds}

export JAVA_HOME=/shared/common/jdk1.7.0-latest
export ANT_HOME=/shared/common/apache-ant-1.9.2

export PATH=${JAVA_HOME}/bin:${ANT_HOME}/bin:$PATH

read inputline
echo " = = Properties in collect.sh == "
echo "inputline: $inputline"

job="$(echo $inputline | cut -d\  -f1)"
buildNumber="$(echo $inputline | cut -d\  -f2)"
buildId="$(echo $inputline | cut -d\  -f3)"
eclipseStream="$(echo $inputline | cut -d\  -f4)"
EBUILDER_HASH="$(echo $inputline | cut -d\  -f5)"

echo "job: $job"
echo "buildNumber: $buildNumber"
echo "buildId: $buildId"
echo "eclipseStream: $eclipseStream"
echo "EBUILDER_HASH: $EBUILDER_HASH"

${ANT_HOME}/bin/ant -version
#       -lib /shared/common/apache-ant-1.9.2/lib/ \
  ${ANT_HOME}/bin/ant -f /shared/eclipse/sdk/collectTestResults.xml \
  -Djob=${job} \
  -DbuildNumber=${buildNumber} \
  -DbuildId=${buildId} \
  -DeclipseStream=${eclipseStream} \
  -DEBUILDER_HASH=${EBUILDER_HASH} \
  -DBUILD_HOME=${BUILD_HOME}