#!/usr/bin/env bash

#*******************************************************************************
# Copyright (c) 2015 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     IBM Corporation - initial API and implementation
#*******************************************************************************

function writeHeader ()
{
  compositeRepoDir="$1"
  if [[ -z "${compositeRepoDir}" ]]
  then
    echo -e "\n\tWARNING: compositeRepoDir not passed to writeHeader function as expected. But will continue with variablei for later use?"
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

  for repo in "${reposToRemove[@]}" 
  do
    echo "        <repository location=\"$repo\" />" >> $antBuildFile
  done

}

function writeClosing ()
{
  echo -e "      </remove>" >> $antBuildFile
  echo -e "    </p2.composite.repository>" >> $antBuildFile
  echo -e "  </target>" >> $antBuildFile
  echo -e "</project>" >> $antBuildFile
}

function generateCleanupXML ()
{
  mainRepoDir=$1
  if [[ -z "${mainRepoDir}" ]]
  then 
    echo -e "/n/tERROR: main repo to work with was not defined"
  else
    writeHeader $mainRepoDir
    writeReposToRemove 
    writeClosing
  fi
}

function getReposToRemove ()
{
  cDir="$1"
  buildType="N*"
  if [[ ! -e "${cDir}" ]]
  then
    echo -e "\n\tiERROR: expected directory did not exist" >&2
    echo -e "\t\t${cDir}" >&2
    reposToRemove=()
  else
    # for "repo names" we want only the last segment of the directory, so use -printf %f. The %C@ is seconds since the beginning of time, for sorting.
    # Some caution is needed here. Seems on eclipse.org "atime" is the one that reflects "when created", 
    # whereas ctime and mtime are all identical, in every directory?! Turns out, mine is that 
    # say too. Apparently p2 "touches" every directory, for some reason. Perhaps only in the "atomic" case? 
    sortedallOldRepos=( $(find ${cDir} -maxdepth 1 -type d -atime +3 -name "${buildType}" -printf "%C@ %f\n" | sort -n | cut -d\  -f2 ) )
    nOldRepos=${#sortedallOldRepos[@]}
    # all builds "find" command should match above, except for age related (and printf) arguments
    nbuilds=$( find ${cDir} -maxdepth 1 -type d -name "${buildType}" | wc -l )
    echo -e "\tNumber of repos before cleaning: $nbuilds"
    echo -e "\tNumber of old repos ${nOldRepos}"
    echo -e "\tDEBUG contents of sortedallOldRepos array"
    for item in "${sortedallOldRepos[@]}"
    do
      echo -e "\t${item}"
    done
    totalMinusOld=$(( nbuilds - nOldRepos ))
    echo -e "\tDEBUG: total minus old: $totalMinusOld"
    if [[ $totalMinusOld -gt 4 ]]
    then
      echo -e "\tDEBUG: number of repos remaining, if all were removed, is greater than 4, so can remove all of the old ones"
      #can remove all old ones
      reposToRemove=("${sortedallOldRepos[@]}")
    else
      # removee all old ones, except for 4
      nToRemove=$(( nbuilds - 4 ))
      echo -e "\tDEBUG: nToRemove: $nToRemove"
      #remove all except newest 4 (if more than 4)
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

function cleanNightlyRepo ()
{
  dryRun=$1
  # Will use the convenient eclipse known to be installed in /shared/simrel
baseBuilder=/shared/simrel/tools/eclipse45/eclipse
eclipseexe=${baseBuilder}/eclipse
if [[ ! -x ${eclipseexe} ]]
then 
  echo -e "\n\tERROR: expected eclipse location not found, or not executable"
  echo -e "\t${eclipseexe}"
  exit 1
fi
JAVA_8_HOME=/shared/common/jdk1.8.0_x64-latest
export JAVA_HOME=${JAVA_8_HOME}
javaexe=${JAVA_HOME}/jre/bin/java
if [[ ! -x ${javaexe} ]]
then 
  echo -e "\n\tERROR: expected java location not found, or not executable"
  echo -e "\t${javaexe}"
  exit 1
fi

antRunner=org.eclipse.ant.core.antRunner
devWorkspace=/shared/eclipse/sdk/cleaners/workspace-cleanup
  echo -e "\tDEBUG: Cleaning repository ${eclipseRepo} on $HOSTNAME on $(date ) " >&2
  getReposToRemove "${eclipseRepo}"
  generateCleanupXML "${eclipseRepo}" 
  if [[ -z "${dryRun}" ]]
  then
    $eclipseexe -nosplash --launcher.suppressErrors -data "${devWorkspace}" -application  ${antRunner} -f $antBuildFile -vm ${javaexe}
    RC=$?
  fi
  if [[ $RC == 0 ]] 
  then
    for file in "${reposToRemove[@]}"
    do
      echo -e "\tDEBUG: directories to remove: ${eclipseRepo}/${file}"
      if [[ -z "${dryRun}" ]]
      then
        rm -rf ${eclipseRepo}/${file}
      fi
    done
  fi 
  if [[ -n "${dryRun}" ]]
  then
    cat $antBuildFile
  fi
}

antBuildFile=cleanupRepoScript.xml

remoteBase="/home/data/httpd/download.eclipse.org"

eclipseRepo="${remoteBase}/eclipse/updates/4.6-N-builds"

# global
declare -a reposToRemove=()

#cleanNightlyRepo dryrun 
cleanNightlyRepo 

unset reposToRemove
