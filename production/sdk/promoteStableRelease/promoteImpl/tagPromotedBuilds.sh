#!/usr/bin/env bash
#*******************************************************************************
# Copyright (c) 2016 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     David Williams - initial API and implementation
#*******************************************************************************

# TODO: might want to add if [[ "${HIDE_SITE}" != "true" ]] logic as we do for
# deferredCompositeAdd script

# TODO: if another build has taken place (such as a PATCH build) we 
# may need to 'pull' aggegator first before we can push our tag

echo "#!/usr/bin/env bash" > ${CL_SITE}/deferredTag.sh
echo "# navigate to gitcache aggregator" >> ${CL_SITE}/deferredTag.sh
echo "pushd ${BUILD_ROOT}/${AGGR_LOCATION}" >> ${CL_SITE}/deferredTag.sh
echo "" >> ${CL_SITE}/deferredTag.sh
echo "# DROP_ID == BUILD_ID, which should already exist as tag (for all I and M builds)" >> ${CL_SITE}/deferredTag.sh
echo "git submodule foreach git tag -a -m \"${NEW_ANNOTATION}\" ${NEW_TAG} ${DROP_ID}" >> ${CL_SITE}/deferredTag.sh
echo "git tag -a -m \"${NEW_ANNOTATION}\" ${NEW_TAG} ${DROP_ID}" >> ${CL_SITE}/deferredTag.sh
echo "RC=\$?" >> ${CL_SITE}/deferredTag.sh
echo "if [[ \$RC != 0 ]]" >> ${CL_SITE}/deferredTag.sh
echo "then" >> ${CL_SITE}/deferredTag.sh
echo "   printf \"\n\t%s\n\" \"ERROR: Failed to tag aggregator old id, ${DROP_ID}, with new tag, ${NEW_TAG} and annotation of ${NEW_ANNOTATION}.\"" >> ${CL_SITE}/deferredTag.sh
echo "   popd" >> ${CL_SITE}/deferredTag.sh
echo "   exit \$RC" >> ${CL_SITE}/deferredTag.sh
echo "fi" >> ${CL_SITE}/deferredTag.sh
echo "git submodule foreach git push origin tag ${NEW_TAG}" >> ${CL_SITE}/deferredTag.sh
echo "git push origin tag ${NEW_TAG}" >> ${CL_SITE}/deferredTag.sh
echo "RC=\$?" >> ${CL_SITE}/deferredTag.sh
echo "if [[ \$RC != 0 ]]" >> ${CL_SITE}/deferredTag.sh
echo "then" >> ${CL_SITE}/deferredTag.sh
echo "   printf \"\n\t%s\n\" \"ERROR: Failed to push new tag, ${NEW_TAG}.\"" >> ${CL_SITE}/deferredTag.sh
echo "   popd" >> ${CL_SITE}/deferredTag.sh
echo "   exit \$RC" >> ${CL_SITE}/deferredTag.sh
echo "fi" >> ${CL_SITE}/deferredTag.sh
echo "popd" >> ${CL_SITE}/deferredTag.sh
chmod +x ${CL_SITE}/deferredTag.sh
echo "Remember to tag milestones and RCs (but, not Releases) with deferredTag.sh" >> "${CL_SITE}/checklist.txt"
