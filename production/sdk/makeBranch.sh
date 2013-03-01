#!/usr/bin/env bash

# Utility script to semi-automate creation of a branch using committer shell (or, e4Build id)

#project=simrel
project=platform
#project=jdt

#reponame=eclipse.platform.team
#reponame=eclipse.jdt.debug
#reponame=eclipse.platform.releng
#reponame=org.eclipse.simrel.build
reponame=eclipse.platform.releng.maps

startTag=R4_2_2
#startTag=JunoSR0
#startTag=R4_2
#startTag=R3_8
branchName=R4_2_2_maintenance_patches
#branchName=R4_2_maintenance
#branchName=R3_8_maintenance

source checkForErrorExit.sh

repoprojectroot=/gitroot/$project
gitreponame=$reponame.git
repo=${repoprojectroot}/${gitreponame}

printf "\n\t%s" "Creating branch $branchName from $startTag "
printf "\n\t%s\n\n" "   for repo $repo "

temprepoarea=/shared/eclipse/temp/mkbranch
mkdir -p $temprepoarea
checkForErrorExit $? "Could not create temprepoarea!?: $temprepoarea"
cd $temprepoarea
checkForErrorExit $? "Could not cd to temprepoarea!?: $temprepoarea"

git clone $repo
checkForErrorExit $? "Could not create clone repo: $repo"


cd $reponame
checkForErrorExit $? "Could not cd to reponame: $reponame"

git checkout master
checkForErrorExit $? "Error during initial checkout of master"

git fetch
checkForErrorExit $? "Error during fetch"

git checkout -b $branchName $startTag
checkForErrorExit $? "Could not create local branch ($branchName) from tag ($startTag)"

# The part above could be done by anyone. The following code is where committer access is required
# (which is accomplished by using e4Build id for the general case).

# note: using cd here is where executing this whole thing from a committer shell on server is handy
cd $repo
checkForErrorExit $? "Could not cd to repo: $repo"

git config hooks.allowcreatenottopicbranch true
checkForErrorExit $? "Could not change config hook"

cd -
checkForErrorExit $? "Could not cd back to local repo"

git push origin $branchName
checkForErrorExit $? "Could not push branch to origin"

cd $repo
checkForErrorExit $? "Could not cd to repo: $repo"

git config --unset hooks.allowcreatenottopicbranch
checkForErrorExit $? "Could not unset config hook"

# simple cleanup ...
#rm -fr $temprepoarea
#checkForErrorExit $? "Error while removing temp repo"

printf "\n\t%s" "Completed creating branch $branchName from $startTag "
printf "\n\t%s\n\n" "   for repo $repo "

exit 0

