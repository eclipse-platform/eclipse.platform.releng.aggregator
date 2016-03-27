#!/usr/bin/env bash

# test code, to test individual functions of syncDropLocation.sh
export PROMOTION_SCRIPT_PATH=${PROMOTION_SCRIPT_PATH:-$( dirname $0 )}
echo "PROMOTION_SCRIPT_PATH: ${PROMOTION_SCRIPT_PATH}"
source ${PROMOTION_SCRIPT_PATH}/syncUpdateUtils.shsource

dlToPath=$(dlToPath 4.3.0 I20130227-0112 CBI)
echo "TEST CBI: dlToPath: $dlToPath"

dlFromPath=$(dlFromPath 4.3.0 I20130227-0112 CBI)
echo "TEST CBI: dlFromPath: $dlFromPath"

dropFromBuildDir=$(dropFromBuildDir 4.3.0 I20130227-0112 CBI)
echo "TEST CBI: dropFromBuildDir: $dropFromBuildDir"

updateSiteOnBuildDir=$(updateSiteOnBuildDir "4.3.0" "I20130227-0112" "CBI")
echo "TEST CBI: updateSiteOnBuildDir: $updateSiteOnBuildDir"

updateSiteOnDL=$(updateSiteOnDL "4.3.0" "I20130227-0112" "CBI")
echo "TEST CBI: updateSiteOnDL: $updateSiteOnDL"

dropOnDLServer=$(dropOnDLServer "4.3.0" "I20130227-0112" "CBI")
echo "TEST CBI: dropOnDLServer: $dropOnDLServer"

