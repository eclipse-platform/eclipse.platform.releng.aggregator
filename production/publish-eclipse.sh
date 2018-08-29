#!/usr/bin/env bash
#*******************************************************************************
# Copyright (c) 2016 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     David Williams - initial API and implementation
#*******************************************************************************
#

if [ $# -ne 1 ]; then
  echo USAGE: $0 env_file
  exit 1
fi

if [ ! -r "$1" ]; then
  echo "$1" cannot be read
  echo USAGE: $0 env_file
  exit 1
fi

source "$1"

export SCRIPT_PATH=${SCRIPT_PATH:-$(pwd)}

cd $BUILD_ROOT

source $SCRIPT_PATH/build-functions.shsource


# derived values
gitCache=$( fn-git-cache "$BUILD_ROOT")
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )

if [ -z "$BUILD_ID" ]; then
  BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
fi

buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BUILD_ID" "$STREAM" )
basebuilderDir=$( fn-basebuilder-dir "$BUILD_ROOT" "$BUILD_ID" "$STREAM" )

printf "\n\tINFO: %s\n" "calling getEBuilderForDropDir.sh from publish-eclipse.sh"
$SCRIPT_PATH/getEBuilderForDropDir.sh $buildDirectory $EBUILDER_HASH

fn-checkout-basebuilder "$basebuilderDir"

launcherJar=$( fn-basebuilder-launcher "$basebuilderDir" )

EBuilderDir="$buildDirectory"/eclipse.platform.releng.aggregator/eclipse.platform.releng.tychoeclipsebuilder

# Temporary fork/condition
# if [[ "true" == "${USING_TYCHO_SNAPSHOT}" ]]
# then
  fn-gather-23-compile-logs "$BUILD_ID" "$aggDir" "$buildDirectory"
#else
#  fn-gather-compile-logs "$BUILD_ID" "$aggDir" "$buildDirectory"
# fi
echo -e "\n\n[DEBUG] == critical values in publish-eclipse.sh == "
echo -e "\n[DEBUG] buildDirectory in publish-eclipse.sh: $buildDirectory"
echo -e "\n[DEBUG] BUILD_ID in publish-eclipse.sh: $BUILD_ID"
fn-parse-compile-logs "$BUILD_ID" \
  "${EBuilderDir}/eclipse/helper.xml" \
  "$buildDirectory" "$launcherJar"

fn-summarize-comparator-logs "$BUILD_ID" \
  "${EBuilderDir}/eclipse/buildScripts/eclipse_compare.xml" \
  "$buildDirectory" "$launcherJar"

# As far as I know, "API Tooling" is not very useful for a patch feature, or X or Y build.
if  [[ ! $BUILD_TYPE =~ [PX] ]]
then
   fn-summarize-apitooling "$BUILD_ID" \
     "${EBuilderDir}/eclipse/buildScripts/api-tools-builder.xml" \
     "$buildDirectory" "$launcherJar"
fi
fn-publish-eclipse "$BUILD_TYPE" "$STREAM" "$BUILD_ID" "$aggDir" "$buildDirectory" "$launcherJar"
