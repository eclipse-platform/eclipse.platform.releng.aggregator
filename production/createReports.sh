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

source localBuildProperties.shsource 2>/dev/null

JAVA_8_HOME=/shared/common/jdk1.8.0_x64-latest
export JAVA_HOME=${JAVA_8_HOME}
buildIdToTest=${BUILD_ID:-"I20160314-2000"}
buildIdToCompare="4.6/R-4.6-201606061100"
build_type=${buildIdToTest:0:1}
build_dir_root="${BUILD_HOME}/4${build_type}/siteDir/eclipse/downloads/drops4"
build_update_root="${BUILD_HOME}/4${build_type}/siteDir/updates"
dl_dir_root="/home/data/httpd/download.eclipse.org/eclipse/downloads/drops4"
if [[ ${build_type} == "N" ]]
then
  update_dir_segment="4.7-N-builds"
elif [[ ${build_type} == "M" ]]
then
  update_dir_segment="4.6-M-builds"
elif [[ ${build_type} == "I" ]]
then
  update_dir_segment="4.7-I-builds"
elif [[ ${build_type} == "Y" ]] 
then
  update_dir_segment="4.7-Y-builds"
else
  echo -e "\nERROR: Unhandled build type: ${build_type} so update_dir_segment undefined: $update_dir_segment"
fi

if [[ -n "$update_dir_segment" ]]
then
  buildToTest="${build_update_root}/${update_dir_segment}/${buildIdToTest}"

  buildToCompare="/home/data/httpd/download.eclipse.org/eclipse/updates/${buildIdToCompare}"


  app_area="${build_dir_root}/${buildIdToTest}"

  output_dir="${build_dir_root}/${buildIdToTest}/buildlogs"
  #remove and re-create in case 'reports' exists from a previous run (eventually will not be needed)
  if [[ -e ${output_dir}/reporeports ]] 
  then
    rm -rf ${output_dir}/reporeports
  fi
  # just in case does not exist yet
  mkdir -p ${output_dir}

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

  # Let's always refetch for now
  # TODO: turned off proxy for now. Ideally would set proper environment variables!
  # --no-verbose -quiet
  #if [[ ! -F ${TMP_DIR}/${tar_name} ]]
  #then
  wget --no-proxy --no-cache -O "${TMP_DIR}/${tar_name}" https://hudson.eclipse.org/cbi/job/cbi.p2repo.analyzers.build/lastSuccessfulBuild/artifact/output/products/${tar_name} 2>&1
  #else 
  #    echo "${TMP_DIR}/${tar_name} already existed, not re-fetched"
  #fi
  # always extract anew each time.
  if [[ -e ${report_app_area} ]]
  then
    rm -fr ${report_app_area}
  fi
  mkdir -p ${report_app_area}

  echo -e "\n\tExtracting: ${TMP_DIR}/${tar_name}"

  tar -xf "${TMP_DIR}/${tar_name}" -C ${report_app_area}

  ${report_app_area}/p2analyze -data ${output_dir}/workspace-report -vm ${JAVA_8_HOME}/bin -vmargs -Xmx1g \
    -DreportRepoDir=${buildToTest} \
    -DreportOutputDir=${output_dir} \
    -DreferenceRepo=${buildToCompare}

  ${report_app_area}/p2analyze -data ${output_dir}/workspace-report -vm ${JAVA_8_HOME}/bin -vmargs -Xmx2g \
    -DuseNewApi=true \
    -DreportRepoDir=${buildToTest} \
    -DreportOutputDir=${output_dir} \
    -DreferenceRepo=${buildToCompare}
fi
