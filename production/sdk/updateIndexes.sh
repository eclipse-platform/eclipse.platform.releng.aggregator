#!/usr/bin/env bash

# Utility to update both 3.x and 4.x index pages

# for testing, may not be in "production" location
if [[ -f /shared/eclipse/sdk/updateIndexFilesFunction.shsource ]] 
then
    source /shared/eclipse/sdk/updateIndexFilesFunction.shsource
else
    source updateIndexFilesFunction.shsource
fi
updateIndex 3 PDE
updateIndex 4 PDE
updateIndex 3 CBI
updateIndex 4 CBI
