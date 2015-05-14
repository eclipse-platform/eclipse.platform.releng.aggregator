#!/usr/bin/env bash

# Utility function to convert repo site metadata files to XZ compressed files. 
# One use case is to invoke with something similar to
#
# find . -maxdepth 3  -name content.jar  -execdir convertxz.sh '{}' \;

source ${HOME}/bin/createXZ.shsource
# don't think this "export function" is needed here?
export -f createXZ
createXZ ${PWD}
