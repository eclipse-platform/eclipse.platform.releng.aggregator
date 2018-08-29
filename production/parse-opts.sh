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

export SCRIPT_PATH=${SCRIPT_PATH:-$(pwd)}

set -- $( getopt -l buildArea:,stream:,branch: -o "" --  "$@" )


while [ $# -gt 0 ]; do
  case "$1" in
    "--buildArea")
      buildArea="$2"; shift;;
    "--stream")
      stream="$2"; shift;;
    "--branch")
      branch="$2"; shift;;
  esac
  shift
done

buildArea_branch=$buildArea/$branch
gitCache=$buildArea_branch/gitCache


if [ -r $gitCache/eclipse.platform.releng.aggregator ]; then
  pushd $gitCache/eclipse.platform.releng.aggregator
else
fi
