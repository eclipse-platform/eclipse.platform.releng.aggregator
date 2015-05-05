#!/usr/bin/env bash

# Important: it is assumed this script is ran from the directory
# that is the parent of the directory to rename

# CAUTION: this is hard coded for going from "I" build to "S" build.
# Needs adjustment for "R" build.

# its assumed oldname is old name of directory and buildId, such as I20120503-1800
# newdirname is new name for directory, such as S-3.8M7-201205031800 and
# newlabel is the new "short name" of the deliverables, such as 3.8M7

if [[ $# != 3 && $# != 4 ]]
then
  # usage:
  scriptname=$(basename $0)
  printf "\n\t%s\n" "This script, $scriptname requires three (optionally four) arguments, in order: "
  printf "\t\t%s\t%s\n" "oldname" "(e.g. I20120503-1800) "
  printf "\t\t%s\t%s\n" "newdirname" "(e.g. S-3.8M7-201205031800) "
  printf "\t\t%s\t%s\n" "newlabel" "(e.g. 3.8M7 or 4.2M7 or KeplerM3) "
  printf "\t\t%s\t%s\n" "dirname"  "Optional, this is used when the rename should be done on a directory other than 'oldName', such as for INDEX_ONLY update."
  printf "\t%s\n" "for example,"
  printf "\t%s\n\n" "./$scriptname I20120503-1800 S-3.8M7-201205031800 3.8M7 [S-3.8M7-201205031800]"
  exit 1
else
  oldname=$1
  newdirname=$2
  newlabel=$3
  dirname=$4
  if [[ -z "${dirname}" ]]
  then
    dirname=$oldname
  fi
  printf "\n\tInput to renameBuild.sh:\n"
  printf "\t\toldname: ${oldname}\n"
  printf "\t\tnewdirname: ${newdirname}\n"
  printf "\t\tnewlabel: ${newlabel}\n"
  printf "\t\tdirname: ${dirname}\n\n\n"
fi


function renamefile ()
{
  # file name is input parameter
  # if the file name ends in gif, do not rename (due to performance analysis gifs).
  if [[ ! $1 =~ .*\.gif  ]]
  then
    if [[ $1 =~ (.*)($oldname)(.*) ]]
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

# be sure to do "long string" first, since "sort string" will also
# match it.
# https://bugs.eclipse.org/bugs/show_bug.cgi?id=435671#7

# specific "replaces" to make sure checksums URLs are correct for equinox
fromString="EQ_BUILD_DIR_SEG = \"${oldname}\""
toString="EQ_BUILD_DIR_SEG = \"${EQUINOX_DL_DROP_DIR_SEGMENT}\""
replaceDirCommand="s!${fromString}!${toString}!g"
echo "replaceDirCommand: $replaceDirCommand"
perl -w -pi -e "${replaceDirCommand}" ${dirname}/buildproperties.*

# specific "replace" to make sure checksums URLs are correct for eclipse
fromString="BUILD_DIR_SEG = \"${oldname}\""
toString="BUILD_DIR_SEG = \"${ECLIPSE_DL_DROP_DIR_SEGMENT}\""
replaceDirCommand="s!${fromString}!${toString}!g"
echo "replaceDirCommand: $replaceDirCommand"
perl -w -pi -e "${replaceDirCommand}" ${dirname}/buildproperties.*

fromString=$oldname
toString=$newlabel
replaceCommand="s!${fromString}!${toString}!g"

# As far as is known, the "directory name" only comes into play in
# the various "buildproperties.*" files, so that's all we'll replace, for now.
# In most files, it's a "relative" location so newdirname should not be needed.
# We assume if "oldName" is surrounded by path separators ('/') it needs whole directory
# name, not just "label".
# See https://bugs.eclipse.org/bugs/show_bug.cgi?id=414739
replaceDirCommand="s!/${fromString}/!/${newdirname}/!g"
perl -w -pi -e ${replaceDirCommand} ${dirname}/buildproperties.*

# not all these file types may exist, we include all the commonly used ones, though,
# just in case future changes to site files started to have them. There is no harm, per se,
# if the perl command fails.
# TODO: could add some "smarts" here to see if all was as expected before making changes.
perl -w -pi -e ${replaceCommand} ${dirname}/*.php
perl -w -pi -e ${replaceCommand} ${dirname}/*.html
perl -w -pi -e ${replaceCommand} ${dirname}/*.xml 2>/dev/null
perl -w -pi -e ${replaceCommand} ${dirname}/checksum/*

# TODO: need to make this part of case statement, to handle
# Integration --> Stable
# Integration --> Release Candidate
# Integration --> Release
# These are for cases where used in headers, titles, etc.
# TODO: final "fall through" case should be based on matching
# new label with digits only, such as "4.3" ... not sure
# if this would work for Equinox "Kepler" or "Kepler Released Build"?
OLD_BUILD_TYPE=${oldname:0:1}
if [[ $OLD_BUILD_TYPE == "I" ]]
then
  oldString="Integration Build"
elif [[ $OLD_BUILD_TYPE == "M" ]]
then
  oldString="Maintenance Build"
else
  oldString="Unexpected OLD_BUILD_TYPE: $OLD_BUILD_TYPE"
fi

if [[ "${newlabel}" =~ .*RC.* ]]
then
  newString="Release Candidate Build"
elif [[ "${newlabel}" =~ .*R.* ]]
then
  newString="Release Build"
elif [[ "${newlabel}" =~ .*M.* ]]
then
  newString="Stable Build"
else
  # releases have labels such as 4.4 or 4.3.1 (or KeplerSR1)  so former
  # won't match any of the above.
  # TODO: put in sanity check to match known release patterns
  # of digits and periods, else bail.
  newString="Release Build"
fi

echo -e "\n\tReplacing ${oldString} with ${newString} in ${oldname}/*.php\n"

replaceBuildNameCommand="s!${oldString}!${newString}!g"
# quotes are critical here, since strings contain spaces!
perl -w -pi -e "${replaceBuildNameCommand}" ${dirname}/*.php

# some special cases, for the buildproperties.php file
# Note, we do php only, since that's what we need, and if we did want
# to rebuild, say using buildproperties.shsource, would be best to work
# from original values. Less sure what to do with Ant properties,
# buildproperties.properties ... but, we'll decide when needed.
# TODO: New label doesn't have "R" in it ... just, for example, "4.3".
# for now, we'll "fall through" to "R",  if doesn't match anything else,
# but this won't work well if/when we add others, such as X or T for test
# builds.

if [[ $OLD_BUILD_TYPE == "I" ]]
then
  oldString="BUILD_TYPE = \"I\""
elif [[ $OLD_BUILD_TYPE == "M" ]]
then
  oldString="BUILD_TYPE = \"M\""
else
  oldString="Unexpected OLD_BUILD_TYPE: $OLD_BUILD_TYPE"
fi

if [[ "${newlabel}" =~ .*RC.* && $OLD_BUILD_TYPE == "I" ]]
then
  newString="BUILD_TYPE = \"S\""
elif [[ "${newlabel}" =~ .*RC.* && $OLD_BUILD_TYPE == "M" ]]
then
  newString="BUILD_TYPE = \"M\""
elif [[ "${newlabel}" =~ .*R.* ]]
then
  newString="BUILD_TYPE = \"R\""
elif [[ "${newlabel}" =~ .*M.* ]]
then
  newString="BUILD_TYPE = \"S\""
else
  # see previous comment on forms of "releases" (4.3.1, 4.2, Kepler, KeplerSR1, etc.)
  newString="BUILD_TYPE = \"R\""
fi

replaceBuildNameCommand="s!${oldString}!${newString}!g"
# quotes are critical here, since strings contain spaces!
perl -w -pi -e "${replaceBuildNameCommand}" ${dirname}/buildproperties.php

# We only ever promote "I" or "M" builds, so this ends with sanity check.
if [[ $OLD_BUILD_TYPE == "I" ]]
then
  oldString="BUILD_TYPE_NAME = \"Integration\""
elif [[ $OLD_BUILD_TYPE == "M" ]]
then
  oldString="BUILD_TYPE_NAME = \"Maintenance\""
else
  oldString="BUILD_TYPE_NAME = \"Unknown OLD_BUILD_TYPE, $OLD_BUILD_TYPE\""
fi

if [[ "${newlabel}" =~ .*RC.* ]]
then
  newString="BUILD_TYPE_NAME = \"Release Candidate\""
elif [[ "${newlabel}" =~ .*R.* ]]
then
  newString="BUILD_TYPE_NAME = \"Release\""
elif [[ "${newlabel}" =~ .*M.* ]]
then
  newString="BUILD_TYPE_NAME = \"Stable\""
else
  newString="BUILD_TYPE_NAME = \"Release\""
fi

echo -e "\n\tReplacing ${oldString} with ${newString} in ${dirname}/buildproperties.php\n"

replaceBuildNameCommand="s!${oldString}!${newString}!g"
# quotes are critical here, since strings might contain spaces!
perl -w -pi -e "${replaceBuildNameCommand}" ${dirname}/buildproperties.php

# One special case for promoted builds, is the "FAILED" icons are
# changed to "OK", since all unit tests accounted for, if not fixed.
oldString="FAIL.gif"
newString="OK.gif"
replaceBuildNameCommand="s!${oldString}!${newString}!g"
# quotes are critical here, since strings might contain spaces!
perl -w -pi -e "${replaceBuildNameCommand}" ${dirname}/index.php

# If the names are equal, then we must be doing an "index only" update, and no need to 
# to 'move'
if [[ ! "${dirname}" == "${newdirname}" ]]
then
  echo -e "\n\n\tMove old directory, $oldname, to new directory, $newdirname.\n\n"
  # move directory before file renames
  mv $oldname $newdirname
fi
# We (currently) rename files under current directory, and in 'checksums'.
# No need to go deeper (currently) and can be harm, since we do have a copy of
# 'repository' in there (so things things with same name as build directory, such
# as branding bundles? and /repository/binaries get renamed too, if we go too deep.
# Even though we should not need that copy of 'repository' any longer, we might,
# some day?
echo -e "\n\n\tRename files in new directory, ./${newdirname}, to new name.\n\n"
nFiles=$(find ./${newdirname} -mindepth 1 -maxdepth 2 -name "*${oldname}*" -print | wc -l)
echo -e "\n\t $nFiles files found to rename.\n"

for file in $(find ./${newdirname} -mindepth 1 -maxdepth 2 -name "*${oldname}*" -print)
do
  renamefile $file
done
