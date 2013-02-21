#!/usr/bin/env bash

# Important: it is assumed this script is ran from the directory
# that is the parent of the directory to rename

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

# not all these file types may exist, we include all the commonly used ones, though,
# just in case future changes to site files started to have them. There is no harm, per se,
# if the perl command fails.
# TODO: could add some "smarts" here to see if all was as expected before making changes.
perl -w -pi -e ${replaceCommand} ${oldname}/*.php
perl -w -pi -e ${replaceCommand} ${oldname}/*.map
perl -w -pi -e ${replaceCommand} ${oldname}/*.html
perl -w -pi -e ${replaceCommand} ${oldname}/*.xml
perl -w -pi -e ${replaceCommand} ${oldname}/checksum/*

# TODO: need to make this part of case statement, to handle
# Integration --> Stable
# Integration --> Release Candidate
# Integration --> Release
# These are for cases where used in headers, titles, etc.
oldBuildName="Integration Build"
newBuildName="Stable Build"
replaceBuildNameCommand="s!${oldBuildName}!${newBuildName}!g"
# quotes are critical here, since strings contain spaces!
perl -w -pi -e "${replaceBuildNameCommand}" ${oldname}/*.php

# move directory before file renames, so it won't be in file path name twice
mv $oldname $newdirname

for file in `find ./${newdirname} -name "*${oldname}*" -print `
do
    renamefile $file
done
