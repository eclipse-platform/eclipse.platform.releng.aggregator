#!/bin/bash -x

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
#     Kit Lo - initial API and implementation
#*******************************************************************************

if [ $# -ne 1 ]; then
  echo USAGE: $0 env_file
  exit 1
fi

source $CJE_ROOT/scripts/common-functions.shsource
source $1

ANT_SCRIPT=$ECLIPSE_BUILDER_DIR/eclipse/buildScripts/api-tools-builder.xml

# Change to appropriate versions and uncomment when entering API freeze
#FREEZE_PARAMS="-DfreezeBaseURL=https://$DOWNLOAD_HOST/eclipse/downloads/drops4/S-4.12RC1-yyyymmddhhmm/eclipse-SDK-4.12RC1-win32-x86_64.zip -DfreezeName=Eclipse-SDK-4.12RC1 -DfreezeFilename=eclipse-SDK-4.12RC1-win32-x86_64.zip"
# Otherwise set to a blank space
FREEZE_PARAMS=" "

pushd $CJE_ROOT/$DROP_DIR/$BUILD_ID
java -jar $LAUNCHER_JAR \
  -application org.eclipse.ant.core.antRunner \
  -buildfile $ANT_SCRIPT \
  -data $CJE_ROOT/$TMP_DIR/workspace-apitoolingsLogs \
  -DEBuilderDir=$ECLIPSE_BUILDER_DIR \
  -DbuildDirectory=$CJE_ROOT/$DROP_DIR/$BUILD_ID \
  -DbuildId=$BUILD_ID \
  -DbuildLabel=$BUILD_ID \
  -DbuildWorkingArea=$CJE_ROOT/$AGG_DIR \
  -DpreviousBaseURL=http://$DOWNLOAD_HOST/eclipse/downloads/drops4/$PREVIOUS_RELEASE_ID/eclipse-SDK-$PREVIOUS_RELEASE_VER-win32-x86_64.zip \
  -DpreviousBaselineName=Eclipse-SDK-$PREVIOUS_RELEASE_VER \
  -DpreviousBaselineFilename=eclipse-SDK-$PREVIOUS_RELEASE_VER-win32-x86_64.zip \
  -Djava.io.tmpdir=$CJE_ROOT/$TMP_DIR \
  $FREEZE_PARAMS \
  apiToolsReports
popd
