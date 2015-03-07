#!/usr/bin/env bash

# Utility to invoke eclipse antrunner, from the "base builder", which should already be
# installed on the build machine, where the build is.
#
#    the build file, if not build.xml, must be second argument
#    that can be followed be target or other arguments

# this localBuildProperties.shsource file is to ease local builds to override some variables.
# It should not be used for production builds.
source localBuildProperties.shsource 2>/dev/null
export BUILD_HOME=${BUILD_HOME:-/shared/eclipse/builds}

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

eclipseStream=$1
if [[ -z "${eclipseStream}" ]]
then
  echo "This script requires eclipseStream as input."
  echo "    For example, $( basename $0 ) I20130306-0033 4.3.0"
  exit 1
fi
shift

eclipseStreamMajor=${eclipseStream:0:1}
buildType=${buildId:0:1}

export BUILD_ROOT=${BUILD_ROOT:-${BUILD_HOME}/${eclipseStreamMajor}${buildType}}

dropSegment=drops
if (( ${eclipseStreamMajor} > 3 ))
then
  dropSegment=drops${eclipseStreamMajor}
fi

basebuilderParent=${BUILD_ROOT}/siteDir/eclipse/downloads/${dropSegment}/${buildId}
if [[ ! -d "${basebuilderParent}" ]]
then
  echo "ERROR: The directory did not exist. Must name existing directory where basebuilder is, or will be installed."
  echo "    basebuilderParent: ${basebuilderParent}"
  exit 1
fi

# TODO: we could check basebuilder here and if not, install it?
#        but not required for immediate use case of using to composite repos.

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
  export JAVA_HOME=${JAVA_HOME:-/shared/common/jdk1.8.0_x64-latest}
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
devArgs=-Xmx512m

echo
echo "   buildId:           ${buildId}"
echo "   buildId:           ${eclipseStream}"
echo "   basebuilderParent: ${basebuilderParent}"
echo "   baseBuilderDir:    ${baseBuilderDir}"
echo "   launcherJar:       ${launcherJar}"
echo "   BUILD_HOME:        ${BUILD_HOME}"
echo "   dev script:        $0"
echo "   devworkspace:      $devworkspace"
echo "   devArgs (-vmargs):  $devArgs"
echo "   javaCMD:           $javaCMD"
echo "   BUILDFILESTR:      $BUILDFILESTR"
echo "   extraArgs:         ${extraArgs}"
echo

${javaCMD}  -jar ${launcherJar} -nosplash -consolelog -debug -data $devworkspace -application org.eclipse.ant.core.antRunner $BUILDFILESTR ${extraArgs} -vmargs $devArgs

