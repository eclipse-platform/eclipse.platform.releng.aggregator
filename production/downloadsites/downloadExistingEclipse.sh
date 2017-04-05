#!/usr/bin/env bash

# Utility to download the overall "web pages" from DL site, without getting all 
# the unnecessary things, and especially without getting the huge artifacts. 

# This is written to be ran "in place" in a local clone of the git tree, so that 
# a proper "compare" can be done with what is already checked in to git. 

# The script assumes there's a known host "build", which can be configured in ~/.ssh/config like this:
#
#Host build
#  HostName build.eclipse.org
#  User your-eclipse-ssh-account-name


rsync -aP --delete-excluded --prune-empty-dirs \
  --exclude 'TIME' --exclude '**/ztime/' --exclude '**/pde/' --exclude '**/equinox/' --exclude '**/eclipse.org-common/' \
  --exclude '**/e4/' \
  --include '**/updates/*/categories*/' --include '**/updates/?-builds/*' --include '**/updates/milestones/*' --exclude '**/updates/*/*' \
  --include '/index.html' --include '**/drops/index.html'  --include '**/drops4/index.html'  \
  --exclude '**/drops/**' --exclude '**/drops4/**' --exclude 'downloads/index.html' --exclude '**/downloads/eclipse3x.html' \
  build:/home/data/httpd/download.eclipse.org/eclipse/ ./eclipse/
  
find . -name content.jar -execdir unzip '{}' \; -delete
