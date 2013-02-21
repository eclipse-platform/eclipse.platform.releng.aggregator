#!/usr/bin/env bash

function createPromotionScriptEq () {

buildId=$1
if [[ -z "${buildId}" ]]
then
    echo "ERROR: this function requires buildId or label to promote"
    exit 1
fi

scriptName="promote-${buildId}.sh"

# remember, there is no 'downloads' segment in equinox locations,
# unlike the eclipse locations
buildRoot=/shared/eclipse/eclipse4I
siteDir=${buildRoot}/siteDir
equinoxPostingDirectory=${siteDir}/equinox/drops

# The 'workLocation' provides a handy central place to have the
# promote script, and log results. ASSUMING this works for all
# types of builds, etc (which is the goal for the sdk promotions).
workLocationEquinox=/shared/eclipse/equinox/promotion

# the cron job must know about and use this same
# location to look for its promotions scripts. (i.e. implicite tight coupling)
promoteScriptLocationEquinox=${workLocationEquinox}/queue

# directory should normally exist -- best to create with committer's ID --
# but in case not
mkdir -p "${promoteScriptLocationEquinox}"

eqFromDir=${equinoxPostingDirectory}/${buildId}
eqToDir="/home/data/httpd/download.eclipse.org/equinox/drops/"

# Note: for proper mirroring at Eclipse, we probably do not want/need to
# maintain "times" on build machine, but let them take times at time of copying.
# If it turns out to be important to maintain times (such as ran more than once,
# to pick up a "more" output, such as test results, then add -t to rsync
# Similarly, if download server is set up right, it will end up with the
# correct permissions, but if not, we may need to set some permissions first,
# then use -p on rsync

# Here is content of promtion script (note, use same ptimestamp created above):
echo "#!/usr/bin/env bash" >  ${promoteScriptLocationEquinox}/${scriptName}
echo "# promotion script created at $ptimestamp" >>  ${promoteScriptLocationEquinox}/${scriptName}
echo "rsync --recursive \"${eqFromDir}\" \"${eqToDir}\"" >> ${promoteScriptLocationEquinox}/${scriptName}

# we restrict "others" rights for a bit more security or safety from accidents
chmod -v ug=rwx,o-rwx ${promoteScriptLocationEquinox}/${scriptName}

}
