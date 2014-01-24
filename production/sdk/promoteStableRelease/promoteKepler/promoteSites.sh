#!/usr/bin/env bash

DROP_ID=M20140117-0910

#DL_LABEL=4.3.2
DL_LABEL=4.3.2RC1
#DL_LABEL_EQ=KeplerSR2
DL_LABEL_EQ=KeplerSR2RC1

# in maintenance, even RCs go in "M-builds"
REPO_SITE_SEGMENT=4.3-M-builds
#REPO_SITE_SEGMENT=4.3

HIDE_SITE=true
# almost always use 'true', to allow some sanity checking, and even mirroring
#HIDE_SITE=false

export CL_SITE=${PWD}
echo "CL_SITE: ${CL_SITE}"

# These are what precedes main drop directory name
#export DL_TYPE=S
#export DL_TYPE=R
export DL_TYPE=M

# variables used on tagging aggregator for milestones (and RCs?) 
# Could probably compute this tag ... but for now easier to type it in each time. 
export NEW_TAG=M4_3_2_RC1
# For now, we'll just use handy Equinox label for tag annotation, but could elaborate in future
export NEW_ANNOTATION="{DL_LABEL_EQ}"
# later combined with BUILD_ROOT, so we get the correct clone
# should very seldom need to change, if ever. 
export AGGR_LOCATION="gitCache/eclipse.platform.releng.aggregator"


# Used in naming repo, etc
export TRAIN_NAME=Kepler

# Build machine locations (would very seldome change)
export BUILD_ROOT=/shared/eclipse/builds/4M
export BUILDMACHINE_BASE_SITE=${BUILD_ROOT}/siteDir/updates/4.3-M-builds

export BUILDMACHINE_BASE_DL=${BUILD_ROOT}/siteDir/eclipse/downloads/drops4
export BUILDMACHINE_BASE_EQ=${BUILD_ROOT}/siteDir/equinox/drops

export PROMOTE_IMPL=/shared/eclipse/sdk/promoteStableRelease/promoteImpl
export BUILD_TIMESTAMP=${DROP_ID//[MI-]/}

# Eclipse Drop Site (final segment)
ECLIPSE_DL_DROP_DIR_SEGMENT=${DL_TYPE}-${DL_LABEL}-${BUILD_TIMESTAMP}
# Equinox Drop Site (final segment)
EQUINOX_DL_DROP_DIR_SEGMENT=${DL_TYPE}-${DL_LABEL_EQ}-${BUILD_TIMESTAMP}

printf "\n\t%s\n\n" "Promoted on: $( date )" > "${CL_SITE}/checklist.txt"
printf "\n\t%20s%25s\n" "DROP_ID" "$DROP_ID" >> "${CL_SITE}/checklist.txt"
printf "\t%20s%25s\n" "DL_LABEL" "$DL_LABEL" >> "${CL_SITE}/checklist.txt"
printf "\t%20s%25s\n" "DL_LABEL_EQ" "$DL_LABEL_EQ" >> "${CL_SITE}/checklist.txt"
printf "\t%20s%25s\n" "REPO_SITE_SEGMENT" "$REPO_SITE_SEGMENT" >> "${CL_SITE}/checklist.txt"
printf "\t%20s%25s\n\n" "HIDE_SITE" "${HIDE_SITE}" >> "${CL_SITE}/checklist.txt"

printf "\t%s\n" "Eclipse downloads:" >> "${CL_SITE}/checklist.txt"
printf "\t%s\n\n" "http://download.eclipse.org/eclipse/downloads/drops4/${ECLIPSE_DL_DROP_DIR_SEGMENT}/" >> "${CL_SITE}/checklist.txt"

printf "\t%s\n" "Update existing (non-production) installs:" >> "${CL_SITE}/checklist.txt"
printf "\t%s\n\n" "http://download.eclipse.org/eclipse/updates/${REPO_SITE_SEGMENT}/" >> "${CL_SITE}/checklist.txt"

printf "\t%s\n" "Specific repository good for building against:" >> "${CL_SITE}/checklist.txt"
printf "\t%s\n\n" "http://download.eclipse.org/eclipse/updates/${REPO_SITE_SEGMENT}/${ECLIPSE_DL_DROP_DIR_SEGMENT}/" >> "${CL_SITE}/checklist.txt"

printf "\t%s\n" "Equinox specific downloads:" >> "${CL_SITE}/checklist.txt"
printf "\t%s\n\n" "http://download.eclipse.org/equinox/drops/${EQUINOX_DL_DROP_DIR_SEGMENT}/" >> "${CL_SITE}/checklist.txt"


# we do Equinox first, since it has to wait in que until 
# cronjob promotes it
${PROMOTE_IMPL}/promoteDropSiteEq.sh ${DROP_ID} ${DL_LABEL_EQ} ${HIDE_SITE}
rccode=$?
if [[ $rccode != 0 ]]
then
    printf "\n\n\t%s\n\n" "ERROR: promoteDropSiteEq.sh failed. Subsequent promotion cancelled."
    exit $rccode
fi

${PROMOTE_IMPL}/promoteDropSite.sh   ${DROP_ID} ${DL_LABEL} ${HIDE_SITE}
rccode=$?
if [[ $rccode != 0 ]]
then
    printf "\n\n\t%s\n\n" "ERROR: promoteDropSite.sh failed. Subsequent promotion cancelled."
    exit $rccode
fi


${PROMOTE_IMPL}/promoteRepo.sh ${DROP_ID} ${DL_LABEL} ${REPO_SITE_SEGMENT} ${HIDE_SITE}
rccode=$?
if [[ $rccode != 0 ]]
then
    printf "\n\n\t%s\n\n" "ERROR: promoteRepo.sh failed."
    exit $rccode
fi

# TODO: move this section to "promoteImpl". 
# TODO: might want to add if [[ "${HIDE_SITE}" != "true" ]] logic as we do for 
# deferedCompositeAdd script

# If all goes well, we create the "tag script", but don't actually run
# until we make the site visible, after doing sanity checking, etc.    

echo "#!/usr/bin/env bash" > deferedTag.sh
echo "# navigate to gitcache aggregator" >> deferedTag.sh 
echo "pushd ${BUILD_ROOT}/${AGGR_LOCATION}" >> deferedTag.sh
echo "" >> deferedTag.sh
echo "# DROP_ID == BUILD_ID, which should already exist as tag (for all I and M builds)" >> deferedTag.sh
echo "git tag -a -m \"${NEW_ANNOTATION}\" ${NEW_TAG} ${DROP_ID}" >> deferedTag.sh
echo "RC=$?" >> deferedTag.sh
echo "if [[ $RC != 0 ]]" >> deferedTag.sh
echo "then" >> deferedTag.sh
echo "   print \"/n/t%s/n\" \"ERROR: Failed to tag aggregator old id, ${DROP_ID}, with new tag, ${NEW_TAG} and annotation of ${NEW_ANNOTATION}.\"" >> deferedTag.sh
echo "   popd" >> deferedTag.sh
echo "   exit $RC" >> deferedTag.sh
echo "fi" >> deferedTag.sh
echo "git push origin tag ${NEW_TAG}" >> deferedTag.sh
echo "RC=$?" >> deferedTag.sh
echo "if [[ $RC != 0 ]]" >> deferedTag.sh
echo "then" >> deferedTag.sh
echo "   print \"/n/t%s/n\" \"ERROR: Failed to push new tag, ${NEW_TAG}.\"" >> deferedTag.sh
echo "   popd" >> deferedTag.sh
echo "   exit $RC" >> deferedTag.sh
echo "fi" >> deferedTag.sh
echo "popd" >> deferedTag.sh
chmod +x deferedTag.sh
echo "Remember to tag milestones with deferedTag.sh" >> "${CL_SITE}/checklist.txt"
#TODO: since HIDE_SITE was ${HIDE_SITE}" >> "${CL_SITE}/checklist.txt"



exit 0
