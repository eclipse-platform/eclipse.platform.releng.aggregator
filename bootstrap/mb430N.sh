#!/usr/bin/env bash

# Simple utility to run as cronjob to run Eclipse Platform builds
# Normally resides in $BUILD_HOME

# basic path required when starting from cron job
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

export BRANCH=master
export BUILD_TYPE=N
export STREAM=4.3.0

$BUILD_HOME/bootstrap.sh $BRANCH $BUILD_TYPE $STREAM

# remove all '.' from stream number
BUILDSTREAMTYPEDIR=${STREAM//./}$BUILD_TYPE

# for now, we "redfine" BUILD_ROOT for smaller, incremental change, but eventually, may work 
# though all scripts so "BRANCH" is no longer part of directory name
export BUILD_ROOT=${BUILD_HOME}/${BUILDSTREAMTYPEDIR}

${BUILD_ROOT}/scripts/master-build.sh ${BUILD_ROOT}/scripts/build_eclipse_org.env

