#!/usr/bin/env bash

# Utility to update both 3.x and 4.x index pages, for all build technologies

# for testing, may not be in "production" location
if [[ -f /shared/eclipse/sdk/updateIndexFilesFunction.shsource ]] 
then
    source /shared/eclipse/sdk/updateIndexFilesFunction.shsource
else
    source updateIndexFilesFunction.shsource
fi
updateIndex

#updateIndex 3 PDE
#updateIndex 4 PDE

# remember, two argument case, with "CBI" 
# is now interpreted as "MAIN". 
#updateIndex 3 CBI
#updateIndex 4 CBI

#updateIndex 3 MAIN
#updateIndex 4 MAIN
