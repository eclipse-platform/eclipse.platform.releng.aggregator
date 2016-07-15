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

# This is a handy utility to use on Hudson that makes a
# shallow clone of the master branch of the
# eclipse.platform.releng.aggregator
# project for the purpose of using the utilities in the
# "production" directory. It clones into a directory named
# 'utilities' to help emphasize we are not using it for the
# main build or making use of its submodules.

# This one file can be retrieved with
#   wget http://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/plain/production/miscToolsAndNotes/cloneUtilities/cloneUtilities.sh
# and then chmod +x cloneUtilities.sh
# and then executed with ./cloneUtilities.sh
# It is best to remove 'cloneUtilities.sh' and wget it each
# time in case it itself changes.

# This "localBuildProperties" file is not for production runs.
# It is only for local testing, where some key locations or hosts may be
# defined differently.
source localBuildProperties.shsource 2>/dev/null


# If running on Hudson, we access the repo directly with file://
# protocol. If not (i.e. if WORKSPACE is not defined) then we use
# git://git.eclipse.org/
export REPO_AND_ACCESS=${REPO_AND_ACCESS:-"file:///gitroot"}
if [[ -z "${WORKSPACE}" ]]
then
  echo -e "\n\t[WARNING] This script is intend to be ran in Hudson."
  echo -e "\t\t But since WORKSPACE was not defined, will define it as PWD for local testing.\n\n"
  export WORKSPACE=${PWD}
  export REPO_AND_ACCESS=${REPO_AND_ACCESS:-"git://git.eclipse.org/gitroot"}
fi

if [[ -e ${WORKSPACE}/utilities ]]
then
  echo -e "\n\t[INFO] utilities directory found to exist, so will fetch and reset\n"
  pushd ${WORKSPACE}/utilities
  RAW_DATE_START="$(date +%s )"
  # This "little tree" should not be dirty, but in case it is, we "stash --all" and
  # then do a fetch and hard reset.
  # Try initial rebase --abort in case starting from a failed previous run.
  printf "\n\t[INFO] Doing rebase --abort in case starting from a failed previous run.\n"
  git rebase --abort
  RC=$?
  if [[ $RC != 0 ]]
  then
    printf "\n\t[INFO] initial git rebase --abort returned non zero return code: RC: $RC\n"
    printf "\n\t       probably due to not needing to do an initial rebase --abort.\n"
  fi

  printf "\n\t[INFO] Doing fetch origin --depth=1 which we'll force to be our new HEAD\n"
  git fetch origin --depth=1
  RC=$?
  if [[ $RC != 0 ]]
  then
    printf "\n\t[ERROR] git fetch origin returned non zero return code: RC: $RC\n"
    exit $RC
  fi
  printf "\n\t[INFO] Now doing reset --hard origin/master forcing our local pointer to what ever was fetched.\n"
  git reset --hard origin/master
  RC=$?
  if [[ $RC != 0 ]]
  then
    printf "\n\t[ERROR] git reset --hard returned non zero return code: RC: $RC\n"
    printf "\n\t[INFO] Doing a rebase --abort to be able to retry to restore clone to pristine.\n"
    git rebase --abort
    RCa=$?
    if [[ $RCa != 0 ]]
    then
      printf "\n\t[ERROR] git rebase --abort returned non zero return code: RCa: $RCa\n"
      exit $RCa
    fi
    exit $RC
  fi

  RAW_DATE_END="$(date +%s )"
  echo -e "\n\t[INFO] Elapsed seconds to pull: $(($RAW_DATE_END - $RAW_DATE_START))\n"
  popd
else
  echo -e "\n\t[INFO] utilities directory found NOT to exist, so will clone a shallow copy.\n"
  # We do the clone ourselves (instead of letting Hudson) since I could find no way to specify
  # options, such as "depth=1" and --config.
  # we measure the 'time' of this "depth=1" clone, to make sure it is not excessive,
  # compared to "wget" the few files needed. (first time, just took 1 second, which is
  # hard to argue with!)
  RAW_DATE_START="$(date +%s )"
  git clone --depth=1 --config="core.autocrlf=input" ${REPO_AND_ACCESS}/platform/eclipse.platform.releng.aggregator.git ${WORKSPACE}/utilities
  RC=$?
  if [[ $RC != 0 ]]
  then
    printf "\n\t[ERROR] git clone returned non zero return code: RC: $RC\n"
    exit $RC
  fi

  RAW_DATE_END="$(date +%s )"
  echo -e "\n\t[INFO] Elapsed seconds to clone (depth 1): $(($RAW_DATE_END - $RAW_DATE_START))"
fi

# just to be sure all are executable
# note: if attributes are changed, that will make working
# tree dirty. In that case, a "reset --hard" will have to
# be done before pull, or better, make sure executable files are
# checked into Git as executable.
echo -e "\n\t[INFO] Make sure sh files are executable. If any "changes" are seen in log, they should be fixed in Git\n"
find utilities -name "*.sh" -exec chmod -c +x '{}' \;
