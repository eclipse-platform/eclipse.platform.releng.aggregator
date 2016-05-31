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
