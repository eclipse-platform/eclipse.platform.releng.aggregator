#!/usr/bin/env bash

# Simple utility to run as cronjob to run Eclipse Platform builds
# Normally resides in $BUILD_HOME

# set minimal path for consistency across machines
# plus, cron jobs do not inherit an environment
# care is needed not have anything in ${HOME}/bin that would effect the build 
# unintentionally, but is required to make use of "source buildeclipse.shsource". 
# Likely only a "release engineer" would be interested, such as to override "SIGNING" for 
# a test I-build on a remote machine. 
export PATH=/usr/local/bin:/usr/bin:/bin:${HOME}/bin
# unset common variables which we don't want (or, set ourselves)
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

# this file is to ease local builds to override some variables. It should not be used for production builds.
source buildeclipse.shsource 2>/dev/null

export BUILD_HOME=${BUILD_HOME:-/shared/eclipse/builds}
export JAVA_HOME=${JAVA_HOME:-/shared/common/jdk1.7.0_11}
export ANT_HOME=${ANT_HOME:-/shared/common/apache-ant-1.8.4}
export ANT_OPTS=${ANT_OPTS:-"-Dbuild.sysclasspath=ignore -Dincludeantruntime=false"}
export MAVEN_PATH=${MAVEN_PATH:-/shared/common/apache-maven-3.0.4/bin}

# no override for minimal $PATH
export PATH=$JAVA_HOME/bin:$MAVEN_PATH:$ANT_HOME/bin:$PATH

export BRANCH=master
export BUILD_TYPE=N
export STREAM=4.3.0

$BUILD_HOME/bootstrap.sh $BRANCH $BUILD_TYPE $STREAM

# remove all '.' from stream number
BUILDSTREAMTYPEDIR=${STREAM//./}$BUILD_TYPE

# for now, we "redfine" BUILD_ROOT for smaller, incremental change, but eventually, may work 
# though all scripts so "BRANCH" is no longer part of directory name
export BUILD_ROOT=${BUILD_HOME}/${BUILDSTREAMTYPEDIR}

# Any invocation of Java, Ant, Maven, etc., should use this as default TMP direcotory, 
# instead of the default /tmp by using 
# -Djava.io.tmpdir=${TMP_DIR}
export TMP_DIR=${TMP_DIR:-${BUILD_ROOT}/tmp}
mkdir -p ${TMP_DIR}
export MAVEN_OPTS=${MAVEN_OPTS:-"-Xmx2048m -XX:MaxPermSize=512m -Dtycho.localArtifacts=ignore -Djava.io.tmpdir=${TMP_DIR}"}
#export MAVEN_OPTS=${MAVEN_OPTS:-"-Xmx2048m -XX:MaxPermSize=512m -Djava.io.tmpdir=${TMP_DIR}"}

env > $BUILD_ROOT/env.txt
echo "= = = = = " >> $BUILD_ROOT/env.txt
java -version  >> $BUILD_ROOT/env.txt 2>&1
ant -version >> $BUILD_ROOT/env.txt
mvn -version >> $BUILD_ROOT/env.txt
echo "= = = = = " >> $BUILD_ROOT/env.txt

${BUILD_ROOT}/scripts/master-build.sh ${BUILD_ROOT}/scripts/build_eclipse_org.env

