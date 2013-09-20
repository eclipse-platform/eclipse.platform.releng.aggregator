#!/usr/bin/env bash

# Important: it is assumed this script is ran from the directory
# that is the parent of the directory to rename

# CAUTION: this is hard coded for going from "I" build to "S" build. 
# Needs adjustment for "R" build.

# its assumed oldname is old name of directory and buildId, such as I20120503-1800
# newdirname is new name for directory, such as S-3.8M7-201205031800 and
# newlabel is the new "short name" of the deliverables, such as 3.8M7
oldname=$1
newdirname=$2
newlabel=$3

function renamefile ()
{
    # file name is input parameter
    if [[ $1 =~ (.*)($oldname)(.*) ]]
    then
        echo "changing $1 to ${BASH_REMATCH[1]}$newlabel${BASH_REMATCH[3]}"
        mv "$1" "${BASH_REMATCH[1]}$newlabel${BASH_REMATCH[3]}"

    fi

}

if [[ $# != 3 ]]
then
    # usage:
    scriptname=$(basename $0)
    printf "\n\t%s\n" "This script, $scriptname requires three arguments, in order: "
    printf "\t\t%s\t%s\n" "oldname" "(e.g. I20120503-1800) "
    printf "\t\t%s\t%s\n" "newdirname" "(e.g. S-3.8M7-201205031800) "
    printf "\t\t%s\t%s\n" "newlabel" "(e.g. 3.8M7 or 4.2M7 or KeplerM3) "
    printf "\t%s\n" "for example,"
    printf "\t%s\n\n" "./$scriptname I20120503-1800 S-3.8M7-201205031800 3.8M7"
    exit 1
fi
echo "Renaming build $oldname to $newdirname with $newlabel"

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
perl -w -pi -e ${replaceDirCommand} ${oldname}/buildproperties.*

# not all these file types may exist, we include all the commonly used ones, though,
# just in case future changes to site files started to have them. There is no harm, per se,
# if the perl command fails.
# TODO: could add some "smarts" here to see if all was as expected before making changes.
perl -w -pi -e ${replaceCommand} ${oldname}/*.php
perl -w -pi -e ${replaceCommand} ${oldname}/*.html
perl -w -pi -e ${replaceCommand} ${oldname}/*.xml
perl -w -pi -e ${replaceCommand} ${oldname}/checksum/*

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
    oldString="Unexpected BUILD_TYPE: $BUILDTYPE"
fi

if [[ "${newlabel}" =~ .*RC.* ]]
then 
    newString="Release Candidate Build"
elif [[ "${newlabel}" =~ .*R.* ]]
then
    newString="Release Build"
elif [[ "${newlabel}" =~ .*S.* ]]
then
    newString="Stable Build"
else 
    newString="newlabel value unexpected or not matched: ${newlabel}"
fi

echo "replacing ${oldString} with ${newString} in ${oldname}/*.php"

replaceBuildNameCommand="s!${oldString}!${newString}!g"
# quotes are critical here, since strings contain spaces!
perl -w -pi -e "${replaceBuildNameCommand}" ${oldname}/*.php

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
elif [[ "${newlabel}" =~ .*S.* ]]
then
    newString="BUILD_TYPE = \"S\""
else 
    newString="BUILD_TYPE = \"R\""
fi

replaceBuildNameCommand="s!${oldString}!${newString}!g"
# quotes are critical here, since strings contain spaces!
perl -w -pi -e "${replaceBuildNameCommand}" ${oldname}/buildproperties.php

if [[ $OLD_BUILD_TYPE == "I" ]]
then
    oldString="BUILD_TYPE_NAME = \"Integration\""
elif [[ $OLD_BUILD_TYPE == "M" ]]
then
    oldString="BUILD_TYPE_NAME = \"Maintenance\""
else
    oldString="BUILD_TYPE_NAME = \"Unknown BUILD_TYPE\""
fi

if [[ "${newlabel}" =~ .*RC.* ]]
then 
    newString="BUILD_TYPE_NAME = \"Release Candidate\""
elif [[ "${newlabel}" =~ .*R.* ]]
then
    newString="BUILD_TYPE_NAME = \"Release\""
elif [[ "${newlabel}" =~ .*S.* ]]
then
    newString="BUILD_TYPE_NAME = \"Stable\""
else 
    newString="BUILD_TYPE_NAME = \"newlabel, ${newlabel}, unexpected or unmatched\""
fi

echo "Replacing ${oldString} with ${newString} in ${oldname}/buildproperties.php"

replaceBuildNameCommand="s!${oldString}!${newString}!g"
# quotes are critical here, since strings might contain spaces!
perl -w -pi -e "${replaceBuildNameCommand}" ${oldname}/buildproperties.php

# One special case for promoted builds, is the "FAILED" icons are 
# changed to "OK", since all unit tests accounted for, if not fixed. 
oldString="FAIL.gif"
newString="OK.gif"
replaceBuildNameCommand="s!${oldString}!${newString}!g"
# quotes are critical here, since strings might contain spaces!
perl -w -pi -e "${replaceBuildNameCommand}" ${oldname}/index.php

# move directory before file renames, so it won't be in file path name twice
mv $oldname $newdirname

for file in `find ./${newdirname} -name "*${oldname}*" -print `
do
    renamefile $file
done
