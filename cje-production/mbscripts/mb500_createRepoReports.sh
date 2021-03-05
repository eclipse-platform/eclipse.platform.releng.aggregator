#!/bin/bash

#*******************************************************************************
# Copyright (c) 2020 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Kit Lo - initial API and implementation
#*******************************************************************************
set -e

if [ $# -ne 1 ]; then
  echo USAGE: $0 env_file
  exit 1
fi

wait

source $CJE_ROOT/scripts/common-functions.shsource
source $1

buildToTest=$CJE_ROOT/$UPDATES_DIR/$BUILD_ID
output_dir=$CJE_ROOT/$DROP_DIR/$BUILD_ID/buildlogs
tar_name=org.eclipse.cbi.p2repo.analyzers.product-linux.gtk.x86_64.tar.gz
report_app_dir=$CJE_ROOT/$TMP_DIR/reportApplication

wget --no-proxy --no-verbose --no-cache -O $CJE_ROOT/$TMP_DIR/$tar_name https://download.eclipse.org/cbi/updates/analyzers/4.7/I20191111-1833/org.eclipse.cbi.p2repo.analyzers.product_I20191111-1833_linux.gtk.x86_64.tar.gz

mkdir -p $report_app_dir
tar -xf $CJE_ROOT/$TMP_DIR/$tar_name -C $report_app_dir

$report_app_dir/p2analyze/p2analyze -data $CJE_ROOT/$TMP_DIR/workspace-report -vm $JAVA_HOME/bin -vmargs -Xmx1g \
  -DreferenceRepo=$CJE_ROOT/$TMP_DIR/$BUILD_TO_COMPARE_SITE/$PREVIOUS_RELEASE_VER/$BASEBUILD_ID \
  -DreportRepoDir=$buildToTest \
  -DreportOutputDir=$output_dir
