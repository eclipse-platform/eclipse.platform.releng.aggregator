#!/usr/bin/env bash
#*******************************************************************************
# Copyright (c) 2016 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     David Williams - initial API and implementation
#*******************************************************************************

echo "#!/usr/bin/env bash" > "${CL_SITE}/deferredSteps.sh"

echo "" >> "${CL_SITE}/deferredSteps.sh"

echo "# As in main scripts, set WORKSPACE to run on Hudson or shared eclipse" >> "${CL_SITE}/deferredSteps.sh"

echo "if [[ -z \"${WORKSPACE}\" ]]" >> "${CL_SITE}/deferredSteps.sh"
echo "then" >> "${CL_SITE}/deferredSteps.sh"
echo "  export UTILITIES_HOME=/shared/eclipse" >> "${CL_SITE}/deferredSteps.sh"
echo "  export WORKSPACE=/shared/eclipse" >> "${CL_SITE}/deferredSteps.sh"
echo "else" >> "${CL_SITE}/deferredSteps.sh"
echo "  export UTILITIES_HOME=\${WORKSPACE}/utilities/production" >> "${CL_SITE}/deferredSteps.sh"
echo "fi" >> "${CL_SITE}/deferredSteps.sh"

echo "# We set DRYRUN to what ever the value was that produced these scripts as a reminder these won't work if DRYRUN was on." >> "${CL_SITE}/deferredSteps.sh"
echo "DRYRUN=${DRYRUN}" >> "${CL_SITE}/deferredSteps.sh"
echo "if [[ \"\${DRYRUN}\" == \"true\" ]]" >> "${CL_SITE}/deferredSteps.sh"
echo "then" >> "${CL_SITE}/deferredSteps.sh" 
echo "   echo \"DRYRUN was set, so exiting. Intended for visual inspection only.\"" >> "${CL_SITE}/deferredSteps.sh"
echo "   exit 1" >> "${CL_SITE}/deferredSteps.sh"
echo "fi" >> "${CL_SITE}/deferredSteps.sh"

#echo "# Utility to automate second, deferred step of a promotion (making visible, at the right time, etc.)" >> "${CL_SITE}/deferredSteps.sh"
#echo "" >> "${CL_SITE}/deferredSteps.sh"
#echo "# equinox is only promoted every 15-30 minutes ... so should do close to the hour, or half hour, " >> "${CL_SITE}/deferredSteps.sh"
#echo "# to avoid looking out of sync." >> "${CL_SITE}/deferredSteps.sh"
#echo "mv ${UTILITIES_HOME}/equinox/promotion/queue/manual-promote-${DL_LABEL_EQ}.sh ${UTILITIES_HOME}/equinox/promotion/queue/promote-${DL_LABEL_EQ}.sh" >> "${CL_SITE}/deferredSteps.sh"
#echo "" >> "${CL_SITE}/deferredSteps.sh"
echo "mv  /home/data/httpd/download.eclipse.org/equinox/drops/${DL_DROP_ID_EQ}/buildHidden" \
  "/home/data/httpd/download.eclipse.org/equinox/drops/${DL_DROP_ID_EQ}/buildHiddenORIG" >> "${CL_SITE}/deferredSteps.sh"
echo "mv /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4/${DL_DROP_ID}/buildHidden /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4/${DL_DROP_ID}/buildHiddenORIG" >> "${CL_SITE}/deferredSteps.sh"
echo "" >> "${CL_SITE}/deferredSteps.sh"
#echo "# variable, optional step (though, guess it doesn't hurt to always do it, just in case?) " >> "${CL_SITE}/deferredSteps.sh"
#echo "# Actually, though, would be best to do in 'first step', to confirm before visible." >> "${CL_SITE}/deferredSteps.sh"
#echo "touch /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4/${DL_DROP_ID}/overrideTestColor" >> "${CL_SITE}/deferredSteps.sh"
#echo "" >> "${CL_SITE}/deferredSteps.sh"
echo "\${UTILITIES_HOME}/sdk/updateIndexes.sh" >> "${CL_SITE}/deferredSteps.sh"
echo "" >> "${CL_SITE}/deferredSteps.sh"

echo "\${WORKSPACE}/${STAGE2DIRSEG}/deferredCompositeAdd.sh" >> "${CL_SITE}/deferredSteps.sh"

# We don't tag, during deferred step, when doing a release.
# It comes a little later.
if [[ "${DL_TYPE}" != "R" ]] 
then
  echo "" >> "${CL_SITE}/deferredSteps.sh"
  echo "\${WORKSPACE}/${STAGE2DIRSEG}/deferredTag.sh" >> "${CL_SITE}/deferredSteps.sh"
fi 
echo "" >> "${CL_SITE}/deferredSteps.sh"
echo "# In theory could automate the 'announce' mail too ... but, " >> "${CL_SITE}/deferredSteps.sh"
echo "# should do final 'sanity check' anyway, before " >> "${CL_SITE}/deferredSteps.sh"
echo "# sending mail, plus, nice to have small 'custom differences' in each, " >> "${CL_SITE}/deferredSteps.sh"
echo "# so not that useful to automate further? " >> "${CL_SITE}/deferredSteps.sh"

chmod +x "${CL_SITE}/deferredSteps.sh"

source ${PROMOTE_IMPL}/promoteUtilities.shsource
findEclipseExe ${DROP_ID}

if [[ "${HIDE_SITE}" != "true" ]]
then
  ${PROMOTE_IMPL}/runAntRunner.sh ${PROMOTE_IMPL}/addToComposite.xml addToComposite -Drepodir="/home/data/httpd/download.eclipse.org/eclipse/updates/${REPO_SITE_SEGMENT}/" -Dcomplocation="${DL_DROP_ID}"
else
  echo "#!/usr/bin/env bash" > ${CL_SITE}/deferredCompositeAdd.sh
  echo "export JAVA_CMD=$JAVA_CMD" >> ${CL_SITE}/deferredCompositeAdd.sh
  echo "export JAVA_EXEC_DIR=${JAVA_EXEC_DIR}" >> ${CL_SITE}/deferredCompositeAdd.sh
  echo "export ECLIPSE_EXE=${ECLIPSE_EXE}" >> ${CL_SITE}/deferredCompositeAdd.sh
  echo "${PROMOTE_IMPL}/runAntRunner.sh ${PROMOTE_IMPL}/addToComposite.xml addToComposite -Drepodir=/home/data/httpd/download.eclipse.org/eclipse/updates/${REPO_SITE_SEGMENT}/ -Dcomplocation=${DL_DROP_ID}" >> ${CL_SITE}/deferredCompositeAdd.sh
  chmod +x ${CL_SITE}/deferredCompositeAdd.sh
  echo "Remember to add to composite, by running deferredCompositeAdd.sh, since HIDE_SITE was ${HIDE_SITE}" >> "${CL_SITE}/checklist.txt"
fi

