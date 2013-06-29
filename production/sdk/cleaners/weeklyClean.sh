#!/usr/bin/env bash

# clean 4.x M builds
find /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4 -maxdepth 1 -ctime +30 -name "M*" -ls -exec rm -fr '{}' \;
# clean 3.x M builds
find /home/data/httpd/download.eclipse.org/eclipse/downloads/drops -maxdepth 1 -ctime +30 -name "M*" -ls -exec rm -fr '{}' \;

source /shared/eclipse/sdk/updateIndexFilesFunction.shsource
updateIndex 


# shared (build machine)
# can be aggressive in removing builds from "downloads", and even "updates" now with new method used in CBI builds

find /shared/eclipse/builds/4I/siteDir/eclipse/downloads/drops4 -maxdepth 1 -ctime +2 -name "I*" -ls -exec rm -fr '{}' \;

find /shared/eclipse/builds/4I/siteDir/equinox/drops -maxdepth 1 -ctime +2 -name "I*" -ls -exec rm -fr '{}' \;

find /shared/eclipse/builds/4I/siteDir/updates/4.4-I-builds -maxdepth 1 -ctime +2 -name "I*" -ls -exec rm -fr '{}' \;

# don't really need these yet, until after Kepler 
find /shared/eclipse/builds/4M/siteDir/eclipse/downloads/drops4 -maxdepth 1 -ctime +2 -name "M*" -ls -exec rm -fr '{}' \;
find /shared/eclipse/builds/4M/siteDir/equinox/drops -maxdepth 1 -ctime +2 -name "M*" -ls -exec rm -fr '{}' \;
find /shared/eclipse/builds/4M/siteDir/updates/4.3-M-builds -maxdepth 1 -ctime +2 -name "M*" -ls -exec rm -fr '{}' \;



