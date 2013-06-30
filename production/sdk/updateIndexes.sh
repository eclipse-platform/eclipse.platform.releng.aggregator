#!/usr/bin/env bash

# Utility to update both 4.x index pages, for all build technologies

# for testing, may not be in "production" location
if [[ -f /shared/eclipse/sdk/updateIndexFilesFunction.shsource ]] 
then
    source /shared/eclipse/sdk/updateIndexFilesFunction.shsource
else
    source updateIndexFilesFunction.shsource
fi
updateIndex


