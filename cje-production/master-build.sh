#!/bin/bash
#*******************************************************************************
# Copyright (c) 2019 IBM Corporation and others.
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

export CJE_ROOT=${CJE_ROOT:-`pwd`}
source $CJE_ROOT/scripts/common-functions.shsource

unset JAVA_TOOL_OPTIONS 
unset _JAVA_OPTIONS

chmod +x mbscripts/*

logDir=$CJE_ROOT/buildlogs
mkdir -p $logDir

pushd mbscripts
for i in $(ls | sort)
do
  fn-run-command ./$i $CJE_ROOT/buildproperties.shsource 2>&1 | tee $logDir/$i.log
  if [ $? != 0 ];then
  	fn-write-property BUILD_FAILED "${BUILD_FAILED} \n$$CJE_ROOT/$DROP_DIR/$BUILD_ID/buildlogs/$i.log"
  fi
done
popd

wait

source $CJE_ROOT/buildproperties.shsource
cp -r $logDir/* $CJE_ROOT/$DROP_DIR/$BUILD_ID/buildlogs
rm -rf $logDir
cp $CJE_ROOT/buildproperties.txt $CJE_ROOT/$DROP_DIR/$BUILD_ID
mv $CJE_ROOT/buildproperties.php $CJE_ROOT/$DROP_DIR/$BUILD_ID
mv $CJE_ROOT/buildproperties.properties $CJE_ROOT/$DROP_DIR/$BUILD_ID
mv $CJE_ROOT/buildproperties.shsource $CJE_ROOT/$DROP_DIR/$BUILD_ID
cp $CJE_ROOT/$DROP_DIR/$BUILD_ID/buildproperties.* $CJE_ROOT/$EQUINOX_DROP_DIR/$BUILD_ID
