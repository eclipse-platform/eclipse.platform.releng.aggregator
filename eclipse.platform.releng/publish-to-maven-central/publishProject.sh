#!/bin/sh
#*******************************************************************************
# Copyright (c) 2016, 2025 GK Software SE and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Stephan Herrmann - initial API and implementation
#********************************************************************************

#PROJECT=jdt
#PROJECT=pde
#PROJECT=platform

gpg --batch --import "${KEYRING}"
for fpr in $(gpg --list-keys --with-colons  | awk -F: '/fpr:/ {print $10}' | sort -u); do
	echo -e "5\ny\n" |  gpg --batch --command-fd 0 --expert --edit-key ${fpr} trust
done

if [ ${PROJECT} == platform ]; then
	GPG_KEY_ID=CC641282
elif [ ${PROJECT} == pde ]; then
	GPG_KEY_ID=86085154
elif [ ${PROJECT} == jdt ]; then
	GPG_KEY_ID=B414F87E
else
	echo "Unexpected project: '$PROJECT'"
	exit 1
fi

if [[ -z ${REPO} ]]; then
	echo 'Required environment variable 'REPO' is not defined.'
	exit 1
fi
PROJECT_PATH=org/eclipse/${PROJECT}

if [ ! -d ${REPO} ]
then
	echo "No repo at ${REPO}"
	exit 1
fi

echo "==== Copy artifacts from ${REPO}/${PROJECT_PATH} ===="

if [ -d ${PROJECT_PATH} ]
then
	rm -r ${PROJECT_PATH}/*
else
	mkdir -p ${PROJECT_PATH}
fi
cp -r ${REPO}/${PROJECT_PATH}/* ${PROJECT_PATH}/


echo "==== UPLOAD ===="

mkdir .log

for pomFile in ${PROJECT_PATH}/*/*/*.pom
do
	pomFolder=`dirname ${pomFile}`
	version=`basename ${pomFolder}`
	if [[ $version == *-SNAPSHOT ]]; then
		URL=https://repo.eclipse.org/content/repositories/eclipse-snapshots/
		REPO=repo.eclipse.org
		MAVEN_CENTRAL_URL=https://repo1.maven.org/maven2/${pomFolder%-SNAPSHOT}
		echo "Checking ${MAVEN_CENTRAL_URL}"
		if curl --output /dev/null --silent --head --fail "$MAVEN_CENTRAL_URL"; then
			echo "The released version of file $pomFile is already present at $MAVEN_CENTRAL_URL."
		fi
	else
		URL=https://oss.sonatype.org/service/local/staging/deploy/maven2/
		REPO=ossrh
		MAVEN_CENTRAL_URL=https://repo1.maven.org/maven2/${pomFolder}
		echo "Checking ${MAVEN_CENTRAL_URL}"
		if curl --output /dev/null --silent --head --fail "$MAVEN_CENTRAL_URL"; then
			echo "Skipping file $pomFile which is already present at $MAVEN_CENTRAL_URL"
			continue;
		fi
	fi

	file=`echo $pomFile | sed -e "s|\(.*\)\.pom|\1.jar|"`
	sourcesFile=`echo $pomFile | sed -e "s|\(.*\)\.pom|\1-sources.jar|"`
	javadocFile=`echo $pomFile | sed -e "s|\(.*\)\.pom|\1-javadoc.jar|"`

	echo -e "\t${file}"

	if [ -f "${sourcesFile}" ]; then
		echo -e "\t${sourcesFile}"
		SOURCES_ARG="-Dsources=${sourcesFile}"
	else
		SOURCES_ARG=""
		# If the -sources.jar is missing, and the main jar contains .class files, then we won't be able to promote this to Maven central.
		if unzip -l ${file} | grep -q -e '.class$'; then 
			echo "BUILD FAILURE ${file} contains .class files and requires a ${sourcesFile}" | tee -a .log/upload.txt
		else
			echo -e "\tMissing ${sourcesFile} but ${file} contains no .class files."
		fi; 
	fi

	if [ -f "${javadocFile}" ]; then
		echo -e "\t${javadocFile}"
		JAVADOC_ARG="-Djavadoc=${javadocFile}"
	else
		JAVADOC_ARG=""
		echo -e "\tMissing ${javadocFile}"
	fi

	echo "mvn -f project-pom.xml -s ${SETTINGS} gpg:sign-and-deploy-file -Dgpg.key.id=${GPG_KEY_ID} -Durl=${URL} -DrepositoryId=${REPO} -DpomFile=${pomFile} -Dfile=${file} ${SOURCES_ARG} ${JAVADOC_ARG}"
	mvn \
		-f project-pom.xml \
		-s ${SETTINGS} \
		gpg:sign-and-deploy-file \
		-Dgpg.key.id=${GPG_KEY_ID} \
		-Durl=${URL} \
		-DrepositoryId=${REPO} \
		-DpomFile=${pomFile} \
		-Dfile=${file} \
		${SOURCES_ARG} \
		${JAVADOC_ARG} \
		>> .log/upload.txt
done

ls -la .log

grep "BUILD FAILURE" .log/*
if [ "$?" -eq 0 ]; then
	echo "Deployment failed, see logs for details"
	exit 1
fi
