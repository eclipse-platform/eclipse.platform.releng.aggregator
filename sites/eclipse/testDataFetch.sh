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

# This script Requires jq: https://jqlang.org/download/

# Work from the directory containing the eclipse and equinox website
cd $(dirname "$0")/..

find eclipse -type f -name "*.json" -delete
find equinox -type f -name "*.json" -delete
rm -rf ./*/build/buildUnstable

# Eclipse website
buildDrop="https://download.eclipse.org/eclipse/downloads/drops4/${buildID}"
rm -rf eclipse/build/compilelogs
mkdir -p eclipse/build/compilelogs

curl --fail -o eclipse/overview/data.json https://download.eclipse.org/eclipse/downloads/data.json
curl --fail -o eclipse/build/buildproperties.json "${buildDrop}/buildproperties.json"
curl --fail -o eclipse/build/buildUnstable "${buildDrop}/buildUnstable" || echo 'No buildUnstable file -> Build was stable'
curl --fail -o eclipse/build/compilelogs/logs.json "${buildDrop}/compilelogs/logs.json"
curl --fail -o eclipse/build/gitLog.json "${buildDrop}/gitLog.json"
curl --fail -o eclipse/build/buildlogs/logs.json "${buildDrop}/buildlogs/logs.json"

# Fetch test results
rm -rf eclipse/build/testresults/*.json
releaseShort=$(jq -r '.releaseShort' eclipse/build/buildproperties.json)
testConfigurations=$(jq -r '.expectedTests[]' eclipse/build/buildproperties.json)
for testConfig in ${testConfigurations//[[:space:]]/ }; do
	filename="ep${releaseShort//./}I-unit-${testConfig}"
	(curl --fail -o eclipse/build/testresults/${filename}-summary.json "${buildDrop}/testresults/${filename}-summary.json" || echo "Tests not yet completed: ${testConfig}")&
	(curl --fail -o eclipse/build/testresults/${filename}.json "${buildDrop}/testresults/${filename}.json" || echo "Tests not yet completed: ${testConfig}")&
done

# Fetch compile logs
compileLogs=$(jq -r 'keys[]' eclipse/build/compilelogs/logs.json)
for log in ${compileLogs//[[:space:]]/ }; do
	if [ "${log}" == 'note' ]; then
		continue
	fi
	logPath="compilelogs/${log}"
	mkdir -p $(dirname eclipse/build/${logPath})
	(curl --fail -o eclipse/build/${logPath} "${buildDrop}/${logPath}")&
done

wait

# Equinox website
buildDrop="https://download.eclipse.org/equinox/drops/${buildID}"
curl --fail -o equinox/overview/data.json https://download.eclipse.org/equinox/data.json
curl --fail -o equinox/build/buildproperties.json "${buildDrop}/buildproperties.json"
curl --fail -o equinox/build/buildUnstable "${buildDrop}/buildUnstable" || echo 'No buildUnstable file -> Build was stable'
