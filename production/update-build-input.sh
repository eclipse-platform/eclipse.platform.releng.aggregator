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

source $SCRIPT_PATH/build-functions.shsource

cd $BUILD_ROOT

# derived values
gitCache=$( fn-git-cache "$BUILD_ROOT")
aggDir=$( fn-git-dir "$gitCache" "$AGGREGATOR_REPO" )
# Confirm file exists as expected
if [[ ! -e "$STREAMS_PATH/repositories_${PATCH_OR_BRANCH_LABEL}.txt" ]]
then 
   echo -e "\n\t[ERROR] repositories file did not exist."
   echo -e "\t[ERROR] expected file: repositories_${PATCH_OR_BRANCH_LABEL}.txt"
   echo -e "\t[ERROR] to be in directory: $STREAMS_PATH\n"
   exit 1
else 
   echo -e "\n\t[INFO] Using repositories file: $STREAMS_PATH/repositories_${PATCH_OR_BRANCH_LABEL}.txt\n"
fi
repositories=$( echo $STREAMS_PATH/repositories_${PATCH_OR_BRANCH_LABEL}.txt )
repoScript=$( echo $SCRIPT_PATH/git-submodule-checkout.sh )


if [ -z "$BUILD_ID" ]; then
  BUILD_ID=$(fn-build-id "$BUILD_TYPE" )
fi


fn-submodule-checkout "$BUILD_ID" "$aggDir" "$repoScript" "$repositories"
fn-add-submodule-updates "$aggDir"
