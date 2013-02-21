#!/usr/bin/env bash

# TODO: The idea is to remove builds over 4 days old, but leave at least 4 on site.
#       This logic, though, in theory, depending on when ran, could find say 6 builds,
#       then remove them all if all older than 4 days.
nbuilds=$( find /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4 -maxdepth 1 -name "N*" -exec echo '{}' \; | wc -l )
if [[ nbuilds > 4 ]]
then
    echo "Number of builds before cleaning: $nbuilds"
    find /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4 -maxdepth 1 -ctime +3 -name "N*" -exec echo '{}' \;
    find /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4 -maxdepth 1 -ctime +3 -name "N*" -exec rm -fr '{}' \;
    nbuilds=$( find /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4 -maxdepth 1 -name "N*" -exec echo '{}' \; | wc -l )
    echo "Number of builds after cleaning: $nbuilds"
    source /shared/eclipse/sdk/updateIndexFilesFunction.shsource
    updateIndex 4 PDE

else
    echo "Nothing cleaned, not more than 4 days"
fi

# shared (build machine)
# can be aggressive in removing builds from "downloads", but not "updates"
find /opt/public/eclipse/eclipse4N/siteDir/eclipse/downloads/drops4 -maxdepth 1 -ctime +1 -name "N*" -exec echo '{}' \;
find /opt/public/eclipse/eclipse4N/siteDir/eclipse/downloads/drops4 -maxdepth 1 -ctime +1 -name "N*" -exec rm -fr '{}' \;

find /opt/public/eclipse/eclipse4N/siteDir/equinox/drops -maxdepth 1 -ctime +1 -name "N*" -exec echo '{}' \;
find /opt/public/eclipse/eclipse4N/siteDir/equinox/drops -maxdepth 1 -ctime +1 -name "N*" -exec rm -fr '{}' \;
#
# promotion scripts
find /opt/public/eclipse/sdk/promotion/queue -name "RAN*" -ctime +2 -exec echo '{}' \;
find /opt/public/eclipse/sdk/promotion/queue -name "RAN*" -ctime +2 -exec rm '{}' \;


