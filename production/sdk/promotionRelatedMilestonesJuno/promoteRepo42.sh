#!/usr/bin/env bash


DROP_SITE_ID=I20120608-1400

DROP_LABEL=S-4.2RC4

BUILD_TIMESTAMP=${DROP_SITE_ID//[I-]/}

DL_SITE_ID="${DROP_LABEL}"-"${BUILD_TIMESTAMP}"

BUILDMACHINE_BASE_SITE=/opt/public/eclipse/eclipse4I/siteDir/updates/4.2-I-builds

DLMACHINE_BASE_SITE=/home/data/httpd/download.eclipse.org/eclipse/updates/4.2milestones

BUILDMACHINE_SITE=${BUILDMACHINE_BASE_SITE}/${DROP_SITE_ID}

DLMACHINE_SITE=${DLMACHINE_BASE_SITE}/${DL_SITE_ID}

# remember, need trailing slash since going from existing directories
# contents to new directories contents
echo "BUILDMACHINE_SITE: ${BUILDMACHINE_SITE}/"
echo "DLMACHINE_SITE: ${DLMACHINE_SITE}"
rsync -vr "${BUILDMACHINE_SITE}/"  "${DLMACHINE_SITE}"

echo " ... remember to update composite files ... "

