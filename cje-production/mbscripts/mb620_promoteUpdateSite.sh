#!/bin/bash -e

#*******************************************************************************
# Copyright (c) 2019, 2025 IBM Corporation and others.
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

if [ $# -ne 1 ]; then
  echo USAGE: $0 env_file
  exit 1
fi

source $CJE_ROOT/scripts/common-functions.shsource
source $1

compositeTargetSize=3
epUpdateDir=/home/data/httpd/download.eclipse.org/eclipse/updates
dropsPath=${epUpdateDir}/${STREAMMajor}.${STREAMMinor}-${BUILD_TYPE}-builds
latestRelDir=/home/data/httpd/download.eclipse.org/eclipse/downloads/drops4
java_home=/opt/public/common/java/openjdk/jdk-21_x64-latest/bin

pushd $CJE_ROOT/$UPDATES_DIR
scp -r ${BUILD_ID} genie.releng@projects-storage.eclipse.org:${dropsPath}/.
popd

if [ "$COMPARATOR_ERRORS" == "true" ] && [ "$BUILD_TYPE" == "I" ]
then
	exit 0
fi

epDownloadDir=/home/data/httpd/download.eclipse.org/eclipse
workingDir=${epDownloadDir}/workingDir
workspace=${workingDir}/${JOB_NAME}-${BUILD_NUMBER}

ssh genie.releng@projects-storage.eclipse.org rm -rf ${workingDir}/${JOB_NAME}*
ssh genie.releng@projects-storage.eclipse.org mkdir -p ${workspace}

#get latest Eclipse platform product
epRelDir=$(ssh genie.releng@projects-storage.eclipse.org ls -d --format=single-column ${latestRelDir}/R-*|sort|tail -1)
ssh genie.releng@projects-storage.eclipse.org tar -C ${workspace} -xzf ${epRelDir}/eclipse-platform-*-linux-gtk-x86_64.tar.gz

#get requisite tools
# Enhance the addToComposite ANT script to remove the oldest/earliest children to ensure the target-size is not exceeded
scp genie.releng@projects-storage.eclipse.org:${dropsPath}/compositeArtifacts.jar compositeArtifacts.jar
#Unzip compositeArtifacts.xml and read the current children from it
currentChildren=$(unzip -p compositeArtifacts.jar compositeArtifacts.xml |\
	xmllint - --xpath '/repository/children/child/@location' |\
	sed --expression 's|location="||g' --expression 's|"||g')
rm compositeArtifacts.jar
childrenArray=(${currentChildren})
echo "Current children of composite repository: ${childrenArray[@]}"

addToComposite_xml=$(curl https://download.eclipse.org/eclipse/relengScripts/cje-production/scripts/addToComposite.xml)
extraTasksMarker='<!--EXTRA_TASKS-->'

# One more child is about to be added to the composite.
# Remove those children from the beginning of the list if the target-size would be exceeded.
removalCount=$(( ${#childrenArray[@]} + 1 - compositeTargetSize ))
if [ "${removalCount}" -gt 0 ]; then
	childrenToRemove="${childrenArray[@]:0:${removalCount}}"
	echo "Remove from composite repsitory: ${childrenToRemove}"
	extraAntTask=''
	for child in ${childrenToRemove}; do
		extraAntTask+="<remove><repository location=\\\"${child}\\\"/></remove>\\n            "
	done
	addToComposite_xml=$(cat <<<"${addToComposite_xml}" | sed --expression "s|${extraTasksMarker}|${extraAntTask}|g")
else
	echo "Composite p2-repository contains only ${#childrenArray[@]} children and adding one will not exceed its target size of ${compositeTargetSize}: ${dropsPath}"
fi

ssh genie.releng@projects-storage.eclipse.org "cat<<<'${addToComposite_xml}' > ${workspace}/addToComposite.xml"

#triggering ant runner
baseBuilderDir=${workspace}/eclipse
javaCMD=${java_home}/java

launcherJar=$(ssh genie.releng@projects-storage.eclipse.org find ${baseBuilderDir}/. -name "org.eclipse.equinox.launcher_*.jar" | sort | head -1 )

devworkspace=${workspace}/workspace-antRunner
devArgs=-Xmx512m
extraArgs="addToComposite -Drepodir=${dropsPath} -Dcomplocation=${BUILD_ID}"

ssh genie.releng@projects-storage.eclipse.org  ${javaCMD} -jar ${launcherJar} -nosplash -consolelog -debug -data $devworkspace -application org.eclipse.ant.core.antRunner -file ${workspace}/addToComposite.xml ${extraArgs} -vmargs $devArgs

ssh genie.releng@projects-storage.eclipse.org rm -rf ${workingDir}/${JOB_NAME}*
