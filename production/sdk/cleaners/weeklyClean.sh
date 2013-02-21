#!/usr/bin/env bash

# clean 4.x M builds
find /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4 -maxdepth 1 -ctime +30 -name "M*" -exec echo '{}' \;
find /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4 -maxdepth 1 -ctime +30 -name "M*" -exec rm -fr '{}' \;
# clean 3.x M builds
find /home/data/httpd/download.eclipse.org/eclipse/downloads/drops -maxdepth 1 -ctime +30 -name "M*" -exec echo '{}' \;
find /home/data/httpd/download.eclipse.org/eclipse/downloads/drops -maxdepth 1 -ctime +30 -name "M*" -exec rm -fr '{}' \;

source /shared/eclipse/sdk/updateIndexFilesFunction.shsource
/shared/eclipse/sdk/updateIndexes.sh


# shared (build machine)
# can be aggressive in removing builds from "downloads", but not "updates"

find /opt/public/eclipse/eclipse4I/siteDir/eclipse/downloads/drops4 -maxdepth 1 -ctime +2 -name "I*" -exec echo '{}' \;
find /opt/public/eclipse/eclipse4I/siteDir/eclipse/downloads/drops4 -maxdepth 1 -ctime +2 -name "I*" -exec rm -fr '{}' \;

find /opt/public/eclipse/eclipse4I/siteDir/equinox/drops -maxdepth 1 -ctime +2 -name "I*" -exec echo '{}' \;
find /opt/public/eclipse/eclipse4I/siteDir/equinox/drops -maxdepth 1 -ctime +2 -name "I*" -exec rm -fr '{}' \;

find /opt/public/eclipse/eclipse4M/siteDir/eclipse/downloads/drops4 -maxdepth 1 -ctime +2 -name "M*" -exec echo '{}' \;
find /opt/public/eclipse/eclipse4M/siteDir/eclipse/downloads/drops4 -maxdepth 1 -ctime +2 -name "M*" -exec rm -fr '{}' \;
find /opt/public/eclipse/eclipse4M/siteDir/equinox/drops -maxdepth 1 -ctime +2 -name "M*" -exec echo '{}' \;
find /opt/public/eclipse/eclipse4M/siteDir/equinox/drops -maxdepth 1 -ctime +2 -name "M*" -exec rm -fr '{}' \;

find /opt/public/eclipse/eclipse3M/siteDir/eclipse/downloads/drops -maxdepth 1 -name "M*" | wc -l
find /opt/public/eclipse/eclipse3M/siteDir/eclipse/downloads/drops -maxdepth 1 -ctime +2 -name "M*" -exec echo '{}' \;
find /opt/public/eclipse/eclipse3M/siteDir/eclipse/downloads/drops -maxdepth 1 -ctime +2 -name "M*" -exec rm -fr '{}' \;
find /opt/public/eclipse/eclipse3M/siteDir/eclipse/downloads/drops -maxdepth 1 -name "M*" | wc -l

find /opt/public/eclipse/eclipse3M/siteDir/equinox/drops3 -maxdepth 1 -name "M*" | wc -l
find /opt/public/eclipse/eclipse3M/siteDir/equinox/drops3 -maxdepth 1 -ctime +2 -name "M*" -exec echo '{}' \;
find /opt/public/eclipse/eclipse3M/siteDir/equinox/drops3 -maxdepth 1 -ctime +2 -name "M*" -exec rm -fr '{}' \;
find /opt/public/eclipse/eclipse3M/siteDir/equinox/drops3 -maxdepth 1 -name "M*" | wc -l


