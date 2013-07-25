#!/bin/bash

BASEDIR=$(pwd)
LOG=$BASEDIR/log_$( date +%Y%m%d%H%M%S ).txt
exec >$LOG 2>&1

BRANCH=master
GIT_PREFIX=ssh://git.eclipse.org
javaHome=/opt/local/jdk1.7.0_07
mvnPath=/opt/pwebster/git/cbi/apache-maven-3.1.0/bin
updateAggregator=false
mavenBREE=-Pno-bree-libs

while [ $# -gt 0 ]
do
    case "$1" in
	"-v")
	    mavenVerbose=-X;;
	"-bree-libs")
	    mavenBREE=-Pbree-libs;;
	"-sign")
	    mavenSign=-Peclipse-sign;;
	"-update")
	    updateAggregator=true;;
	"-anonymous")
	    GIT_PREFIX=git://git.eclipse.org;;
	"-gitPrefix")
	    GIT_PREFIX="$2" ; shift;;
	"-branch")
	    BRANCH="$2" ; shift;;
	"-javaHome")
	    javaHome="$2" ; shift;;
	"-mavenPath")
	    mvnPath="$2" ; shift;;
    esac
    shift
done


export MAVEN_OPTS=-Xmx2560m
LOCAL_REPO=$BASEDIR/localRepo


if [ -z "$JAVA_HOME" ]; then
    export JAVA_HOME=$javaHome
fi

mvnRegex=$( echo $mvnPath | sed 's!/!.!g' )
if ! (echo $PATH | grep "$mvnRegex" >/dev/null ); then
    export PATH=${mvnPath}:$PATH
fi


cloneAggregator() {
    if [ ! -d eclipse.platform.releng.aggregator ]; then
	git clone \
	-b $BRANCH \
	${GIT_PREFIX}/gitroot/platform/eclipse.platform.releng.aggregator.git
	pushd eclipse.platform.releng.aggregator
	git submodule init
	# this will take a while ... a long while
	git submodule update
	popd
    else
	pushd eclipse.platform.releng.aggregator
	git fetch
	git checkout $BRANCH
	git pull
	git submodule update
	popd
    fi
}

installEclipseParent () {
    pushd eclipse.platform.releng.aggregator
    mvn -f eclipse-platform-parent/pom.xml \
    clean install \
    -Dmaven.repo.local=$LOCAL_REPO
    popd
}

buildAggregator () {
    pushd eclipse.platform.releng.aggregator
    mvn $mavenVerbose \
    clean install \
    $mavenSign \
    $mavenBREE \
    -Dmaven.test.skip=true \
    -Dmaven.repo.local=$LOCAL_REPO
    popd
}

# steps to get going

if $updateAggregator; then
    cloneAggregator
fi

# pick up any changes
installEclipseParent

# build from the aggregator root
buildAggregator

