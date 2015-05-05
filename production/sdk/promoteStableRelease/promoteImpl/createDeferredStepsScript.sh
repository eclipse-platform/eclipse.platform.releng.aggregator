#!/usr/bin/env bash

# This is DL_DROP_ID for Eclipse. The one for equinox has DL_LABEL_EQ in middle.
DL_DROP_ID=${DL_TYPE}-${DL_LABEL}-${BUILD_TIMESTAMP}


echo "#!/usr/bin/env bash" >> "${CL_SITE}/deferredSteps.sh"
echo "" >> "${CL_SITE}/deferredSteps.sh"
echo "# Utility to automate second, deferred step of a promotion (making visible, at the right time, etc.)" >> "${CL_SITE}/deferredSteps.sh"
echo "" >> "${CL_SITE}/deferredSteps.sh"
echo "# equinox is only promoted every 30 minutes ... so should do close to the hour, or half hour, " >> "${CL_SITE}/deferredSteps.sh"
echo "# to avoid looking out of sync." >> "${CL_SITE}/deferredSteps.sh"
echo "mv /shared/eclipse/equinox/promotion/queue/manual-${DL_LABEL_EQ}.sh /shared/eclipse/equinox/promotion/queue/promote-${DL_LABEL_EQ}.sh" >> "${CL_SITE}/deferredSteps.sh"
echo "" >> "${CL_SITE}/deferredSteps.sh"
echo "mv /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4/${DL_DROP_ID}/buildHidden /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4/${DL_DROP_ID}/buildHiddenORIG" >> "${CL_SITE}/deferredSteps.sh"
echo "" >> "${CL_SITE}/deferredSteps.sh"
echo "# variable, optional step (though, guess it doesn't hurt to always do it, just in case?) " >> "${CL_SITE}/deferredSteps.sh"
echo "# Actually, though, would be best to do in 'first step', to confirm before visible." >> "${CL_SITE}/deferredSteps.sh"
echo "touch /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4/${DL_DROP_ID}/overrideTestColor" >> "${CL_SITE}/deferredSteps.sh"
echo "" >> "${CL_SITE}/deferredSteps.sh"
echo "/shared/eclipse/sdk/updateIndexes.sh" >> "${CL_SITE}/deferredSteps.sh"
echo "" >> "${CL_SITE}/deferredSteps.sh"
echo " # TODO: improve this location assumption, later." >> "${CL_SITE}/deferredSteps.sh"
echo " # assuming execution of this script is being done directly in the promoteLuna, or promoteMars directory" >> "${CL_SITE}/deferredSteps.sh"
echo "./deferredCompositeAdd.sh" >> "${CL_SITE}/deferredSteps.sh"
echo "" >> "${CL_SITE}/deferredSteps.sh"
echo "./deferredTag.sh" >> "${CL_SITE}/deferredSteps.sh"
echo "" >> "${CL_SITE}/deferredSteps.sh"
echo "# In theory could automate the 'announce' mail too ... but, " >> "${CL_SITE}/deferredSteps.sh"
echo "# should do final 'sanity check' anyway, before " >> "${CL_SITE}/deferredSteps.sh"
echo "# sending mail, plus, nice to have small 'custom differences' in each, " >> "${CL_SITE}/deferredSteps.sh"
echo "# so not that useful to automate further? " >> "${CL_SITE}/deferredSteps.sh"

chmod +x "${CL_SITE}/deferredSteps.sh"
