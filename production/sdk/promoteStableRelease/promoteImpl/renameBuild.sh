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

# Utility for renaming a build. It primarily changes the directory, renames some files. 
# This makes adjustments in index.php and buildproperties.php.

if [[ $# != 4 && $# != 5 ]]
then
  # usage:
  scriptname=$(basename $0)
  printf "\n\t%s\n" "This script, $scriptname requires four (optionally five) arguments, in order: "
  printf "\t\t%s\t%s\n" "oldname" "(e.g. I20120503-1800) "
  printf "\t\t%s\t%s\n" "oldlabel" "(e.g. I20120503-1800 or 4.5.1RC3) "
  printf "\t\t%s\t%s\n" "newdirname" "(e.g. S-3.8M7-201205031800) "
  printf "\t\t%s\t%s\n" "newlabel" "(e.g. 3.8M7 or 4.2M7 or KeplerM3) "
  printf "\t\t%s\t%s\n" "dirname"  "Optional, this is used when the rename should be done on a directory other than 'oldname', such as for INDEX_ONLY update."
  printf "\t%s\n" "for example,"
  printf "\t%s\n" "./$scriptname I20120503-1800 I20120503-1800 S-3.8M7-201205031800 3.8M7 [S-3.8M7-201205031800]"
  exit 1
else
  oldname=$1
  oldlabel=$2
  newdirname=$3
  newlabel=$4
  export dirname=$5
  if [[ -z "${dirname}" ]]
  then
    export dirname=$oldname
  fi
  printf "\n\tInput to renameBuild.sh:\n"
  printf "\t\toldname: ${oldname}\n"
  printf "\t\toldlabel: ${oldlabel}\n"
  printf "\t\tnewdirname: ${newdirname}\n"
  printf "\t\tnewlabel: ${newlabel}\n"
  printf "\t\tdirname: ${dirname}\n\n"
fi


function renamefile ()
{
  # file name is input parameter
  # if the file name ends in gif, do not rename (due to performance analysis gifs).
  if [[ ! $1 =~ .*\.gif  ]]
  then
    if [[ $1 =~ (.*)($oldlabel)(.*) ]]
    then
      echo "changing $1 to ${BASH_REMATCH[1]}$newlabel${BASH_REMATCH[3]}"
      #TODO Could check here, if already equal, and if so, do not try "mv", though doubt it hurts
      # anything, it clutters log with confusing messages during "INDEX_ONLY" update.
      # BUT, some files might have to be updated, so we can not just skip.
      mv "$1" "${BASH_REMATCH[1]}$newlabel${BASH_REMATCH[3]}"
    fi
  fi
}


if [[ "${oldname}" == "${dirname}" ]]
then
  echo "Renaming build $oldname to $newdirname with $newlabel"
else
  echo "Renaming build $oldname to $newdirname with $newlabel but working in directory ${dirname}"
fi

# save original copy, for easy "compare" or "diff"
mkdir -p ${dirname}/ORIG
for file in ${dirname}/buildproperties.*
do
  cp --backup=numbered "${file}" "${dirname}/ORIG/"
done


# ===============================
# We actually call twice, once for equinox, and once for eclipse 
# with different "input". So the expectation is some of these matches
# will "match nothing" depending on the input parameters. 
# Would be better to have a rename for equinox, and one for eclipse?
# ===============================
# be sure to do "long string" first, since "sort string" will also
# match it.
# https://bugs.eclipse.org/bugs/show_bug.cgi?id=435671#7

# specific "replaces" to make sure checksums URLs are correct for equinox
fromString="EQ_BUILD_DIR_SEG = \"${oldname}\""
toString="EQ_BUILD_DIR_SEG = \"${EQUINOX_DL_DROP_DIR_SEGMENT}\""
replaceDirCommand="s!${fromString}!${toString}!g"
echo "replaceDirCommand: $replaceDirCommand"
perl -w -pi -e "${replaceDirCommand}" ${dirname}/buildproperties.php
# ===============================
# specific "replace" to make sure checksums URLs are correct for eclipse
fromString="BUILD_DIR_SEG = \"${oldname}\""
toString="BUILD_DIR_SEG = \"${ECLIPSE_DL_DROP_DIR_SEGMENT}\""
replaceDirCommand="s!${fromString}!${toString}!g"
echo "replaceDirCommand: $replaceDirCommand"
perl -w -pi -e "${replaceDirCommand}" ${dirname}/buildproperties.php
# ===============================



# As far as is known, the "directory name" only comes into play in
# the various "buildproperties.*" files, so that's all we'll replace, for now.
# In most files, it's a "relative" location so newdirname should not be needed.
# We assume if "oldname" is surrounded by path separators ('/') it needs whole directory
# name, not just "label".
# See https://bugs.eclipse.org/bugs/show_bug.cgi?id=414739
replaceDirCommand="s!/${oldname}/!/${newdirname}/!g"
perl -w -pi -e "${replaceDirCommand}" ${dirname}/buildproperties.php

# ===============================
# Don't believe needed (though, used to work this way?)
#perl -w -pi -e ${replaceCommand} ${dirname}/checksum/*

# ===============================
# This section was added/modified from some "dead code". 
# Not sure if it is required, or not?
# the value of "BUILD_ID" in buildproperties.php was not being changed. 
# Not sure if should be done with a broad stroke, or make it specific to 
# just "BUILD_ID"? 
#But, otherwise needs label?
#fromString=$oldname
fromString=$oldlabel
toString=$newlabel
replaceCommand="s!${fromString}!${toString}!g"
perl -w -pi -e "${replaceCommand}" ${dirname}/buildproperties.php
# ===============================
# It appears THIS is the one required ... changing label, inside files, 
# not "directory name" as above.
perl -w -pi -e "${replaceCommand}" ${dirname}/checksum/*

echo -e "Now GPG resigning the files of checksums, since names inside them changed from $oldlabel to $newlabel."
${PROMOTE_IMPL}/resignFiles.sh 

# ===============================

# TODO: can a case statement be used? (Or, at least a function?)
# Integration --> Stable
# Integration --> Release Candidate
# Integration --> Release
# These are for cases where used in headers, titles, etc.

OLD_BUILD_TYPE=${BUILD_TYPE}
# ===============================
if [[ "${oldlabel}" =~ .*RC.* ]]
then
  oldString="Release Candidate Build"
elif [[ "${OLD_BUILD_TYPE}" == "I" ]]
then
  oldString="Integration Build"
elif [[ "${OLD_BUILD_TYPE}" == "M" ]]
then
  oldString="Maintenance Build"
elif [[ "${OLD_BUILD_TYPE}" == "S" ]]
then
  oldString="Stable Build"
else
  echo -e "ERROR: Unexpected OLD_BUILD_TYPE: ${OLD_BUILD_TYPE}"
  exit 1
fi

if [[ "${newlabel}" =~ .*RC.* ]]
then
  newString="Release Candidate Build"
elif [[ "${DL_TYPE}" == "R" ]]
then
  newString="Release Build"
elif [[ "${DL_TYPE}" == "S" ]]
then
  newString="Stable Build"
elif [[ "${DL_TYPE}" == "M" ]]
then
  newString="Maintenance Build"
else
  echo -e "ERROR: Unexpected DL_TYPE: ${DL_TYPE}"
  exit 1
fi

echo -e "\n\tReplacing ${oldString} with ${newString} in ${dirname}/buildproperties.php\n"

replaceBuildNameCommand="s!${oldString}!${newString}!g"
# quotes are critical here, since strings contain spaces!
perl -w -pi -e "${replaceBuildNameCommand}" ${dirname}/buildproperties.php
# ===============================
# some special cases, for the buildproperties.php file
# Note, we do php only, since that's what we need, and if we did want
# to rebuild, say using buildproperties.shsource, would be best to work
# from original values. Less sure what to do with Ant properties,
# buildproperties.properties ... but, we'll decide when needed.

oldString="BUILD_TYPE = \"${OLD_BUILD_TYPE}\""
# We export explicitly what new TYPE should be, from promoteSites.sh script.
newString="BUILD_TYPE = \"${DL_TYPE}\""

replaceBuildNameCommand="s!${oldString}!${newString}!g"
# quotes are critical here, since strings contain spaces!
perl -w -pi -e "${replaceBuildNameCommand}" ${dirname}/buildproperties.php
# ===============================
# some special cases, for the buildproperties.php file
# Note, we do php only, since that's what we need, and if we did want
# to rebuild, say using buildproperties.shsource, would be best to work
# from original values. Less sure what to do with Ant properties,
# buildproperties.properties ... but, we'll decide when needed.

#TODO:  these assignments do not look quite right?
oldString="BUILD_ID = \"${BUILD_LABEL}\""
# We export explicitly what new TYPE should be, from promoteSites.sh script.
newString="BUILD_ID = \"${DL_LABEL}\""

replaceBuildNameCommand="s!${oldString}!${newString}!g"
# quotes are critical here, since strings contain spaces!
perl -w -pi -e "${replaceBuildNameCommand}" ${dirname}/buildproperties.php
# ==============================
# We only ever promote "I" or "M" builds, so this ends with sanity check.
# TODO: current objective, though, is to also cover promoting an S-build, say, 
# to an R-build -- so, work to do? Or is adding 'stable' enough? Test!
if [[ "${oldlabel}" =~ .*RC.* ]]
then
  oldString="BUILD_TYPE_NAME = \"Release Candidate\""
elif [[ $OLD_BUILD_TYPE == "I" ]]
then
  oldString="BUILD_TYPE_NAME = \"Integration\""
elif [[ $OLD_BUILD_TYPE == "S" ]]
then
  oldString="BUILD_TYPE_NAME = \"Stable\""
elif [[ $OLD_BUILD_TYPE == "M" ]]
then
  oldString="BUILD_TYPE_NAME = \"Maintenance\""
else
  echo -e "\n\tERROR: Unexpected OLD_BUILD_TYPE value. ${OLD_BUILD_TYPE}, in $0."
  exit 1
fi

if [[ "${newlabel}" =~ .*RC.* ]]
then
  newString="BUILD_TYPE_NAME = \"Release Candidate\""
elif [[ "${DL_TYPE}" == "R" ]]
then
  newString="BUILD_TYPE_NAME = \"Release\""
elif [[ "${DL_TYPE}" == "S" ]]
then
  newString="BUILD_TYPE_NAME = \"Stable\""
else
  echo -e "\n\tERROR: Unexpected DL_TYPE value, ${DL_TYPE}, in $0."
  exit 1
fi

echo -e "\n\tReplacing ${oldString} with ${newString} in ${dirname}/buildproperties.php\n"

replaceBuildNameCommand="s!${oldString}!${newString}!g"
# quotes are critical here, since strings might contain spaces!
perl -w -pi -e "${replaceBuildNameCommand}" ${dirname}/buildproperties.php
# ==============================
# One special case for promoted builds, is the "FAILED" icons are
# changed to "OK", since all unit tests accounted for, if not fixed.
# IS THIS STILL NEEDED?
#oldString="FAIL.gif"
#newString="OK.gif"
#replaceBuildNameCommand="s!${oldString}!${newString}!g"
# quotes are critical here, since strings might contain spaces!
#perl -w -pi -e "${replaceBuildNameCommand}" ${dirname}/index.php
# ==============================

# If the names are equal, then we must be doing an "index only" update, and no need to 
# to 'move'
if [[ ! "${dirname}" == "${newdirname}" ]]
then
  echo -e "\n\tMove old directory, $oldname, to new directory, $newdirname. (before file renames)\n"
  mv $oldname $newdirname
else 
  echo -e "\n\tINFO: dirname was equal to newdirname, so assuming this is a update index job"
  echo -e "\t\tdirname: ${dirname} \tnewdirname: ${newdirname}"
fi
# ==============================
# We (currently) rename files under current directory, and in 'checksums'.
# No need to go deeper (currently) and can be harm, since we do have a copy of
# 'repository' in there (so things things with same name as build directory, such
# as branding bundles? and /repository/binaries get renamed too, if we go too deep.
# Even though we should not need that copy of 'repository' any longer, we might,
# some day?
echo -e "\n\tRename files in new directory, ./${newdirname}, to new name."
echo -e "\tLooking for file names containing oldlabel: \"*${oldlabel}*\""
nFiles=$(find ./${newdirname} -mindepth 1 -maxdepth 2 -name "*${oldlabel}*" -print | wc -l)
echo -e "\n\t $nFiles files found to rename.\n"

for file in $(find ./${newdirname} -mindepth 1 -maxdepth 2 -name "*${oldlabel}*" -print)
do
  renamefile $file
done
