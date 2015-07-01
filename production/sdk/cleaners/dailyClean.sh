#!/usr/bin/env bash

# Utility to remove builds over 4 days old, but leave at least 4 on site.

cDir="/home/data/httpd/download.eclipse.org/eclipse/downloads/drops4"
buildType="N*"
allOldBuilds=$( find ${cDir} -maxdepth 1 -type d -ctime +3 -name "${buildType}*" )
echo -e "\n\tDEBUG: allOldBuilds: \n${allOldBuilds}"

nOldBuilds=$( echo -e "${allOldBuilds}" | wc -l )
echo -e "nOldBuilds: $nOldBuilds"

#if (( ${nOldBuilds} > 4 ))
#then
    # Make sure we leave at least 4 on DL server, no matter how old
    # TODO: how to avoid 'ls' (see http://mywiki.wooledge.org/ParsingLs)
    newest=$( ls -1 -t -d ${cDir}/${buildType} | head -4)
    #DEBUG    echo -e "\n\tnewest: \n${newest}";
    reNotToDelete=$(printf '%s\n' "${newest[@]}" | paste -sd '|')
    echo "DEBUG: reNotToDelete: ${reNotToDelete}"
    for buildname in ${allOldBuilds}; do
        if [[ $buildname =~ $reNotToDelete ]]
        then
            echo -e "Not removed (since one of 4 newest, even though old): \n$buildname"
        else
            rm -fr $buildname
            RC=$?
            if [[ $RC = 0 ]]
            then
                echo -e "Removed: $buildname"
            else
                echo -e "\n\tAn Error occured removeding $buildname. RC: $RC"
            fi
        fi
    done

    nbuilds=$( find ${cDir} -maxdepth 1 -name ${buildType} | wc -l )
    echo "Number of builds after cleaning: $nbuilds"

    source /shared/eclipse/sdk/updateIndexFilesFunction.shsource
    updateIndex

#else
#    echo "Nothing cleaned, not more than 4 days"
#fi

# shared (build machine)
# can be aggressive in removing builds from "downloads" and now, with CBI build, from updates too
#find /shared/eclipse/eclipse4N/siteDir/eclipse/downloads/drops4 -maxdepth 1 -ctime +1 -name "N*" -ls -exec rm -fr '{}' \;
find /shared/eclipse/builds/4N/siteDir/eclipse/downloads/drops4 -maxdepth 1 -ctime +1 -name "N*" -ls -exec rm -fr '{}' \;
find /shared/eclipse/builds/4N/siteDir/equinox/drops -maxdepth 1 -ctime +1 -name "N*" -ls -exec rm -fr '{}' \;
find /shared/eclipse/builds/4N/siteDir/updates/4.6-N-builds -maxdepth 1 -ctime +1 -name "N*" -ls -exec rm -fr '{}' \;
#
# promotion scripts
find /shared/eclipse/sdk/promotion/queue -name "RAN*" -ctime +2 -ls -exec rm '{}' \;

