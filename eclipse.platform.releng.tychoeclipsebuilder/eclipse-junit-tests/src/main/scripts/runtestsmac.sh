#!/usr/bin/env bash

# This file should never exist or be needed for production machine,
# but allows an easy way for a "local user" to provide this file
# somewhere on the search path ($HOME/bin is common),
# and it will be included here, thus can provide "override values"
# to those defined by defaults for production machine.,
# such as for vmcmd

source localTestsProperties.shsource

# by default, use the java executable on the path for outer and test jvm
#vmcmd=/shared/common/jdk-1.6.x86_64/jre/bin/java
if [[ -z "{vmcmd}" ]]
then
    vmcmd=java
fi

echo "vmcmd: $vmcmd"

#this value must be set when using rsh to execute this script, otherwise the script will execute from the user's home directory
dir=.

# operating system, windowing system and architecture variables
os=
ws=
arch=

# list of tests (targets) to execute in test.xml
tests=

# default value to determine if eclipse should be reinstalled between running of tests
installmode="clean"

# name of a property file to pass to Ant
properties=

# ext dir customization. Be sure "blank", if not defined explicitly on command line
extdirproperty=

# message printed to console
usage="usage: $0 -os <osType> -ws <windowingSystemType> -arch <architecture> [-noclean] [<test target>][-properties <path>]"


# proces command line arguments
while [ $# -gt 0 ]
do
    case "${1}" in
        -dir)
            dir="${2}"; shift;;
        -os)
            os="${2}"; shift;;
        -ws)
            ws="${2}"; shift;;
        -arch)
            arch="${2}"; shift;;
        -noclean)
            installmode="noclean";;
        -properties)
            properties="-propertyfile ${2}";shift;;
        -extdirprop)
            extdirproperty="-Djava.ext.dirs=${2}";shift;;
        -vm)
            vmcmd="${2}"; shift;;
        *)
            tests=$tests\ ${1};;
	esac
	shift
done

echo "Specified test targets (if any): ${tests}"

echo "Specified ext dir (if any): ${extdirproperty}"

# for *nix systems, os, ws and arch values must be specified
if [ "x$os" = "x" ]
then
	echo >&2 "$usage"
	exit 1
fi

if [ "x$ws" = "x" ]
then
	echo >&2 "$usage"
	exit 1
fi

if [ "x$arch" = "x" ]
then
	echo >&2 "$usage"
	exit 1
fi

#necessary when invoking this script through rsh
cd $dir

if [[ ! -r eclipse ]]
then
	tar -xzf eclipse-SDK-*.tar.gz
    # note, the file pattern to match, must not start with */plugins because there is no leading '/' in the zip file, since they are repos.
	unzip -qq -o -C eclipse-junit-tests-*.zip plugins/org.eclipse.test* -d eclipse/dropins/
fi


# run tests
launcher=`ls eclipse/plugins/org.eclipse.equinox.launcher_*.jar`

# it have been recommended not to "probe and publish" information about systems
# for slight improvement in security. Bug 387747
# so I have commented out most such probes, so they won't be routine.

    #echo "list all environment variables in effect as tests start"
    #printenv

# -Dtimeout=300000 "${ANT_OPTS}"
 if [[ ! -z "${extdirproperty}" ]]
 then
	$vmcmd "${extdirproperty}" -Dosgi.os=$os -Dosgi.ws=$ws -Dosgi.arch=$arch  -jar $launcher -data workspace -application org.eclipse.ant.core.antRunner -file ${PWD}/test.xml $tests -Dws=$ws -Dos=$os -Darch=$arch -D$installmode=true $properties -logger org.apache.tools.ant.DefaultLogger
 else
	$vmcmd -Dosgi.os=$os -Dosgi.ws=$ws -Dosgi.arch=$arch  -jar $launcher -data workspace -application org.eclipse.ant.core.antRunner -file ${PWD}/test.xml $tests -Dws=$ws -Dos=$os -Darch=$arch -D$installmode=true $properties -logger org.apache.tools.ant.DefaultLogger
 fi


