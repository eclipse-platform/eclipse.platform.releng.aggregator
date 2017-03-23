#!/usr/bin/env bash
#*******************************************************************************
# Copyright (c) 2016 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     David Williams - initial API and implementation
#*******************************************************************************

# Assuming this is ran before "promote", so data is read and written to build machine,
# and then will be promoted with rest of build.

echo -e "\n\n${0##*/} starting"
source localBuildProperties.shsource 2>/dev/null

JAVA_8_HOME=/shared/common/jdk1.8.0_x64-latest
export JAVA_HOME=${JAVA_8_HOME}

# BUILD_ID is normally provided as an environment variable, but
# can provide a default here (especially useful for local testing).
# But best to leave commented out if not doing testing script since 
# otherwise may miss finding script errors.
#buildIdToTest=${BUILD_ID:-"I20160314-2000"}
buildIdToTest=${BUILD_ID}


echo -e "\tbuildIdToTest: ${BUILD_ID}"
# default is "latest release" though that typically only applies to M-builds.
# so does not do much good to specify it here.
# TODO: we could have a "previous_release" sort of variable that 
# would be defined in parent pom or build_eclipse_org.shsource so that
# we do not need to change this source. 
buildIdToCompare="4.6/R-4.6.3-201703010400"

build_type=${buildIdToTest:0:1}
echo -e "\tbuild_type: ${build_type}"
build_dir_root="${BUILD_HOME}/4${build_type}/siteDir/eclipse/downloads/drops4"

build_update_root="${BUILD_HOME}/4${build_type}/siteDir/updates"
dl_dir_root="/home/data/httpd/download.eclipse.org/eclipse/downloads/drops4"
repo_root="/home/data/httpd/download.eclipse.org/eclipse/updates"

function latestSimpleRepo
{
  if [[ $# != 2 ]]
  then
    echo "\n\t[ERROR] Program error. ${0##*/} requires parent directory of simple repositories and name pattern of repo required, such as M20*."
    exit 9
  fi
  parentDir=$1
  namePattern=$2
  latestDLrepo=$(find ${parentDir} -maxdepth 1 -name ${namePattern} -type d | sort | tail -1)
  # we want to return only the "last part" of the directory name, since 
  # rest of code is expecting that. Rest of code could be modified so it took
  # "whole path", but that change should be done under separate bug, if/when desired.
  latestDLrepoSegment=${latestDLrepo##*/}
  # this echo is our "return" value, so can not echo anything else
  echo ${latestDLrepoSegment}
}

if [[ ${build_type} == "N" ]]
then
  update_dir_segment="4.7-N-builds"
  # Note: I am not sure all N-build comparisons are meaninful.
  #       we may want a way to "skip" those comparisons.
  latest_M_build=$(latestSimpleRepo "${repo_root}/4.6-M-builds" "M20*")
  RC=$?
  if [[ $RC != 0 ]]
  then
    exit $RC
  fi
  echo -e "\tlatest_M_build: $latest_M_build"
  buildIdToCompare="4.6-M-builds/${latest_M_build}"
elif [[ ${build_type} == "M" ]]
then
  update_dir_segment="4.6-M-builds"
  buildIdToCompare="4.6/R-4.6.3-201703010400"
  echo -e "\tlatest_R_build: R-4.6.3-201703010400"
elif [[ ${build_type} == "I" ]]
then
  update_dir_segment="4.7-I-builds"
  # We use a function that gets the "latest" simple repo under
  # 4.6-M-builds and use that automatically so each I-build automatically is
  # compared to the latest M-build, instead of having to manually update this value
  # TODO: But, the "4.6" part has to be updated every year. There is probably
  # some other variable to "infer" that from so this script never has to change.
  latest_M_build=$(latestSimpleRepo "${repo_root}/4.6-M-builds" "M20*")
  RC=$?
  if [[ $RC != 0 ]]
  then
    exit $RC
  fi
  echo -e "\tlatest_M_build: $latest_M_build"
  buildIdToCompare="4.6-M-builds/${latest_M_build}"
elif [[ ${build_type} == "Y" ]]
then
  update_dir_segment="4.7-Y-builds"
  # Note: we use same value for Y-builds as for I-builds, since conceptually
  # they are the same, except that Y-builds use some code from BETA_JAVA9 branch.
  latest_M_build=$(latestSimpleRepo "${repo_root}/4.6-M-builds" "M20*")
  RC=$?
  if [[ $RC != 0 ]]
  then
    exit $RC
  fi
  echo -e "\tlatest_M_build: $latest_M_build"
  buildIdToCompare="4.6-M-builds/${latest_M_build}"
else
  echo -e "\nERROR: Unhandled build type: ${build_type} so update_dir_segment undefined: $update_dir_segment"
  echo -e "\n\tand repo reports not produced."
  #TODO: we *might* want to do an 'exit 1' here (or similar) but we may also simply have a releng tests
  #      that "fails" if the reports do not exists.
fi
echo -e "\tbuildIdToCompare: ${buildIdToCompare}"
if [[ -n "$update_dir_segment" ]]
then
  buildToTest="${build_update_root}/${update_dir_segment}/${buildIdToTest}"

  buildToCompare="${repo_root}/${buildIdToCompare}"

  app_area="${build_dir_root}/${buildIdToTest}"

  output_dir="${build_dir_root}/${buildIdToTest}/buildlogs"
  #remove and re-create in case 'reports' exists from a previous run (eventually will not be needed)
  if [[ -e ${output_dir}/reporeports ]]
  then
    rm -rf ${output_dir}/reporeports
  fi
  # just in case does not exist yet
  mkdir -p ${output_dir}

  # This analyzersBuildId can (currently) be found by "drilling down" at 
  # http://download.eclipse.org/cbi/updates/analyzers/4.6/
  # analyzersBuildId=I20161201-1633
  # We use analyzer from hipp this way we have one less version to track
  tar_name=org.eclipse.cbi.p2repo.analyzers.product-linux.gtk.x86_64.tar.gz

  report_app_area="${app_area}/reportApplication"

  if [[ -z "${TMP_DIR}" ]]
  then
    echo -e "\n\tERROR: TMP_DIR surprisingly not defined.\n"
    echo -e "\n\t\tThus stopping create report, but exiting with 0\n"
    exit 0
  fi
  if [[ ! -d "${TMP_DIR}" ]]
  then
    echo -e "\n\tERROR: TMP_DIR surprisingly did not exist  at ${TMP_DIR}.\n"
    echo -e "\n\t\tSo, creating ${TMP_DIR}\n"
    mkdir -p ${TMP_DIR}
  fi

  # turned off proxy for now. Ideally would set proper environment variables!
  # Let's fetch always. otherwise we'll miss any upgrades
  wget --no-proxy --no-verbose --no-cache -O "${TMP_DIR}/${tar_name}" https://hudson.eclipse.org/cbi/job/cbi.p2repo.analyzers_cleanAndDeploy/lastSuccessfulBuild/artifact/output/products/${tar_name} 2>&1
  
  # always extract anew each time.
  if [[ -e ${report_app_area} ]]
  then
    rm -fr ${report_app_area}
  fi
  mkdir -p ${report_app_area}

  echo -e "\n\tExtracting: ${TMP_DIR}/${tar_name}"

  tar -xf "${TMP_DIR}/${tar_name}" -C ${report_app_area}

  # first do the "old" (normal) case
  ${report_app_area}/p2analyze/p2analyze -data ${output_dir}/workspace-report -vm ${JAVA_8_HOME}/bin -vmargs -Xmx1g \
    -DreportRepoDir=${buildToTest} \
    -DreportOutputDir=${output_dir} \
    -DreferenceRepo=${buildToCompare}

  # now run with "new api" which produced the color coded (experimental) reports
  ${report_app_area}/p2analyze/p2analyze -data ${output_dir}/workspace-report -vm ${JAVA_8_HOME}/bin -vmargs -Xmx2g \
    -DuseNewApi=true \
    -DreportRepoDir=${buildToTest} \
    -DreportOutputDir=${output_dir} \
    -DreferenceRepo=${buildToCompare}
fi
