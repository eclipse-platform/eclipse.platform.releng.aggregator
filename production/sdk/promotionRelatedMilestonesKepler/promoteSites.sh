#!/usr/bin/env bash

DROP_ID=I20130523-1400
DL_LABEL=4.3RC2
DL_LABEL_EQ=KeplerRC2

printf "\n\n\t%s\t%s\n" "DROP_ID" "$DROP_ID"
printf "\n\t%s\t%s\n" "DL_LABEL" "$DL_LABEL"
printf "\n\t%s\t%s\n" "DL_LABEL_EQ" "$DL_LABEL_EQ"

# we do Equinox first, since it has to wait in que until 
# cronjob promotes it
./promoteDropSiteEq.sh ${DROP_ID} ${DL_LABEL_EQ}
rccode=$?
if [[ $rccode != 0 ]]
then
    printf "\n\n\t%s\n\n" "ERROR: promoteDropSiteEq.sh failed. Subsequent promotion cancelled."
    exit $rccode
fi

./promoteDropSite.sh   ${DROP_ID} ${DL_LABEL}
rccode=$?
if [[ $rccode != 0 ]]
then
    printf "\n\n\t%s\n\n" "ERROR: promoteDropSite.sh failed. Subsequent promotion cancelled."
    exit $rccode
fi


./promoteRepo.sh ${DROP_ID} ${DL_LABEL}
rccode=$?
if [[ $rccode != 0 ]]
then
    printf "\n\n\t%s\n\n" "ERROR: promoteRepo.sh failed."
    exit $rccode
fi

exit 0
