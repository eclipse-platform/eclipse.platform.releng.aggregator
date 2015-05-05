#!/usr/bin/env bash

# TODO: might want to add if [[ "${HIDE_SITE}" != "true" ]] logic as we do for
# deferredCompositeAdd script

echo "#!/usr/bin/env bash" > deferredTag.sh
echo "# navigate to gitcache aggregator" >> deferredTag.sh
echo "pushd ${BUILD_ROOT}/${AGGR_LOCATION}" >> deferredTag.sh
echo "" >> deferredTag.sh
echo "# DROP_ID == BUILD_ID, which should already exist as tag (for all I and M builds)" >> deferredTag.sh
echo "git tag -a -m \"${NEW_ANNOTATION}\" ${NEW_TAG} ${DROP_ID}" >> deferredTag.sh
echo "RC=\$?" >> deferredTag.sh
echo "if [[ \$RC != 0 ]]" >> deferredTag.sh
echo "then" >> deferredTag.sh
echo "   printf \"/n/t%s/n\" \"ERROR: Failed to tag aggregator old id, ${DROP_ID}, with new tag, ${NEW_TAG} and annotation of ${NEW_ANNOTATION}.\"" >> deferredTag.sh
echo "   popd" >> deferredTag.sh
echo "   exit \$RC" >> deferredTag.sh
echo "fi" >> deferredTag.sh
echo "git push origin tag ${NEW_TAG}" >> deferredTag.sh
echo "RC=\$?" >> deferredTag.sh
echo "if [[ \$RC != 0 ]]" >> deferredTag.sh
echo "then" >> deferredTag.sh
echo "   printf \"/n/t%s/n\" \"ERROR: Failed to push new tag, ${NEW_TAG}.\"" >> deferredTag.sh
echo "   popd" >> deferredTag.sh
echo "   exit \$RC" >> deferredTag.sh
echo "fi" >> deferredTag.sh
echo "popd" >> deferredTag.sh
chmod +x deferredTag.sh
echo "Remember to tag milestones and RCs (but, not Releases) with deferredTag.sh" >> "${CL_SITE}/checklist.txt"
#TODO: since HIDE_SITE was ${HIDE_SITE}" >> "${CL_SITE}/checklist.txt"
