#!/bin/bash -x
#*******************************************************************************
# Copyright (c) 2021 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Sravan Kumar Lakkimsetti - initial API and implementation
#*******************************************************************************

DMG="$1"; shift

PRIMARY_BUNDLE_ID="${DMG//-macosx-cocoa-x86_64.dmg/}"

retryCount=3
while [ ${retryCount} -gt 0 ]; do

  RAW_RESPONSE=$(curl -sS --write-out "\n%{http_code}" -X POST -F file=@"${DMG}" -F 'options={"primaryBundleId": "'"${PRIMARY_BUNDLE_ID}"'", "staple": true};type=application/json' https://cbi.eclipse.org/macos/xcrun/notarize || : )

  RESPONSE=$(head -n-1 <<<"${RAW_RESPONSE}")
  STATUS_CODE=$(tail -n1 <<<"${RAW_RESPONSE}")

  if [[ "${STATUS_CODE}" != "200" ]]; then
    echo "Bad response from the server (status=${STATUS_CODE})"
    echo "${RESPONSE}"
    retryCount=$((retryCount - 1))
    if [ $retryCount -eq 0 ]; then
      echo "Notarization failed 3 times. Exiting"
      exit 1
    else
      echo "Retrying..."
      sleep 2
    fi
  fi

  UUID=$(echo "$RESPONSE" | grep -Po '"uuid"\s*:\s*"\K[^"]+' || : )
  STATUS=$(echo "$RESPONSE" | grep -Po '"status"\s*:\s*"\K[^"]+' || : )

  while [[ ${STATUS} == 'IN_PROGRESS' ]]; do
    sleep 1m
    RESPONSE=$(curl -sS "https://cbi.eclipse.org/macos/xcrun/${UUID}/status")
    STATUS=$(echo "$RESPONSE" | grep -Po '"status"\s*:\s*"\K[^"]+' || : )
  done

  if [[ ${STATUS} != 'COMPLETE' ]]; then
    echo "Notarization failed: ${RESPONSE}"
    retryCount=$((retryCount - 1))
    if [ $retryCount -eq 0 ]; then
      echo "Notarization failed 3 times. Exiting"
      exit 1
    else
      echo "Retrying..."
      sleep 2
    fi
  else
    break
  fi

done

rm "${DMG}"

curl -sSJO "https://cbi.eclipse.org/macos/xcrun/${UUID}/download"
