#!/usr/bin/env bash
#*******************************************************************************
# Copyright (c) 2016 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     David Williams - initial API and implementation
#*******************************************************************************

# Utility to run on Hudson, to periodically confirm that our 
# atomic composite repositories are valid. 

# can be retrieved, on Hudson, with 
# wget --no-verbose --no-cache  -O checkComposites.sh http://${GIT_HOST}/c/platform/eclipse.platform.releng.aggregator.git/plain/production/miscToolsAndNotes/checkComposites/checkComposites.sh;
# and typically set chmod +x checkComposites.sh
# and then executed in "bash script" build step.

baseEclipseAccessDir=/home/data/httpd/download.eclipse.org
baseEclipseDirSegment=eclipse/downloads/drops4/R-4.10-201812060815
baseEclipse=eclipse-platform-4.10-linux-gtk-x86_64.tar.gz
repoFileAccess=file:///home/data/httpd/download.eclipse.org/
repoHttpAccess=http://download.eclipse.org
repoAccess=${repoFileAccess}
# TODO: reduce this list soon
repoList="\
/eclipse/updates/4.11/ \
/eclipse/updates/4.10/ \
/eclipse/updates/4.10-I-builds/ \
/eclipse/updates/4.10milestones/ \
/eclipse/updates/4.11-I-builds/ \
/eclipse/updates/4.11milestones/ \
/eclipse/updates/4.11-Y-builds/ \
/eclipse/updates/4.11-P-builds/ \
"


# WORKSPACE will be defined in Hudson. For convenience of local, remote, testing we will make several
# assumptions if it is not defined.
if [[ -z "${WORKSPACE}" ]]
then
  echo -e "\n\tWORKSPACE not defined. Assuming local, remote test."
  WORKSPACE=${PWD}
  # access can remain undefined if we have direct access, such as on Hudson.
  # The value used here will depend on .ssh/config
  access=build:
  repoAccess=${repoHttpAccess}
fi

# Confirm that Eclipse Platform has already been installed, if not, install it
if [[ ! -d ${WORKSPACE}/eclipse ]]
then
  # We assume we have file access to 'downloads'. If not direct, at least via rsync.
  echo -e "\n\trsynching eclipse platform archive to ${WORKSPACE}"
  echo -e "\n\trsync command: rsync ${access}${baseEclipseDirSegment}/${baseEclipse} ${WORKSPACE}"
  rsync ${access}${baseEclipseAccessDir}/${baseEclipseDirSegment}/${baseEclipse} ${WORKSPACE}
  tar -xf ${baseEclipse} -C ${WORKSPACE}
fi

for repo in ${repoList}
do
  echo -e "\n\n\tChecking repo:\n\t\t ${repoAccess}${repo}\n\n"
  nice -n 10 ${WORKSPACE}/eclipse/eclipse -nosplash -consolelog --launcher.suppressErrors -application org.eclipse.equinox.p2.director -repository ${repoAccess}${repo} -list -vm /shared/common/jdk1.8.0_x64-latest/bin/java
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo -e "\n\t[ERROR]: p2.director list returned a non-zero return code: $RC"
    exit $RC
  fi
done

