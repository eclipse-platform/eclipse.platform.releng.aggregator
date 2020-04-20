#!/usr/bin/env bash
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

# Utility to run on Hudson, to periodically confirm that our 
# atomic composite repositories are valid. 

# can be retrieved, on Hudson, with 
# wget --no-verbose --no-cache  -O checkComposites.sh https://${GIT_HOST}/c/platform/eclipse.platform.releng.aggregator.git/plain/production/miscToolsAndNotes/checkComposites/checkComposites.sh;
# and typically set chmod +x checkComposites.sh
# and then executed in "bash script" build step.

dropsPath=/home/data/httpd/download.eclipse.org/eclipse/downloads/drops4
repoHttpAccess=https://download.eclipse.org
repoAccess=${repoHttpAccess}
# TODO: reduce this list soon
repoList="\
/eclipse/updates/4.15/ \
/eclipse/updates/4.15-P-builds/ \
/eclipse/updates/4.16/ \
/eclipse/updates/4.16-I-builds/ \
/eclipse/updates/4.16milestones/ \
/eclipse/updates/4.16-Y-builds/ \
/eclipse/updates/4.16-P-builds/ \
"


# Confirm that Eclipse Platform has already been installed, if not, install it
if [[ ! -d ${WORKSPACE}/eclipse ]]
then
    epRelDir=$(ssh genie.releng@projects-storage.eclipse.org ls -d --format=single-column ${dropsPath}/R-*|sort|tail -1)
    scp genie.releng@projects-storage.eclipse.org:${epRelDir}/eclipse-platform-*-linux-gtk-x86_64.tar.gz .
    tar -xzf eclipse-platform-*-linux-gtk-x86_64.tar.gz -C ${WORKSPACE}
fi

for repo in ${repoList}
do
  echo -e "\n\n\tChecking repo:\n\t\t ${repoAccess}${repo}\n\n"
  nice -n 10 ${WORKSPACE}/eclipse/eclipse -nosplash -consolelog --launcher.suppressErrors -application org.eclipse.equinox.p2.director -repository ${repoAccess}${repo} -list -vm /opt/tools/java/oracle/jdk-8/latest/bin/java
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo -e "\n\t[ERROR]: p2.director list returned a non-zero return code: $RC"
    exit $RC
  fi
done

