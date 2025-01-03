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

echo "========== Repo created: =========="
du -sc ${Repo}/*
du -sc ${Repo}/org/*
du -sc ${Repo}/org/eclipse/*
echo "==================================="


#================================================================================
#   (2) Remove irrelevant stuff
#================================================================================
# Removes from the build output of cbiAggregator everything that is not relevant for maven.
# All removed directories / files will be logged to .logs/removed.txt

echo "==== Remove irrelevant stuff ===="

pushd ${Repo}

if [ ! -d .logs ]
then
	mkdir .logs
elif [ -f .logs/removed.txt ]
then
	rm .logs/removed.txt
fi

#==== remove the p2 repository (not logged): ====

rm -rf p2.index p2.packed content.jar artifacts.jar

#==== remove features: ====

echo "== Features: ==" | tee >> .logs/removed.txt

find * -type d -name \*feature.group -print -exec rm -rf {} \; -prune >> .logs/removed.txt
find * -type d -name \*feature.jar -print -exec rm -rf {} \; -prune >> .logs/removed.txt

#==== remove eclipse test plug-ins: ====

echo "== Test plugins: ==" | tee >> .logs/removed.txt

ls -d org/eclipse/*/*.test* >> .logs/removed.txt
rm -r org/eclipse/*/*.test*

#==== remove other non-artifacts: ====

echo "== Other non-artifacts: ==" | tee >> .logs/removed.txt

find tooling -type d >> .logs/removed.txt
rm -r tooling*

# ... folders that contain only 1.2.3/foo-1.2.3.pom but no corresponding 1.2.3/foo-1.2.3.jar:
function hasPomButNoJar() {
		pushd ${1}
		# expect only one sub-directory, starting with a digit, plus maven-metadata.xml*:
		other=`ls -d [!0-9]* 2> /dev/null`
        if echo "${other}" | tr "\n" " " | egrep "^maven-metadata.xml maven-metadata.xml.md5 maven-metadata.xml.sha1 \$"
        then
        	: # clean -> proceed below
        else
        	exit 1 # unexpected content found, don't remove
        fi
        # scan all *.pom inside the version sub-directory
        r=1
        for pom in `ls [0-9]*/*.pom 2> /dev/null`
        do
                jar=`echo ${pom} | sed -e "s|\(.*\)\.pom|\1.jar|"`
                if [ -f ${jar} ]
                then
                		# jar found, so keep it
                        exit 1
                fi
                # pom without jar found, let's answer true below
                r=0
        done
        popd
        exit $r
}
export -f hasPomButNoJar

find org/eclipse/{jdt,pde,platform} -type d \
	-exec bash -c 'hasPomButNoJar "$@"' bash {} \; \
	-print -exec rm -rf {} \; -prune >> .logs/removed.txt
# second "bash" is used as $0 in the function

popd

echo "========== Repo reduced: =========="
du -sc ${Repo}/*
du -sc ${Repo}/org/*
du -sc ${Repo}/org/eclipse/*
echo "==================================="

#================================================================================
#   (3) Garbage Collector
#================================================================================
# Removes from the build output of cbiAggregator everything that is not referenced 
# from any pom below org/eclipse/{platform,jdt,pde}
#
# Log output:
#  .logs/removedGarbage.txt	all directories during garbage collection 
#  .logs/gc.log 			incoming dependencies of retained artifacts
#  .logs/empty-dirs.txt		removed empty directories 

echo "==== Garbage Collector ===="

pushd ${Repo}

#==== function gc_bundle(): ====
# Test if pom ${1} is referenced in any other pom.
# If not, append the containing directory to the file "toremove.txt"
function gc_bundle {
        AID=`echo ${1} | sed -e "s|.*/\(.*\)[_-].*|\1|"`
        DIR=`echo ${1} | sed -e "s|\(.*\)/[0-9].*|\1|"`
        POM=`basename ${1}`

        ANSWER=`find org/eclipse/{platform,jdt,pde} -name \*.pom \! -name ${POM} \
        		 -exec grep -q "<artifactId>${AID}</artifactId>" {} \; -print -quit`

        if [ "$ANSWER" == "" ]
        then
                echo "Will remove $DIR"
                echo $DIR >> toremove.txt
        else
                echo "$1 is used by $ANSWER"
        fi
}
export -f gc_bundle

#==== run the garbage collector: ====
# iterate (max 5 times) in case artifacts were used only from garbage:
for iteration in 1 2 3 4 5
do
	echo "== GC iteration ${iteration} =="

	# look for garbage only outside platform, jdt or pde folders:
	find -name platform -prune -o -name jdt -prune -o -name pde -prune -o \
		 -name \*.pom -exec bash -c 'gc_bundle "$@"' bash {} \; \
		 > gc-${iteration}.log
	# second "bash" is used as $0 in the function
	
	if [ ! -f toremove.txt ]
	then
		# no more garbage found
		break
	fi
	cat toremove.txt >> .logs/removedGarbage.txt
	for d in `cat toremove.txt`; do rm -r $d; done	
	rm toremove.txt
done

# merge gc logs:
cat gc-*.log | sort --unique > .logs/gc.log
rm gc-*.log

#==== remove all directories that have become empty: ====
for iteration in 1 2 3 4 5 6 ; do find -type d -empty -print \
 	-exec rmdir {} \; -prune; done \
 	>> .logs/empty-dirs.txt

echo "========== Repo reduced: =========="
du -sc ${Repo}/*
du -sc ${Repo}/org/*
du -sc ${Repo}/org/eclipse/*
echo "==================================="

popd

#================================================================================
#   (4) Enrich POMs
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

echo "platform"
java -jar ${ENRICH_POMS_JAR} "${Repo}/org/eclipse/platform"
echo "jdt"
java -jar ${ENRICH_POMS_JAR} "${Repo}/org/eclipse/jdt"
echo "pde"
java -jar ${ENRICH_POMS_JAR} "${Repo}/org/eclipse/pde"


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
	for pom in org/eclipse/${group}/*/*/*.pom
	do
		javadoc=`echo ${pom} | sed -e "s|\(.*\)\.pom|\1-javadoc.jar|"`
		cp ${jar} ${javadoc}
	done	
}

pushd ${Repo}
createJavadocs platform org.eclipse.platform:org.eclipse.platform.doc.isv
createJavadocs jdt org.eclipse.jdt:org.eclipse.jdt.doc.isv
createJavadocs pde org.eclipse.pde:org.eclipse.pde.doc.user
popd

echo "========== Repo completed ========="
