#!/usr/bin/env bash

# TODO: The idea is to remove builds over 4 days old, but leave at least 4 on site.
#       This logic, though, in theory, depending on when ran, could find say 6 builds,
#       then remove them all if all older than 4 days.
nbuilds=$( find /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4 -maxdepth 1 -name "N*" -exec echo '{}' \; | wc -l )
if [[ $nbuilds > 4 ]]
then
    echo "Number of builds before cleaning: $nbuilds"
    find /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4 -maxdepth 1 -ctime +3 -name "N*" -ls -exec rm -fr '{}' \;
    nbuilds=$( find /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4 -maxdepth 1 -name "N*" -exec echo '{}' \; | wc -l )
    echo "Number of builds after cleaning: $nbuilds"
    source /shared/eclipse/sdk/updateIndexFilesFunction.shsource
    updateIndex 

else
    echo "Nothing cleaned, not more than 4 days"
fi

# shared (build machine)
# can be aggressive in removing builds from "downloads" and now, with CBI build, from updates too
#find /shared/eclipse/eclipse4N/siteDir/eclipse/downloads/drops4 -maxdepth 1 -ctime +1 -name "N*" -ls -exec rm -fr '{}' \;
find /shared/eclipse/builds/4N/siteDir/eclipse/downloads/drops4 -maxdepth 1 -ctime +1 -name "N*" -ls -exec rm -fr '{}' \;
find /shared/eclipse/builds/4N/siteDir/equinox/drops -maxdepth 1 -ctime +1 -name "N*" -ls -exec rm -fr '{}' \;
find /shared/eclipse/builds/4N/siteDir/updates/4.3-N-builds -maxdepth 1 -ctime +1 -name "N*" -ls -exec rm -fr '{}' \;
#
# promotion scripts
find /shared/eclipse/sdk/promotion/queue -name "RAN*" -ctime +2 -ls -exec rm '{}' \;

