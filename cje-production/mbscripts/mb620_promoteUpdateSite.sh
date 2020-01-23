#!/bin/bash

#*******************************************************************************
# Copyright (c) 2019 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Sravan Lakkimsetti - initial API and implementation
#*******************************************************************************
set -e

if [ $# -ne 1 ]; then
  echo USAGE: $0 env_file
  exit 1
fi

source $CJE_ROOT/scripts/common-functions.shsource
source $1

epUpdateDir=/home/data/httpd/download.eclipse.org/updates
dropsPath=${epUpdateDir}/${STREAMMajor}.${STREAMMinor}-${BUILD_TYPE}-builds
pushd $CJE_ROOT/$UPDATES_DIR
scp -r ${BUILD_ID} genie.releng@projects-storage.eclipse.org:${dropsPath}/.
popd

epDownloadDir=/home/data/httpd/download.eclipse.org/eclipse
workingDir=${epDownloadDir}/workingDir
workspace=${workingDir}/${JOB_NAME}-${BUILD_NUMBER}

ssh genie.releng@projects-storage.eclipse.org rm -rf ${workingDir}/${JOB_NAME}*
ssh genie.releng@projects-storage.eclipse.org mkdir -p ${workspace}

#get java 8
scp -r /opt/tools/java/oracle/jdk-8/latest genie.releng@projects-storage.eclipse.org:${workspace}/jdk8

#get latest Eclipse platform product
epRelDir=$(ssh genie.releng@projects-storage.eclipse.org ls -d --format=single-column ${dropsPath}/R-*|sort|tail -1)
ssh genie.releng@projects-storage.eclipse.org tar -C ${workspace} -xzf ${epRelDir}/eclipse-platform-*-linux-gtk-x86_64.tar.gz

#get requisite tools
ssh genie.releng@projects-storage.eclipse.org wget -O ${workspace}/addToComposite.xml https://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/plain/cje-production/scripts/addToComposite.xml

#triggering ant runner
baseBuilderDir=${workspace}/eclipse
javaCMD=${workspace}/jdk8/bin/java

launcherJar=$(ssh genie.releng@projects-storage.eclipse.org find ${baseBuilderDir}/. -name "org.eclipse.equinox.launcher_*.jar" | sort | head -1 )

devworkspace=${workspace}/workspace-antRunner
devArgs=-Xmx512m
extraArgs="addToComposite -Drepodir=${dropsPath} -Dcomplocation=${BUILD_ID}"

ssh genie.releng@projects-storage.eclipse.org  ${javaCMD} -jar ${launcherJar} -nosplash -consolelog -debug -data $devworkspace -application org.eclipse.ant.core.antRunner -file ${workspace}/addToComposite.xml ${extraArgs} -vmargs $devArgs

ssh genie.releng@projects-storage.eclipse.org rm -rf ${workingDir}/${JOB_NAME}*
