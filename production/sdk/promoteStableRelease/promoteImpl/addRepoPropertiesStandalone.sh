#!/usr/bin/env bash

# Utility to add "stats" to repos. For documentation, see 
# https://wiki.eclipse.org/WTP/Releng/Tools/addRepoProperties

# This "standalone" version is to add the stats property "after the fact". 
# It is not used in the automated "promoteRepos". 


./addRepoProperties.sh \
  file:///home/data/users/david_williams/downloads/eclipse/updates/4.3-P-builds/
  4.3-P-builds \
  P20140317-1600 \
  1.0.0.v20140317-1956
