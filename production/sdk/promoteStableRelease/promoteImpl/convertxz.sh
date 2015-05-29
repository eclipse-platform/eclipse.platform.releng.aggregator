#!/usr/bin/env bash

# Utility function to convert repo site metadata files to XZ compressed files. 
# One use case is to invoke with something similar to
#
# find . -maxdepth 3  -name content.jar  -execdir convertxz.sh '{}' \;

source ${HOME}/bin/createXZ.shsource
# add -noforce if we should leave existing xml.xz files alone. (such as from a cron job, 
# that repeatedly checks a directory tree via 'find', etc.
createXZ ${PWD} -
