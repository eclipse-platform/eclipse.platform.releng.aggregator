#!/usr/bin/env bash

# Utility to trigger the promotion of build. This utility just creates 
# a file to be executed by cron job. The actual promotion is done by files 
# in sdk directory of build machine. This "cron job approach" is required since
# a different user id must promote things to "downloads". The promotion scripts 
# also trigger the unit tests on Hudson.


# The 'workLocation' provides a handy central place to have the
# promote script, and log results. ASSUMING this works for all
# types of builds, etc (which is the goal for the sdk promotions).
workLocation=/shared/eclipse/sdk/promotion

# the cron job must know about and use this same
# location to look for its promotions scripts. (i.e. implicite tight coupling)
promoteScriptLocationEclipse=$workLocation/queue

# directory should normally exist -- best to create first, with committer's ID --
# but in case not
mkdir -p "${promoteScriptLocationEclipse}"

#always use TEST for now
testbuildonly=true
scriptName=promote-${eclipseStream}-${buildId}.sh
if [[ "${testbuildonly}" == "true" ]]
then
    # allows the "test" creation of promotion script, but, not have it "seen" be cron job
    scriptName=TEST-$scriptName
fi
# Here is content of promtion script:
ptimestamp=$( date +%Y%m%d%H%M )
echo "#!/usr/bin/env bash" >  ${promoteScriptLocationEclipse}/${scriptName}
echo "# promotion script created at $ptimestamp" >>  ${promoteScriptLocationEclipse}/${scriptName}
echo "$workLocation/syncDropLocation.sh $eclipseStream $buildId" >> ${promoteScriptLocationEclipse}/${scriptName}

# we restrict "others" rights for a bit more security or safety from accidents
chmod -v ug=rwx,o-rwx ${promoteScriptLocationEclipse}/${scriptName}

# no need to promote anything for 3.x builds
# (equinox portion should be the same, so we will
# create for equinox for for only 4.x primary builds)
if [[ $eclipseStream > 4 ]]
then
    # The 'workLocation' provides a handy central place to have the
    # promote script, and log results. ASSUMING this works for all
    # types of builds, etc (which is the goal for the sdk promotions).
    workLocationEquinox=/shared/eclipse/equinox/promotion

    # the cron job must know about and use this same
    # location to look for its promotions scripts. (i.e. tight coupling)
    promoteScriptLocationEquinox=${workLocationEquinox}/queue

    # directory should normally exist -- best to create with committer's ID --
    # but in case not
    mkdir -p "${promoteScriptLocationEquinox}"

    eqFromDir=${equinoxPostingDirectory}/${buildId}
    # fake location, for now
    eqToDir="/shared/eclipse/temp/eqdrops"
    #eqToDir="/home/data/httpd/download.eclipse.org/equinox/drops/"
    
    # Note: for proper mirroring at Eclipse, we probably do not want/need to
    # maintain "times" on build machine, but let them take times at time of copying.
    # If it turns out to be important to maintain times (such as ran more than once,
    # to pick up a "more" output, such as test results, then add -t to rsync
    # Similarly, if download server is set up right, it will end up with the
    # correct permissions, but if not, we may need to set some permissions first,
    # then use -p on rsync

    # Here is content of promtion script (note, use same ptimestamp created above):
    echo "#!/usr/bin/env bash" >  ${promoteScriptLocationEquinox}/${scriptName}
    echo "# promotion script created at $ptimestamp" >  ${promoteScriptLocationEquinox}/${scriptName}
    echo "rsync --recursive \"${eqFromDir}\" \"${eqToDir}\"" >> ${promoteScriptLocationEquinox}/${scriptName}

    # we restrict "others" rights for a bit more security or safety from accidents
    chmod -v ug=rwx,o-rwx ${promoteScriptLocationEquinox}/${scriptName}
else
    echo "Did not create promote script for equinox since $eclipseStream less than 4"
fi


echo "normal exit from build phase of $(basename $0)"

exit 0
