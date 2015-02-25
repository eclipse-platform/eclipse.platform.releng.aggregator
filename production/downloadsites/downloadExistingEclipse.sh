#!/usr/bin/env bash

# Utility to download the overall "webpages" from DL site, without getting all 
# the unnessary things, and espcially without getting the huge artifacts. 

# This is written to be ran "in place" in a local clone of the git tree, so that 
# a proper "compare" can be done with what is already checked in to git. 

rsync -aP --delete-excluded \
  --exclude '/TIME' --exclude '**/ztime/' --exclude '**/pde/' --exclude '**/equinox/' --exclude '**/eclipse.org-common/' \
  --exclude '**/e4/' --exclude '**/updates/' \
  --include '/index.html' --include '**/drops/index.html'  --include '**/drops4/index.html'  \
  --exclude '**/drops/**' --exclude '**/drops4/**' --exclude 'downloads/index.html' --exclude '**/downloads/eclipse3x.html' \
  build:/home/data/httpd/download.eclipse.org/eclipse/ ./eclipse/