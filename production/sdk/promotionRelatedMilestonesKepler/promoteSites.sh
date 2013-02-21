#!/usr/bin/env bash

DROP_ID=I20130204-1400
DL_LABEL=4.3M5a
DL_LABEL_EQ=KeplerM5a

./promoteDropSiteEq.sh ${DROP_ID} ${DL_LABEL_EQ}
rccode=$?
if [[ $rccode != 0 ]]
then
    printf "\n\n\t%s\n\n" "ERROR: promoteDropSiteEq.sh failed. Subsequent promotion cancelled."
    exit $rccode
fi

./promoteDropSite.sh   ${DROP_ID} ${DL_LABEL}
if [[ $rccode != 0 ]]
then
    printf "\n\n\t%s\n\n" "ERROR: promoteDropSite.sh failed. Subsequent promotion cancelled."
    exit $rccode
fi


./promoteRepo.sh ${DROP_ID} ${DL_LABEL}
if [[ $rccode != 0 ]]
then
    printf "\n\n\t%s\n\n" "ERROR: promoteRepo.sh failed."
    exit $rccode
fi

exit 0
