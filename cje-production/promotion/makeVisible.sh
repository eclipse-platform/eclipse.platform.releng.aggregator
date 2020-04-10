#!/bin/bash -x
#*******************************************************************************
# Copyright (c) 2020 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Sravan Kumar Lakkimsetti - initial API and implementation
#*******************************************************************************


DROP_ID=$(echo $DROP_ID|tr -d ' ')

if [[ -z "${DROP_ID}" ]]
then
  echo -e "\n\t[ERROR] DROP_ID must be defined for ${0##*/}"
  exit 1
else
  export DROP_ID
  echo -e "\n\t[INFO] DROP_ID: $DROP_ID"  
fi

# CHECKPOINT is the code for either milestone (M1, M2, ...) 
# or release candidate (RC1, RC2, ...). 
# It should be empty for the final release.
CHECKPOINT=$(echo $CHECKPOINT|tr -d ' ')
if [[ -z "${CHECKPOINT}" ]]
then
  echo -e "\n\t[WARNING] CHECKPOINT was blank in ${0##*/}"
else
  export CHECKPOINT
  echo -e "\n\t[INFO] CHECKPOINT: $CHECKPOINT"  
fi

# STREAM is the three digit release number, such as 4.7.0 or 4.6.1.
STREAM=$(echo $STREAM|tr -d ' ')
if [[ -z "${STREAM}" ]]
then
  echo -e "\n\t[ERROR] STREAM must be defined for ${0##*/}"
  exit 1
else
  export STREAM
  echo -e "\n\t[INFO] STREAM: $STREAM"  
fi

# DL_TYPE ("download type") is the build type we are naming 
# the build *TO*
# for main line (master) code, it is always 'S' (from I-build) until it's 'R'
#export DL_TYPE=S
#export DL_TYPE=R
DL_TYPE=$(echo $DL_TYPE|tr -d ' ')
if [[ -z "${DL_TYPE}" ]]
then
  echo -e "\n\t[ERROR] DL_TYPE must be defined for ${0##*/}"
  exit 1
else
  # Could probably define default - or validate! - based on first letter of DROP_ID
  # M --> M
  # I --> S
  export DL_TYPE
  echo -e "\n\t[INFO] DL_TYPE: $DL_TYPE"  
fi

SSH_PREFIX="ssh genie.releng@projects-storage.eclipse.org"

# Main promotion scripts starts here

if [[ "${STREAM}" =~ ^([[:digit:]]+)\.([[:digit:]]+)\.([[:digit:]]+)$ ]]
then
  export BUILD_MAJOR=${BASH_REMATCH[1]}
  export BUILD_MINOR=${BASH_REMATCH[2]}
  export BUILD_SERVICE=${BASH_REMATCH[3]}
else
  echo "STREAM must contain major, minor, and service versions, such as 4.3.0"
  echo "    but found ${STREAM}"
  exit 1
fi

# regex section
# BUILD_TYPE is the prefix of the build --
# that is, for what we are renaming the build FROM
PATTERN="^([MI])([[:digit:]]{8})-([[:digit:]]{4})$"
if [[ "${DROP_ID}" =~ $PATTERN ]]
then
  export BUILD_TYPE=${BASH_REMATCH[1]}
  export BUILD_TIMESTAMP=${BASH_REMATCH[2]}${BASH_REMATCH[3]}
  # Label and ID are the same, in this case
  export BUILD_LABEL=$DROP_ID
else
  echo -e "\n\tERROR: DROP_ID, ${DROP_ID}, did not match any expected pattern."
  exit 1
fi

# For initial releases, do not include service in label
if [[ "${BUILD_SERVICE}" == "0" ]]
then
  export DL_LABEL=${BUILD_MAJOR}.${BUILD_MINOR}${CHECKPOINT}
else
  export DL_LABEL=${BUILD_MAJOR}.${BUILD_MINOR}.${BUILD_SERVICE}${CHECKPOINT}
fi

# This is DL_DROP_ID for Eclipse. The one for equinox has DL_LABEL_EQ in middle.
export DL_DROP_ID=${DL_TYPE}-${DL_LABEL}-${BUILD_TIMESTAMP}

# Build machine locations (would very seldom change)
export BUILD_ROOT=${BUILD_ROOT:-/home/data/httpd/download.eclipse.org}

export BUILDMACHINE_BASE_DL=${BUILD_ROOT}/eclipse/downloads/drops4
export BUILDMACHINE_BASE_EQ=${BUILD_ROOT}/equinox/drops

# Eclipse Drop Site (final segment)
export ECLIPSE_DL_DROP_DIR_SEGMENT=${DL_TYPE}-${DL_LABEL}-${BUILD_TIMESTAMP}

${SSH_PREFIX} rm ${BUILDMACHINE_BASE_DL}/${ECLIPSE_DL_DROP_DIR_SEGMENT}/buildHidden
${SSH_PREFIX} rm ${BUILDMACHINE_BASE_EQ}/${ECLIPSE_DL_DROP_DIR_SEGMENT}/buildHidden

#Add to composite
buildId=${ECLIPSE_DL_DROP_DIR_SEGMENT}
case ${DL_TYPE} in
  "S" )
    export REPO_SITE_SEGMENT=${BUILD_MAJOR}.${BUILD_MINOR}milestones
    ;;
  "R" )
    export REPO_SITE_SEGMENT=${BUILD_MAJOR}.${BUILD_MINOR}
    ;;
  *)
    echo -e "\n\tERROR: case statement for repo output did not match any pattern."
    echo -e   "\t       Not written to handle DL_TYPE of ${DL_TYPE}\n"
    exit 1
esac

epDownloadDir=/home/data/httpd/download.eclipse.org/eclipse
dropsPath=${epDownloadDir}/downloads/drops4
p2RepoPath=${epDownloadDir}/updates
buildDir=${dropsPath}/${buildId}

workingDir=${epDownloadDir}/workingDir

workspace=${workingDir}/${JOB_NAME}-${BUILD_NUMBER}

ssh genie.releng@projects-storage.eclipse.org rm -rf ${workingDir}/${JOB_NAME}*

ssh genie.releng@projects-storage.eclipse.org mkdir -p ${workspace}
ssh genie.releng@projects-storage.eclipse.org cd ${workspace}

#get latest Eclipse platform product
epRelDir=$(ssh genie.releng@projects-storage.eclipse.org ls -d --format=single-column ${dropsPath}/R-*|sort|tail -1)
ssh genie.releng@projects-storage.eclipse.org tar -C ${workspace} -xzf ${epRelDir}/eclipse-platform-*-linux-gtk-x86_64.tar.gz

#get requisite tools
ssh genie.releng@projects-storage.eclipse.org wget -O ${workspace}/addToComposite.xml https://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/plain/cje-production/scripts/addToComposite.xml

#triggering ant runner
baseBuilderDir=${workspace}/eclipse
javaCMD=/opt/public/common/java/openjdk/jdk-11_x64-latest/bin/java

launcherJar=$(ssh genie.releng@projects-storage.eclipse.org find ${baseBuilderDir}/. -name "org.eclipse.equinox.launcher_*.jar" | sort | head -1 )

scp genie.releng@projects-storage.eclipse.org:${buildDir}/buildproperties.shsource .
source ./buildproperties.shsource
repoDir=/home/data/httpd/download.eclipse.org/eclipse/updates/${REPO_SITE_SEGMENT}

devworkspace=${workspace}/workspace-antRunner
devArgs=-Xmx512m
extraArgs="addToComposite -Drepodir=${repoDir} -Dcomplocation=${buildId}"
ssh genie.releng@projects-storage.eclipse.org  ${javaCMD} -jar ${launcherJar} -nosplash -consolelog -debug -data $devworkspace -application org.eclipse.ant.core.antRunner -file ${workspace}/addToComposite.xml ${extraArgs} -vmargs $devArgs

ssh genie.releng@projects-storage.eclipse.org rm -rf ${workingDir}/${JOB_NAME}*

#Tag Source
if [[ "${DL_TYPE}" != "R" ]]
then 
  TAG=${DL_TYPE}${BUILD_MAJOR}_${BUILD_MINOR}_${BUILD_SERVICE}_${CHECKPOINT}

  cd ${WORKSPACE}
  git config --global user.email "releng-bot@eclipse.org"
  git config --global user.name "Eclipse Releng Bot"
  git clone --recursive ssh://genie.releng@git.eclipse.org:29418/platform/eclipse.platform.releng.aggregator.git

  pushd eclipse.platform.releng.aggregator
  git checkout master
  git submodule foreach git checkout master
  git submodule foreach git clean -f -d -x
  git submodule foreach git clean -f -d -x
  git reset --hard
  git submodule foreach git reset --hard
  git checkout master
  git submodule foreach git checkout master
  git pull
  git submodule foreach git pull

  git submodule foreach git tag -a -m "${DL_LABEL}" ${TAG} ${DROP_ID}
  git tag -a -m "${DL_LABEL}" ${TAG} ${DROP_ID}
  RC=$?
  if [[ $RC != 0 ]]
  then
    printf "\n\t%s\n" "ERROR: Failed to tag aggregator old id, ${DROP_ID}, with new tag, ${TAG} and annotation of ${DL_LABEL}."
    popd
    exit $RC
  fi
  git submodule foreach git push --verbose origin tag ${TAG}
  git push origin tag ${TAG}
  RC=$?
  if [[ $RC != 0 ]]
  then
    printf "\n\t%s\n" "ERROR: Failed to push new tag, ${TAG}."
    popd
    exit $RC
  fi
  popd
fi
