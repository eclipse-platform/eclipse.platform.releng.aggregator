#!/usr/bin/env bash

# Utility to update both 3.x and 4.x index pages

# for testing, may not be in "production" location
if [[ -f /shared/eclipse/sdk/updateIndexFilesFunction.shsource ]] 
then
    source /shared/eclipse/sdk/updateIndexFilesFunction.shsource
else
    source updateIndexFilesFunction.shsource
fi
echo "= = = = invalid args"
updateIndex xys
echo "returned: $?"
updateIndex 8
echo "returned: $?"
updateIndex xyx 8
echo "returned: $?"
updateIndex 3 CBIxx
echo "returned: $?"
echo "= = = = dual args, MAJOR first" 
updateIndex 3 MAIN
echo "returned: $?"
updateIndex 4 MAIN
echo "returned: $?"
updateIndex 3 CBI
echo "returned: $?"
updateIndex 4 CBI
echo "returned: $?"
echo "= = = = dual args, BUILD_KIND first" 
updateIndex MAIN 3
echo "returned: $?"
updateIndex MAIN 4
echo "returned: $?"
updateIndex CBI 3
echo "returned: $?"
updateIndex CBI 4
echo "returned: $?"
echo "= = = = single arg, MAJOR"
updateIndex 3
echo "returned: $?"
updateIndex 4
echo "returned: $?"
echo "= = = = single arg, BUILD_KIND"
updateIndex CBI
echo "returned: $?"
updateIndex MAIN
echo "returned: $?"
echo "= = = = no arg"
updateIndex 
echo "returned: $?"


