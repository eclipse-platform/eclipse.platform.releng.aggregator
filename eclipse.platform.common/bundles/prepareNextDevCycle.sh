#!/bin/bash -eu
set -o pipefail

#******************************************************************************
# Copyright (c) 2025, 2026 Hannes Wellmann and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Hannes Wellmann - initial API and implementation
#******************************************************************************

# This script is called by the pipeline for preparing the next development cycle (this file's name is crucial!)
# and applies the changes required individually for the common documentation bundles.
# The calling pipeline also defines environment variables usable in this script.

declare -A MIGRATION_GUIDE_DOC_BUNDLES
MIGRATION_GUIDE_DOC_BUNDLES['org.eclipse.platform.doc.isv']=renderPlatformDocTopicEntry
MIGRATION_GUIDE_DOC_BUNDLES['org.eclipse.jdt.doc.isv']=renderJDTDocTopicEntry

TOPIC_LIMIT=8
OLD_TOPIC_LIMIT=4

cd $(dirname "$0")

# Update links to N&N entries
for whatsNewFile in */whatsNew/*_whatsnew.html; do
	sed --in-place "${whatsNewFile}" \
		--expression "s|Eclipse ${PREVIOUS_RELEASE_VERSION}|Eclipse ${NEXT_RELEASE_VERSION}|g" \
		--expression "s|news/${PREVIOUS_RELEASE_VERSION}/|news/${NEXT_RELEASE_VERSION}/|g"
done

# Clear content of all forceQualifierUpdate files in this directory
for file in */forceQualifierUpdate.txt; do
	> "$file"
done

# Create new migration guide file structure
for bundle in ${!MIGRATION_GUIDE_DOC_BUNDLES[@]}; do
	portingDir="${bundle}/porting/${NEXT_RELEASE_VERSION}"
	mkdir -p "${portingDir}"
	for file in ${bundle}/templates/porting/*; do
		filePath="${portingDir}/$(basename ${file})"
		cp -v "${file}" "${filePath}"
		sed --in-place "${filePath}" \
			--expression "s/\${NEXT_RELEASE_VERSION}/${NEXT_RELEASE_VERSION}/g" \
			--expression "s/\${PREVIOUS_RELEASE_VERSION}/${PREVIOUS_RELEASE_VERSION}/g" \
			--expression "s/\${NEXT_RELEASE_YEAR}/${NEXT_RELEASE_YEAR}/g"
	done
	mv -v "${portingDir}/eclipse_porting_guide.html" "${bundle}/porting/eclipse_${NEXT_RELEASE_VERSION//./_}_porting_guide.html"
done


# Update topics TOC

function renderPlatformDocTopicEntry() {
	local nextReleaseVersion="$1"
	local previousReleaseVersion="$2"
	cat <<EOF
${INDENT}<topic label="Migrating to Eclipse ${nextReleaseVersion} from ${previousReleaseVersion}">
${INDENT}	<topic label="Introduction" href="porting/eclipse_${nextReleaseVersion//./_}_porting_guide.html"/>
${INDENT}	<topic label="FAQ" href="porting/${nextReleaseVersion}/faq.html" />
${INDENT}	<topic label="Incompatibilities" href="porting/${nextReleaseVersion}/incompatibilities.html" />
${INDENT}	<topic label="Adopting ${nextReleaseVersion} mechanisms and API" href="porting/${nextReleaseVersion}/recommended.html" />
${INDENT}</topic>
EOF
}

function renderJDTDocTopicEntry() {
	local nextReleaseVersion="$1"
	local previousReleaseVersion="$2"
	cat <<EOF
${INDENT}<topic label="Migrating to Eclipse JDT ${nextReleaseVersion} from ${previousReleaseVersion}">
${INDENT}	<topic label="Introduction" href="porting/eclipse_${nextReleaseVersion//./_}_porting_guide.html"/>
${INDENT}	<topic label="FAQ"                             href="porting/${nextReleaseVersion}/faq.html" />
${INDENT}	<topic label="Incompatibilities"               href="porting/${nextReleaseVersion}/incompatibilities.html" />
${INDENT}	<topic label="Adopting ${nextReleaseVersion} Mechanisms and API" href="porting/${nextReleaseVersion}/recommended.html" />
${INDENT}</topic>
EOF
}

for bundle in ${!MIGRATION_GUIDE_DOC_BUNDLES[@]}; do
	pushd $bundle
		# List entries in descending order
		mapfile -t allVersions < <(find porting -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort -Vr)
		
		echo "Generate ${bundle}/topics_Porting.xml for versions ${allVersions[@]}"
		
		function buildTopicsList() {
			local start=$1
			local len=$2
			for (( i=start; i < (start + len); i++ )); do
				renderTopicEntry=${MIGRATION_GUIDE_DOC_BUNDLES[$bundle]}
				$renderTopicEntry "${allVersions[i]}" "${allVersions[i+1]}"
			done	
		}
		
		INDENT=$'\t'
		topics="$(buildTopicsList 0 $TOPIC_LIMIT)"
		INDENT=$'\t\t'
		olderTopics="$(buildTopicsList $TOPIC_LIMIT $OLD_TOPIC_LIMIT)"
		
		# Replace placeholders with generated lists and create final file
		awk -v tops="$topics" -v oTops="$olderTopics" \
			'{ sub(/<topics\/>/, tops); sub(/<olderTopics\/>/, oTops); print }' \
			'templates/topics_Porting.xml' > 'topics_Porting.xml'
	popd
done

# Delete old entries and files that exceed the specified limit
migrationGuidesLimit="$(( TOPIC_LIMIT + OLD_TOPIC_LIMIT))"
for bundle in ${!MIGRATION_GUIDE_DOC_BUNDLES[@]}; do
	dirsToDelete=$(find ${bundle}/porting/ -mindepth 1 -maxdepth 1 -type d | sort -V | head -n -${migrationGuidesLimit})
	rm -rf -v ${dirsToDelete}
	filesToDelete=$(find ${bundle}/porting/eclipse_*_porting_guide.html -type f | sort -V | head -n -${migrationGuidesLimit})
	rm -rf -v ${filesToDelete}
done


git add .
git commit --message="Prepare doc bundles for ${NEXT_RELEASE_VERSION}

Adds the initial Migration guide for Eclipse ${NEXT_RELEASE_VERSION} from ${PREVIOUS_RELEASE_VERSION}, among others.
"
