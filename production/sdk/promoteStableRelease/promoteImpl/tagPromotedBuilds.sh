#!/usr/bin/env bash

# TODO: might want to add if [[ "${HIDE_SITE}" != "true" ]] logic as we do for
# deferedCompositeAdd script

echo "#!/usr/bin/env bash" > deferedTag.sh
echo "# navigate to gitcache aggregator" >> deferedTag.sh
echo "pushd ${BUILD_ROOT}/${AGGR_LOCATION}" >> deferedTag.sh
echo "" >> deferedTag.sh
echo "# DROP_ID == BUILD_ID, which should already exist as tag (for all I and M builds)" >> deferedTag.sh
echo "git tag -a -m \"${NEW_ANNOTATION}\" ${NEW_TAG} ${DROP_ID}" >> deferedTag.sh
echo "RC=\$?" >> deferedTag.sh
echo "if [[ \$RC != 0 ]]" >> deferedTag.sh
echo "then" >> deferedTag.sh
echo "   print \"/n/t%s/n\" \"ERROR: Failed to tag aggregator old id, ${DROP_ID}, with new tag, ${NEW_TAG} and annotation of ${NEW_ANNOTATION}.\"" >> deferedTag.sh
echo "   popd" >> deferedTag.sh
echo "   exit \$RC" >> deferedTag.sh
echo "fi" >> deferedTag.sh
echo "git push origin tag ${NEW_TAG}" >> deferedTag.sh
echo "RC=\$?" >> deferedTag.sh
echo "if [[ \$RC != 0 ]]" >> deferedTag.sh
echo "then" >> deferedTag.sh
echo "   print \"/n/t%s/n\" \"ERROR: Failed to push new tag, ${NEW_TAG}.\"" >> deferedTag.sh
echo "   popd" >> deferedTag.sh
echo "   exit \$RC" >> deferedTag.sh
echo "fi" >> deferedTag.sh
echo "popd" >> deferedTag.sh
chmod +x deferedTag.sh
echo "Remember to tag milestones and RCs (but, not Releases) with deferedTag.sh" >> "${CL_SITE}/checklist.txt"
#TODO: since HIDE_SITE was ${HIDE_SITE}" >> "${CL_SITE}/checklist.txt"
