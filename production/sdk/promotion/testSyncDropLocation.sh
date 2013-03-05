#!/usr/bin/env bash

# test code, to test individual functions of syncDropLocation.sh
SCRIPTDIR=$( dirname $0 )
echo "SCRIPTDIR: ${SCRIPTDIR}"
source ${SCRIPTDIR}/syncUpdateUtils.shsource

# 4.3.0 I20130227-0112 PDE master

dlToPath=$(dlToPath 4.3.0 I20130227-0112 PDE)
 echo "TEST PDE: dlToPath: $dlToPath" 

dlToPath=$(dlToPath 4.3.0 I20130227-0112 CBI)
 echo "TEST CBI: dlToPath: $dlToPath" 
 
dlFromPath=$(dlFromPath 4.3.0 I20130227-0112 PDE)
 echo "TEST PDE: dlFromPath: $dlFromPath" 

dlFromPath=$(dlFromPath 4.3.0 I20130227-0112 CBI)
 echo "TEST CBI: dlFromPath: $dlFromPath" 
 
dropFromBuildDir=$(dropFromBuildDir 4.3.0 I20130227-0112 PDE)
 echo "TEST PDE: dropFromBuildDir: $dropFromBuildDir"

dropFromBuildDir=$(dropFromBuildDir 4.3.0 I20130227-0112 CBI)
 echo "TEST CBI: dropFromBuildDir: $dropFromBuildDir"
 
 updateSiteOnBuildDir=$(updateSiteOnBuildDir "4.3.0" "I20130227-0112" "PDE")
 echo "TEST CBI: updateSiteOnBuildDir: $updateSiteOnBuildDir"
  
 updateSiteOnBuildDir=$(updateSiteOnBuildDir "4.3.0" "I20130227-0112" "CBI")
 echo "TEST CBI: updateSiteOnBuildDir: $updateSiteOnBuildDir"
 
 updateSiteOnDL=$(updateSiteOnDL "4.3.0" "I20130227-0112" "PDE")
 echo "TEST CBI: updateSiteOnDL: $updateSiteOnDL"
  
 updateSiteOnDL=$(updateSiteOnDL "4.3.0" "I20130227-0112" "CBI")
 echo "TEST CBI: updateSiteOnDL: $updateSiteOnDL"

 dropOnDLServer=$(dropOnDLServer "4.3.0" "I20130227-0112" "PDE")
 echo "TEST CBI: dropOnDLServer: $dropOnDLServer"
  
 dropOnDLServer=$(dropOnDLServer "4.3.0" "I20130227-0112" "CBI")
 echo "TEST CBI: dropOnDLServer: $dropOnDLServer"
 