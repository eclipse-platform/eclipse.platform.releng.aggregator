#!/bin/bash -xe
#*******************************************************************************
# Copyright (c) 2026, 2026 Hannes Wellmann and others.
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
#*******************************************************************************

if [ "$#" -ne 1 ]; then
	echo "Expected usage: $0 <buildID>"
	exit 1
fi
buildID=$1

# Work from the directory containing the eclipse and equinox website
cd $(dirname "$0")/..

find eclipse -type f -name "*.json" -delete
find equinox -type f -name "*.json" -delete

# Eclipse website
buildDrop="https://download.eclipse.org/eclipse/downloads/drops4/${buildID}"
curl --fail -o eclipse/overview/data.json https://download.eclipse.org/eclipse/downloads/data.json
curl --fail -o eclipse/build/buildproperties.json "${buildDrop}/buildproperties.json"
curl --fail -o eclipse/build/compilerSummary.json "${buildDrop}/compilerSummary.json"
curl --fail -o eclipse/build/gitLog.json "${buildDrop}/gitLog.json"
curl --fail -o eclipse/build/buildlogs/logFiles.json "${buildDrop}/buildlogs/logFiles.json"

# Download test results
# Requires jq: https://jqlang.org/download/
mkdir -p eclipse/build/testresults
releaseShort=$(jq -r '.releaseShort' eclipse/build/buildproperties.json)
testConfigurations=$(jq -r '.expectedTests[]' eclipse/build/buildproperties.json)
for testConfig in ${testConfigurations//[[:space:]]/ }; do
	filename="ep${releaseShort//./}I-unit-${testConfig}"
	curl --fail -o eclipse/build/testresults/${filename}-summary.json "${buildDrop}/testresults/${filename}-summary.json"
	curl --fail -o eclipse/build/testresults/${filename}.json "${buildDrop}/testresults/${filename}.json"
done

# Equinox website
curl --fail -o equinox/overview/data.json https://download.eclipse.org/equinox/data.json
curl --fail -o equinox/build/buildproperties.json "https://download.eclipse.org/equinox/drops/${buildID}/buildproperties.json"
