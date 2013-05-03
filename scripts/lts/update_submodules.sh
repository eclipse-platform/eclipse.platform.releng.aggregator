#!/bin/bash

#
# This script is used by the LTS Hudson builder to update the aggregator submodule automatically
#

AGGR_BRANCH=R4_2_maintenance

cd $WORKSPACE

git clone -b $AGGR_BRANCH --recursive ssh://lts.eclipse.org:29418/platform/eclipse.platform.releng.aggregator ./

while read line
do
    repo=$( echo $line | cut -d':' -f1 )
    branch=$( echo $line | cut -d':' -f2 )
    #trim
    branch=`echo $branch`
    if [ -e $repo ]
    then
        pushd $repo
        git fetch
        if [ "$branch" == "R4_2_maintenance" ] || [ "$branch" == "R3_8_maintenance" ]
        then
            git checkout origin/$branch
        else
            git checkout $branch
        fi
        popd
    fi
done < streams/repositories.txt

git checkout $AGGR_BRANCH
git rebase origin/$AGGR_BRANCH
git commit -a -m "Update submodules" && echo "Submodules updated"
git push origin $AGGR_BRANCH

