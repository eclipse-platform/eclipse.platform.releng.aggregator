#!/bin/bash

#*******************************************************************************
# Copyright (c) 2022 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     IBM Corporation - initial API and implementation
#*******************************************************************************

function writeHeader ()
{
  compositeRepoDir="$1"
  antBuildFile=$2
  if [[ -z "${compositeRepoDir}" ]]
  then
    echo -e "\n\tWARNING: compositeRepoDir not passed to writeHeader function as expected. But will continue with variable for later use?"
    compositeRepoDir="\$\{compositeRepoDir\}"
  fi
  echo -e "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" > $antBuildFile
  echo -e "<project" >> $antBuildFile
  echo -e "  basedir=\".\"" >>$antBuildFile
  echo -e "  default=\"cleanup\">" >>$antBuildFile
  echo -e "  <target name=\"cleanup\">" >>$antBuildFile
  echo -e "    <p2.composite.repository>" >>$antBuildFile
  echo -e "      <repository location=\"file://${compositeRepoDir}\" />" >>$antBuildFile
  echo -e "      <remove>" >> $antBuildFile
}

function writeReposToRemove ()
{
  antBuildFile=$1
  for repo in "${reposToRemove[@]}"
  do
    echo "        <repository location=\"$repo\" />" >> $antBuildFile
  done

}

function writeClosing ()
{
  antBuildFile=$1
  echo -e "      </remove>" >> $antBuildFile
  echo -e "    </p2.composite.repository>" >> $antBuildFile
  echo -e "  </target>" >> $antBuildFile
  echo -e "</project>" >> $antBuildFile
}

function generateCleanupXML ()
{
  mainRepoDir=$1
  antBuildFile=$2
  if [[ -z "${mainRepoDir}" || ! -e "${mainRepoDir}" ]]
  then
    echo -e "\n\tERROR: main repo to work with was not defined or did not exist"
  else
    writeHeader $mainRepoDir $antBuildFile
    writeReposToRemove $antBuildFile
    writeClosing $antBuildFile
  fi
}

function getReposToRemove ()
{
  cDir="$1"
  buildType=$2
  nRetain=$3
  buildDir=${remoteBase}/eclipse/downloads/drops4

  if [[ ! -e "${cDir}" ]]
  then
    echo -e "\n\tERROR: expected directory did not exist" >&2
    echo -e "\t\t${cDir}" >&2
    reposToRemove=()
    return 1
  else
    echo -e "\n\tDEBUG: working with directory ${cDir}"
    # for "repo names" we want only the last segment of the directory, so use -printf %f. The %C@ is seconds since the beginning of time, for sorting.
    # Some caution is needed here. Seems on eclipse.org "atime" is the one that reflects "when created",
    # whereas ctime and mtime are all identical, in every directory?! Turns out, mine is that
    # say too. Apparently p2 "touches" every directory, for some reason. Perhaps only in the "atomic" case?
    # But, atime can vary from system to system, depending .. some systems do update, when accessed?
    sortedallOldRepos=( $(find ${cDir} -maxdepth 1 -type d  -name "${buildType}*" -printf "%C@ %f\n" | sort | cut -d\  -f2 ) )
    #nOldRepos=${#sortedallOldRepos[@]}
    # all builds "find" command should match above, except for age related (and printf) arguments
    nbuilds=$( find ${cDir} -maxdepth 1 -type d -name "${buildType}*" | wc -l )
    echo -e "\tNumber of repos before cleaning: $nbuilds"
    #echo -e "\tNumber of old repos ${nOldRepos}"
    echo -e "\tDEBUG contents of sortedallOldRepos array"
    for item in "${sortedallOldRepos[@]}"
    do
      echo -e "\t${item}"
    done
    #totalMinusOld=$(( nbuilds - nOldRepos ))
    #echo -e "\tDEBUG: total minus old: $totalMinusOld"

    #remove unstable builds from the list
    if [[ "$buildType" == "I" ]]
    then
      stableBuildRepos=()
      for i in "${sortedallOldRepos[@]}"
      do
        if [[ ! -f ${buildDir}/${i}/buildUnstable ]]
        then
          stableBuildRepos+=(${i})
        fi
      done
      echo "Stable Builds"
      for item in "${stableBuildRepos[@]}"
      do
        echo -e "\t${item}"
      done
     sortedallOldRepos=("${stableBuildRepos[@]}")
     nbuilds=${#sortedallOldRepos[@]}
     stableBuildRepos=()
    fi

    echo -e "\tDEBUG contents of sortedallOldRepos array after removing unstable builds"
    for item in "${sortedallOldRepos[@]}"
    do
      echo -e "\t${item}"
    done

    if [[ $nbuilds -gt $nRetain ]]
    then
      # remove all old ones, except for nRetain
      nToRemove=$(( nbuilds - nRetain ))
      echo -e "\tDEBUG: nToRemove: $nToRemove"
      #remove all except newest nRetain (if more than nRetain)
      if [[ ${nToRemove} -gt 0 ]]
      then
        echo -e "\tDEBUG: number of old repos to remove found to be ${nToRemove}"
        reposToRemove=("${sortedallOldRepos[@]:0:$nToRemove}")
      else
        echo -e "\tDEBUG: number of old repos to remove found to be ${nToRemove} so we will remove none"
        reposToRemove=()
      fi
    fi
  fi
}

function cleanRepo ()
{
  eclipseRepo=$1
  buildType=$2
  nRetain=$3
  dryRun=$4
  # Changed to "hard coded" location of where to expect on Hudson.
  eclipseexe=${workspace}/eclipse/eclipse
  if [[ ! -x ${eclipseexe} ]]
  then
    echo -e "\n\tERROR: expected eclipse location not found, or not executable"
    echo -e "\t${eclipseexe}"
    exit 1
  fi
  javaexe=${JAVA_HOME}/bin/java
  if [[ ! -x ${javaexe} ]]
  then
    echo -e "\n\tERROR: expected java location not found, or not executable"
    echo -e "\t${javaexe}"
    exit 1
  fi

  antBuildFile=${workspace}/cleanupRepoScript${buildType}.xml
  antRunner=org.eclipse.ant.core.antRunner

  # To allow this cron job to work from hudson, or traditional crontab
  devWorkspace=${workspace}/workspace-cleanup

  echo -e "\tDEBUG: Cleaning repository ${eclipseRepo} on $HOSTNAME on $(TZ="America/New_York" date ) " >&2
  getReposToRemove "${eclipseRepo}" $buildType $nRetain
  RC=$?
  if [[ $RC == 0 ]]
  then
    # be sure there are some to remove
    nToRemove=${#reposToRemove[@]}
    if [[ $nToRemove == 0 ]]
    then
      echo -e "\tfound no files to remove for current repo"
    else
      echo -e "\n\tfound $nToRemove so generating ant file"
      generateCleanupXML "${eclipseRepo}" $antBuildFile
      if [[ -z "${dryRun}" ]]
      then
        $eclipseexe -nosplash --launcher.suppressErrors -data "${devWorkspace}" -application  ${antRunner} -f $antBuildFile -vm ${javaexe}
        RC=$?
      fi
      if [[ $RC == 0 ]]
      then
        # we only clean N-build directories, others need to be manually cleaned
        # after every milestone, or after every release
        if [[ $buildType == "N" ]]
        then
          for file in "${reposToRemove[@]}"
          do
            echo -e "\tDEBUG: directories to remove: ${eclipseRepo}/${file}"
            if [[ -z "${dryRun}" ]]
            then
              rm -rf ${eclipseRepo}/${file}
            fi
          done
          else
            echo -e "\n\tReminder: only composite cleaned. For $buildType builds must cleanup simple repos ever milestone or release".
        fi
      fi
      if [[ -n "${dryRun}" ]]
      then
        echo "Since dryrun printing $antBuildFile"
        cat $antBuildFile
      fi
    fi
  fi
}


workspace=$1
remoteBase="/home/data/httpd/download.eclipse.org"

eclipseIRepo="${remoteBase}/eclipse/updates/4.36-I-builds"
eclipseYRepo="${remoteBase}/eclipse/updates/4.36-Y-builds"
eclipsePRepo="${remoteBase}/eclipse/updates/4.36-P-builds"
eclipseBuildTools="${remoteBase}/eclipse/updates/buildtools"

doDryrun=
# global
declare -a reposToRemove=()
cleanRepo $eclipseIRepo I 2 $doDryrun
declare -a reposToRemove=()
cleanRepo $eclipseYRepo Y 2 $doDryrun
declare -a reposToRemove=()
cleanRepo $eclipsePRepo P 2 $doDryrun
declare -a reposToRemove=()
cleanRepo $eclipseBuildTools I 2 $doDryrun

unset reposToRemove
