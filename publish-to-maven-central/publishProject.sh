#!/bin/sh
#*******************************************************************************
# Copyright (c) 2016, 2018 GK Software SE and others.
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
	SETTINGS_NAME=releng
	GPG_KEY_ID=CC641282
elif [ ${PROJECT} == pde ]; then
	SETTINGS_NAME=pde
	GPG_KEY_ID=86085154
elif [ ${PROJECT} == jdt ]; then
	SETTINGS_NAME=jdt
	GPG_KEY_ID=B414F87E
else
	echo "Unexpected project: '$PROJECT'"
	exit 1
fi

REPO_BASE=${WORKSPACE}/archive
REPO=${REPO_BASE}/repo-${REPO_ID}
PROJECT_PATH=org/eclipse/${PROJECT}

wget -nv https://ci.eclipse.org/releng/job/CBIaggregator/${REPO_ID}/artifact/*zip*/archive.zip
unzip -q archive.zip

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

SETTINGS=/home/jenkins/.m2/settings-deploy-ossrh-${SETTINGS_NAME}.xml
MVN=/opt/tools/apache-maven/latest/bin/mvn

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

	echo "${MVN} -f ${PROJECT}-pom.xml -s ${SETTINGS} gpg:sign-and-deploy-file -Dgpg.key.id=${GPG_KEY_ID} -Durl=${URL} -DrepositoryId=${REPO} -Dfile=${file} -DpomFile=${pomFile}"
	echo ${MVN} -f ${PROJECT}-pom.xml -s ${SETTINGS} gpg:sign-and-deploy-file \
		-Dgpg.key.id=${GPG_KEY_ID} \
		-Durl=${URL} -DrepositoryId=${REPO} \
		-Dfile=${file} -DpomFile=${pomFile} \
		>> .log/artifact-upload.txt

	if [ -f "${sourcesFile}" ]; then
		echo -e "\t${sourcesFile}"
		echo ${MVN} -f ${PROJECT}-pom.xml -s ${SETTINGS} gpg:sign-and-deploy-file \
			-Dgpg.key.id=${GPG_KEY_ID} \
			-Durl=${URL} -DrepositoryId=${REPO} \
			-Dfile=${sourcesFile} -DpomFile=${pomFile} -Dclassifier=sources \
			>> .log/sources-upload.txt
	fi

	if [ -f "${javadocFile}" ]; then
		echo -e "\t${javadocFile}"
		echo ${MVN} -f ${PROJECT}-pom.xml -s ${SETTINGS} gpg:sign-and-deploy-file \
			-Dgpg.key.id=${GPG_KEY_ID} \
			-Durl=${URL} -DrepositoryId=${REPO} \
			-Dfile=${javadocFile} -DpomFile=${pomFile} -Dclassifier=javadoc \
			>> .log/javadoc-upload.txt
		fi
done

ls -la .log

grep "BUILD FAILURE" .log/*
if [ "$?" -eq 0 ]; then
	echo "Deployment failed, see logs for details"
	exit 1
fi
