#!/bin/bash
#*******************************************************************************
# Copyright (c) 2019 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Sravan Lakkimsetti - initial API and implementation
#*******************************************************************************

# Utility to clean build machine
echo -e "\n\tDaily clean of ${HOSTNAME} download server on $(TZ="America/New_York" date )\n"

#
# Checks whether a build can be retained or not.
# returns 0  if the build can be retained
#
function canBeRetained ()
{
  retval=1
  buildName=$1
  grep BUILD_FAILED ${buildName}/buildproperties.shsource >/dev/null 2>&1
  if [ $? -ne 0 ]; then #build didn't fail
    if [ -f "${buildName}/buildUnstable" ]; then  #build is marked unstable so should not be retained
      retval=1
    else
      retval=0
    fi
  fi
  return $retval
}

#
# remove a build
#

function removeBuild ()
{
  buildname=$1
  rm -fr $buildname
  RC=$?
  if [[ $RC = 0 ]];then
    echo -e "Removed: $buildname"
  else
    echo -e "\n\tAn Error occurred removing $buildname. RC: $RC"
  fi
}

cDir="/home/data/httpd/download.eclipse.org/eclipse/downloads/drops4"
buildType="I*"
allOldBuilds=$( find ${cDir} -maxdepth 1 -type d -ctime +5 -name "${buildType}"|sort )
nbuilds=$( find ${cDir} -maxdepth 1 -type d -name "${buildType}" | wc -l )
echo -e "\tNumber of I-builds before cleaning: $nbuilds"

# Make sure we leave at least 4 on DL server, no matter how old
# To avoid 'ls' see http://mywiki.wooledge.org/ParsingLs
# technically, applies to "find" as well.
shopt -s nullglob
count=0
files=(${cDir}/${buildType})
# We count on files being "ordered" by date/timestamp in name
for  ((i=${#files[@]}-1; i>=0; --i)); do
  newest[$count]="${files[$i]}"
  count=$(( count + 1 ))
  if [ $count -gt 3 ]
  then
    break
  fi
done

areNotToDelete=$(printf '%s\n' "${newest[@]}" | paste -sd '|')

currentWeekNum=0 #week number from start of the year user to identify the week in which the build is created
found=0 #it will be 1 when we found a build that can be retained in that week

for buildname in ${allOldBuilds}; do
  if [[ $buildname =~ $areNotToDelete ]]
  then
    echo -e "\tDEBUG: Not removed (since one of 4 newest, even though old): \n\t$buildname"
  else
    buildId=$(basename $buildname) #extract buildId
    yy=$(echo $buildId|cut -b2-5)  #extract year from buildId
    mm=$(echo $buildId|cut -b6-7)  #extract month
    dd=$(echo $buildId|cut -b8-9)  #extract day
    day=${mm}/${dd}/${yy}          #construct build date
    dayOfWeek=$(TZ="America/New_York" date -d $day +%u)  #get the day of the week like monday, tue, etc
    weekNum=$(TZ="America/New_York" date -d $day +%U)    #get the week number from start of the year

    #special case for Sunday. unix considers sunday as the start of the week. but for us we need to consider
    #monday as start of the week. For this purpose we subtract 1 to place the build in previous week
    if [ $dayOfWeek -eq 7 ]; then
      weekNum=$(expr $weekNum - 1)
      if [ $weekNum -le 0 ]; then  #check for the yearend
        weekNum=53
      fi
    fi

    canBeRetained ${buildname}
    retain=$?

    if [ $weekNum -eq $currentWeekNum ]; then
      if [ $retain -eq 0 -a $found -ne 1 ]; then  # we didn't found a build that can be retained in the current week
        found=1
      else
        removeBuild $buildname
      fi
    else #week changed
      currentWeekNum=$weekNum
      found=0
      if [ $retain -eq 0 -a $dayOfWeek -eq 1 ]; then
        found=1
      else
        removeBuild $buildname
      fi
    fi
  fi
done

nbuilds=$( find ${cDir} -maxdepth 1 -type d -name "${buildType}" | wc -l )
echo -e "\tNumber of N-builds after cleaning: $nbuilds"

