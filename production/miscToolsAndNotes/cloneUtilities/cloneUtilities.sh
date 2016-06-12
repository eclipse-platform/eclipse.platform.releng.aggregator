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
#   wget http://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/plain/production/miscToolsAndNotes/cloneUtilities/choneUtilities.sh
# and then executed 

# This "localBuildProperties" file is not for production runs.
# It is only for local testing, where some key locations or hosts may be
# defined differently.
source localBuildProperties.shsource 2>/dev/null

# put the aggregator repo in a subdirectory of WORKSPACE, utilities, in case we ever need
# a special clean-up method where some things are cleaned but not others.
# Plus, it helps emphasize we are using *this* clone of the aggregator for its utilities, not as an
# aggregation of submodules.

if [[ -e ${WORKSPACE}/utilities ]]
then
  echo -e "\n\t[INFO] utilities directory found to exist, so will reset and pull\n"
  pushd ${WORKSPACE}/utilities
  RAW_DATE_START="$(date +%s )"
  # This "little tree" should not be dirty, but in case it is, we "stash --all" and
  # then do a hard reset before doing a pull.
  # IFF we ever wanted to "switch branches" here, then we should also do a fetch before
  # doing the pull, in case the "branch is new".
  echo -e "\n\t[INFO] The tree should not be dirty, but stash all changes and reset hard, just in case.\n"
  git stash save --all
  RC=$?
  if [[ $RC != 0 ]]
  then
    printf "\n\t[ERROR] git stash returned non zero return code: RC: $RC\n"
    exit $RC
  fi
  git reset --hard
  RC=$?
  if [[ $RC != 0 ]]
  then
    printf "\n\t[ERROR] git reset --hard returned non zero return code: RC: $RC\n"
    exit $RC
  fi
  echo -e "\n\t[INFO] pull utilities to get any recent changes.\n"
  git pull
  RC=$?
  if [[ $RC != 0 ]]
  then
    printf "\n\t[ERROR] git reset --hard returned non zero return code: RC: $RC\n"
    exit $RC
  fi
  RAW_DATE_END="$(date +%s )"
  echo -e "\n\t[INFO] Elapsed seconds to pull: $(($RAW_DATE_END - $RAW_DATE_START))"
  popd
else
  echo -e "\n\t[INFO] utilities directory found NOT to exist, so will clone a shallow copy.\n"
  # We do the clone ourselves (instead of letting Hudson) since I could find no way to specify
  # options, such as "depth=1" and --config.
  # we measure the 'time' of this "depth=1" clone, to make sure it is not excessive,
  # compared to "wget" the few files needed. (first time, just took 1 second, which is
  # hard to argue with!)
  RAW_DATE_START="$(date +%s )"
  git clone --depth=1 --config="core.autocrlf=input" file:///gitroot/platform/eclipse.platform.releng.aggregator.git ${WORKSPACE}/utilities
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
echo -e "\n\t[INFO] Make sure sh files are executable. If any "changes" are seen in log, they should be gixed in Git\n"
find utilities -name "*.sh" -exec chmod -c +x '{}' \;


