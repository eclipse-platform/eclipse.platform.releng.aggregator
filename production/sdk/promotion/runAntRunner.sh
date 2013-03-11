#!/usr/bin/env bash

# Utility to invoke eclipse antrunner, from the "base builder", which should already be 
# installed on the build machine, where the build is.
#
#    the build file, if not build.xml, must be second argument
#    that can be followed be target or other arguments

# this buildeclipse.shsource file is to ease local builds to override some variables. 
# It should not be used for production builds.
source buildeclipse.shsource 2>/dev/null

# If used for other things, where a build doesn't exist, 
# could/should likely use something like 'temp' or some other "cache" directory.
buildId=$1
if [[ -z "${buildId}" ]]
then
    echo "This script requires previous (local) build Id as input."
    echo "    For example, $( basename $0 ) I20130306-0033"
    exit 1
fi
shift

BUILD_HOME=${BUILD_HOME:-/shared/eclipse/builds}
# TODO: make stream sensitive
BUILD_ROOT=${BUILD_ROOT:-${BUILD_HOME}/4I}
# TODO: make stream sensitive
basebuilderParent=${BUILD_ROOT}/siteDir/eclipse/downloads/drops4/${buildId}
if [[ ! -d "${basebuilderParent}" ]]
then
    echo "ERROR: The directory did not exist. Must name existing directory where basebuilder is, or will be installed."
    echo "    basebuilderParent: ${basebuilderParent}"
    exit 1
fi

# TODO: we could call/check install basebuilder here, but not required for 
# immediate use case of using to composite repos.

baseBuilderDir=${basebuilderParent}/org.eclipse.releng.basebuilder
if [[ ! -d "${baseBuilderDir}" ]]
then
    echo "ERROR: The directory did not exist."
    echo "    baseBuilderDir: ${baseBuilderDir}"
    exit 1
fi

launcherJar=$( find $baseBuilderDir/ -name "org.eclipse.equinox.launcher_*.jar" | sort | head -1 )
if [[ -z "${launcherJar}" || ! -f "${launcherJar}" ]]
then
    echo "ERROR: The launcher did not exist."
    echo "    launcherJar: ${launcherJar}"
    exit 1
fi

if [[ -z "${JAVA_HOME}" ]]
then
  export JAVA_HOME=${JAVA_HOME:-/shared/common/jdk1.7.0_11}
fi
if [[ ! -d "${JAVA_HOME}" ]]
then
    echo "ERROR: JAVA_HOME did not exist."
    echo "    JAVA_HOME: ${JAVA_HOME}"
    exit 1
fi
javaCMD=${JAVA_HOME}/bin/java

BUILDFILE=$1
if [ -e $BUILDFILE ]
then
    BUILDFILESTR=" -file $BUILDFILE"
    shift
else
    BUILDFILESTR=" -file build.xml"
fi

# use special $@ to keep all (remaining) arguments quoted (instead of one big string)
extraArgs="$@"

echo
echo " BUILDFILESTR: $BUILDFILESTR"
if [ -n "${extraArgs}" ]
then
    echo "   extraArgs: ${extraArgs}"
    echo "      as it is right now, target name must be first \"extraArg\" if specifying one."
fi
echo


devworkspace="${BUILD_HOME}"/workspace-antRunner
devArgs=-Xmx256m

echo
echo "   buildId:           ${buildId}"
echo "   basebuilderParent: ${basebuilderParent}"
echo "   baseBuilderDir:    ${baseBuilderDir}"
echo "   launcherJar:       ${launcherJar}"
echo "   BUILD_HOME:        ${BUILD_HOME}"
echo "   dev script:        $0"
echo "   devworkspace:      $devworkspace"
echo "   devArgs:           $devArgs"
echo "   javaCMD:           $javaCMD"
echo

${javaCMD}  -jar ${launcherJar} --launcher.suppressErrors  -nosplash -console -data $devworkspace -application org.eclipse.ant.core.antRunner $BUILDFILESTR ${extraArgs} -vmargs $devArgs

