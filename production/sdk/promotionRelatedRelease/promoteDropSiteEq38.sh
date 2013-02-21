#!/usr/bin/env bash

# note, for equinox, we use the "4.2 build",
# but label is "3.8"

DROP_ID=M20130204-1200
DL_LABEL=3.8.2
# DROP_TYPE either S, R
DROP_TYPE=R

BUILD_TIMESTAMP=${DROP_ID//[MI-]/}
DL_DROP_ID=${DROP_TYPE}-${DL_LABEL}-${BUILD_TIMESTAMP}

source createEquinoxPromotionScriptFunction.sh

#

cd /opt/public/eclipse/eclipse4M/siteDir/equinox/drops
echo "PWD: ${PWD}"
cp /opt/public/eclipse/sdk/renameBuild.sh .

echo "save temp backup copy to ${DROP_ID}ORIG"
rsync -ra ${DROP_ID}/ ${DROP_ID}ORIG

echo "rename ${DROP_ID} ${DL_DROP_ID} ${DL_LABEL}"
./renameBuild.sh ${DROP_ID} ${DL_DROP_ID} ${DL_LABEL}

# For Equinox, we don't do the promotion, just create a
# script to do it and put it in the right place for others to run.
createPromotionScriptEq ${DL_DROP_ID}

echo "move backup back to original"
mv ${DROP_ID}ORIG ${DROP_ID}

rm renameBuild.sh
