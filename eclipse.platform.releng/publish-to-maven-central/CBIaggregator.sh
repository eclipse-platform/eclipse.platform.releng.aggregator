#!/bin/bash -e
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

#================================================================================
#   Parameters
#================================================================================

WORKING_ROOT=$(pwd)
# Directory containing this and other scripts and resources
BASE_DIR=$( cd -- "$(dirname ${0})" &> /dev/null && pwd )

# ECLIPSE:
SDK_BUILD_DIR=R-4.34-202411201800
SDK_VERSION=4.34
FILE_ECLIPSE="https://download.eclipse.org/eclipse/downloads/drops4/${SDK_BUILD_DIR}/eclipse-SDK-${SDK_VERSION}-linux-gtk-x86_64.tar.gz"

# AGGREGATOR:
URL_AGG_UPDATES=https://download.eclipse.org/cbi/updates/p2-aggregator/products/nightly/latest

# LOCAL TOOLS:
LOCAL_TOOLS=${WORKING_ROOT}/tools
DIR_AGGREGATOR=aggregator
AGGREGATOR=${LOCAL_TOOLS}/${DIR_AGGREGATOR}/cbiAggr
ECLIPSE=${LOCAL_TOOLS}/eclipse/eclipse

# ENRICH POMS tool:
ENRICH_POMS_JAR=${WORKING_ROOT}/work/EnrichPoms.jar
ENRICH_POMS_PACKAGE=org.eclipse.platform.releng.maven.pom

# AGGREGATION MODEL:
FILE_SDK_AGGR="${BASE_DIR}/SDK4Mvn.aggr"

#================================================================================
# Util functions
#================================================================================
function require_executable() {
	if [ -x ${1} ]
	then
		echo "Successfully installed: ${1}"
	else
		echo "not executable: ${1}"
		ls -l ${1}
		exit 1
	fi
}

#================================================================================
#   (1) Install and run the CBI aggregator
#================================================================================
echo "==== CBI aggregator ===="

# Set whether this is a snapshot build or not
snapshot="false"
for arg in "$@"; do
	echo $arg
	if [ "$arg" = "-snapshot" ]; then
		snapshot="true"
	fi
done
sed -e "s/snapshot=\".*\"/snapshot=\"${snapshot}\"/g" -i ${FILE_SDK_AGGR} 


if [ ! -d ${LOCAL_TOOLS} ]
then
	mkdir ${LOCAL_TOOLS}
fi

if [ ! -x ${ECLIPSE} ]
then
	pushd ${LOCAL_TOOLS}
	echo "Extracting Eclipse from ${FILE_ECLIPSE} ..."
	curl -L ${FILE_ECLIPSE} | tar -xzf -
	popd
fi
require_executable ${ECLIPSE}

if [ ! -x ${AGGREGATOR} ]
then
	echo "Installing the CBI aggregator into ${LOCAL_TOOLS}/${DIR_AGGREGATOR} ..."
	${ECLIPSE} --launcher.suppressErrors -noSplash \
		-application org.eclipse.equinox.p2.director \
		-r ${URL_AGG_UPDATES} \
		-d ${LOCAL_TOOLS}/${DIR_AGGREGATOR} -p CBIProfile \
		-installIU org.eclipse.cbi.p2repo.cli.product
fi
require_executable ${AGGREGATOR}

RepoRaw="${WORKING_ROOT}/repo-raw"
Repo="${WORKING_ROOT}/repo"
mkdir ${RepoRaw}

echo "Running the aggregator with build model ${FILE_SDK_AGGR} ..."
${AGGREGATOR} aggregate --buildModel ${FILE_SDK_AGGR} \
  --action CLEAN_BUILD --buildRoot ${RepoRaw} \
  -vmargs -Dorg.eclipse.ecf.provider.filetransfer.excludeContributors=org.eclipse.ecf.provider.filetransfer.httpclientjava

mv ${RepoRaw}/final ${Repo}
rm -rf ${RepoRaw}

#================================================================================
#   (2) Enrich POMs
#================================================================================
# Add some required information to the generated poms:
# - dynamic content (retrieved mostly from MANIFEST.MF):
#   - name
#   - url
#   - scm connection, tag and url
# - semi dynamic
#   - developers (based on static map git-repo-base -> project leads)
# - static content
#   - license
#   - organization
#   - issue management


echo "==== Enrich POMs ===="

# build the jar:
mkdir -p ${WORKING_ROOT}/work/bin
javac -d "${WORKING_ROOT}/work/bin" $(find "${BASE_DIR}/src" -name \*.java)
pushd "${WORKING_ROOT}/work/bin"
jar --create --verbose --main-class=${ENRICH_POMS_PACKAGE}.EnrichPoms --file=${ENRICH_POMS_JAR} $(find * -name \*.class)
popd
ls -l ${ENRICH_POMS_JAR}

for project in {platform,jdt,pde}; do
  echo "${project}"
  java -jar ${ENRICH_POMS_JAR} "${Repo}/org/eclipse/${project}"
done

echo "==== Add Javadoc stubs ===="

# (groupSimpleName, javadocArtifactGA)
function createJavadocs() {
	group=${1}
	jar="${1}-javadoc.jar"
	artifact=${2}
	if [ -r ${jar} ]
	then
		rm ${jar}
	fi
	echo -e "Corresponding javadoc can be found in artifact ${artifact}\n" > README.txt
	jar cf ${jar} README.txt
	for pom in org/eclipse/${group}/*/*/*.pom; do
		pomFolder=$(dirname ${pom})
		if [[ ! $pomFolder =~ ${EXCLUDED_ARTIFACTS} ]]; then
			javadoc=`echo ${pom} | sed -e "s|\(.*\)\.pom|\1-javadoc.jar|"`
			cp ${jar} ${javadoc}
		fi
	done	
}

pushd ${Repo}
createJavadocs platform org.eclipse.platform:org.eclipse.platform.doc.isv
createJavadocs jdt org.eclipse.jdt:org.eclipse.jdt.doc.isv
createJavadocs pde org.eclipse.pde:org.eclipse.pde.doc.user
popd

echo "========== Repo completed ========="
