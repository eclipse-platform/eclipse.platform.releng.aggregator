#!/usr/bin/env bash

# Utility to add "stats" to repos. For documentation, see
# https://wiki.eclipse.org/WTP/Releng/Tools/addRepoProperties

# This "standalone" version is to add the stats property "after the fact".
# It is not used in the automated "promoteRepos".

export BUILD_ROOT=${BUILD_ROOT:-/shared/eclipse/builds/4P}

printf "\n\tDEBUG: %s\n" "BUILD_ROOT: ${BUILD_ROOT}"

#printf "\n\ERROR: %\n" "Script needs to be updated for java9patch, if needed, and these lines removed"
#exit 1

./addRepoPropertiesPatchBuild.sh \
  "/home/data/httpd/download.eclipse.org/eclipse/updates/4.5-P-builds/P20150622-0925" \
  "4.5-P-builds" \
  "P20150622-0925" \
  "org.eclipse.jdt.java9patch,org.eclipse.jdt.java9patch.source" \
  "_1.0.0.v20150622-0644_BETA_JAVA9"

#./addRepoProperties.sh\
  #  "/data/httpd/download.eclipse.org/eclipse/updates/4.3-P-builds/P20140311-1530"\
  #  "4.3-P-builds"\
  #  "P20140220-1424"\
  #  "org.eclipse.jdt.java8patch"\
  #  "_1.0.0.v20140220-1916"

