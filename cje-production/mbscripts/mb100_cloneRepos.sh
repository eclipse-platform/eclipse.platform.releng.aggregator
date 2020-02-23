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

source $CJE_ROOT/scripts/common-functions.shsource
source $1

git clone -b $BRANCH --recursive $GIT_ROOT$AGG_REPO ../$AGG_DIR
git checkout $BRANCH
git pull
pushd ../$AGG_DIR
git submodule foreach 'git fetch; SUBMODULE_BRANCH=$(grep $name: ../../../streams/repositories_$PATCH_OR_BRANCH_LABEL.txt | cut -f2 -d\ ); SUBMODULE_BRANCH=${SUBMODULE_BRANCH:-$BRANCH}; echo Checking out $SUBMODULE_BRANCH; git checkout $SUBMODULE_BRANCH; git pull'
popd

pushd "../$AGG_DIR"
adds=$( git submodule | grep "^+" | cut -f2 -d" " )
if [ -z "$adds" ]; then
	echo No updates for the submodules
else
	echo git add $adds
	git add $adds
fi
popd
