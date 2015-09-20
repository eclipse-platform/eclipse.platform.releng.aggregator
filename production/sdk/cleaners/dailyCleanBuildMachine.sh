#!/usr/bin/env bash

# Utility to clean build machine

function removeOldPromotionScripts ()
{
  ## remove old promotion scripts
  find /shared/eclipse/sdk/promotion/queue -name "RAN*" -ctime +4 -ls -exec rm '{}' \;
  find /shared/eclipse/sdk/promotion/queue -name "TEST*" -ctime +1 -ls -exec rm '{}' \;
  find /shared/eclipse/sdk/promotion/queue -name "ERROR*" -ctime +4 -ls -exec rm '{}' \;
  find /shared/eclipse/equinox/promotion/queue -name "RAN*" -ctime +4 -ls -exec rm '{}' \;
  find /shared/eclipse/equinox/promotion/queue -name "TEST*" -ctime +1 -ls -exec rm '{}' \;
  find /shared/eclipse/equinox/promotion/queue -name "ERROR*" -ctime +4 -ls -exec rm '{}' \;
}

function removeOldDirectories ()
{
  rootdir=$1
  ctimeAge=$2
  pattern=$3
  echo -e "\n\tCleaning rootdir: ${rootdir}"
  if [[ -e "${rootdir}" ]] 
  then
    # leave at least one, on build machine.
    # TODO: this may work most of the time, since ran daily, but the correct 
    # fix is work with the list. This will delete all, on some occasions.
    # Also, for build machine, it is really only I and M builds that we want to leave one in place.
    count=$( find "${rootdir}" -maxdepth 1 -type d -ctime ${ctimeAge} -name "${pattern}"  | wc -l )
    if [[ $count -gt 1 ]]
    then
      find "${rootdir}" -maxdepth 1 -type d -ctime ${ctimeAge} -name "${pattern}" -ls -exec rm -fr '{}' \;
    fi
  else 
    echo -e "\t\tINFO: rootdir did not exist."
  fi
}

function removeBuildFamily ()
{
  basedir="/shared/eclipse/${buildmachine}/${major}${buildType}/siteDir"
  chkdir="eclipse/downloads/drops4"
  removeOldDirectories "${basedir}/${chkdir}" "${days}" "${buildType}20*" 
  chkdir="equinox/drops"
  removeOldDirectories "${basedir}/${chkdir}" "${days}" "${buildType}20*" 
  chkdir="updates/${major}.${minor}-${buildType}-builds"
  removeOldDirectories "${basedir}/${chkdir}" "${days}" "${buildType}20*" 
}

function cleanBuildMachine ()
{
  echo -e "\n\tDaily clean of ${HOSTNAME} build machine on $(date )\n"
  echo -e "\tRemember to "turn off" when M build or I build needs to be deferred promoted,"
  echo -e "\tsuch as for "quiet week".\n"

  INUSE_BEFORE=$(nice -12 du /shared/eclipse/builds -sh)

  buildmachine=$1
  major=4

  minor=6
  days="+4"
  buildType=P
  removeBuildFamily

  minor=6
  days="+2"
  buildType=N
  removeBuildFamily

  minor=6
  days="+4"
  buildType=I
  removeBuildFamily

  minor=5
  days="+4"
  buildType=M
  removeBuildFamily

  INUSE_AFTER=$(nice -12 du /shared/eclipse/builds -sh)

  echo -e "\n\tDisk used before cleaning: $INUSE_BEFORE"
  echo -e "\tDisk used after cleaning: $INUSE_AFTER"
}

cleanBuildMachine builds


