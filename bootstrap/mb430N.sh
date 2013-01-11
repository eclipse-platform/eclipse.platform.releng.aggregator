#!/usr/bin/env bash

# Simple utility to run as cronjob to run Eclipse Platform builds
# Normally resides in $BUILD_HOME

# path required when starting from cron job
export PATH=/usr/local/bin:/usr/bin:/bin:${HOME}/bin:

# 0002 is often the default for shell users, but it is not when ran from
# a cron job, so we set it explicitly, so releng group has write access to anything
# we create.
oldumask=`umask`
umask 0002
echo "umask explicitly set to 0002, old value was $oldumask"

# this file is to ease local builds to override some variables. It should not be used for production builds.
source buildeclipse.shsource 2>/dev/null

export BUILD_HOME=/shared/eclipse/builds

branch=master
buildtype=N
eclipseStream=4.3.0

$BUILD_HOME/bootstrap.sh $branch $buildtype $eclipseStream

# remove all '.' from stream number
BUILDSTREAMTYPEDIR=${eclipseStream//./}$buildtype
${BUILD_HOME}/${BUILDSTREAMTYPEDIR}/scripts/master-build.sh ${BUILD_HOME}/${BUILDSTREAMTYPEDIR}/scripts/build_eclipse_org.env

