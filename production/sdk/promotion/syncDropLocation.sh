#!/usr/bin/env bash

SCRIPTDIR=$( dirname $0 )
echo "SCRIPTDIR: ${SCRIPTDIR}"
source ${SCRIPTDIR}/syncUpdateUtils.shsource

# this buildeclipse.shsource file is to ease local builds to override some variables. 
# It should not be used for production builds.
source buildeclipse.shsource 2>/dev/null

function sendPromoteMail ()
{

    SITE_HOST=${SITE_HOST:-download.eclipse.org}

    echo "     Starting sendPromoteMail"
    eclipseStream=$1
    if [[ -z "${eclipseStream}" ]]
    then
        printf "\n\n\t%s\n\n" "ERROR: Must provide eclipseStream as first argument, for this function $(basename $0)"
        return 1;
    fi
    echo "     eclipseStream: ${eclipseStream}"

    buildId=$2
    if [[ -z "${buildId}" ]]
    then
        printf "\n\n\t%s\n\n" "ERROR: Must provide buildId as second argument, for this function $(basename $0)"
        return 1;
    fi
    echo "     buildId: ${buildId}"

    BUILD_KIND=$3
    if [[ -z "${BUILD_KIND}" ]]
    then
        printf "\n\n\t%s\n\n" "ERROR: Must provide BUILD_KIND as third argumnet, for this function $(basename $0)"
        return 1;
    fi
    echo "     BUILD_KIND: ${BUILD_KIND}"

    # optional? Or blank? 
    BUILD_FAILED=$4


    eclipseStreamMajor=${eclipseStream:0:1}
    buildType=${buildId:0:1}
    echo "     buildType: ${buildType}"

    # ideally, the user executing this mail will have this special file in their home directory,
    # that can specify a custom 'from' variable, but still you must use your "real" ID that is subscribed
    # to the wtp-dev mailing list
    #   set from="\"Your Friendly WTP Builder\" <real-subscribed-id@real.address>"
    # correction ... doesn't work. Seems the subscription system set's the "from" name, so doesn't work when
    # sent to mail list (just other email addresses)
    # espeically handy if send from one id (e.g. "david_williams)
    # only good with 'mail', not 'sendmail'
    #export MAILRC=~/.e4Buildmailrc

    # common part of URL and file path
    # varies by build stream
    # examples of end result:
    # http://download.eclipse.org/eclipse/downloads/drops4/N20120415-2015/
    # /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4/N20120415-2015


    mainPath=$( dlToPath "$eclipseStream" "$buildId" "$BUILD_KIND" )
    echo "     mainPath: $mainPath"
    if [[ "$mainPath" == 1 ]]
    then
        printf "\n\n\t%s\n\n" "ERROR: mainPath could not be computed."
        return 1
    fi

    downloadURL=http://${SITE_HOST}/${mainPath}/${buildId}/

    if [[ -n "${BUILD_FAILED}" ]] 
    then
        EXTRA_SUBJECT_STRING=" - BUILD FAILED"
    else
        EXTRA_SUBJECT_STRING=""
    fi

    if [[ -n "${POM_UPDATES}" ]] 
    then
        EXTRA_SUBJECT_STRING="${EXTRA_SUBJECT_STRING} - POM UPDATES REQUIRED"
    fi

    if [[ "${BUILD_KIND}" == "CBI" ]]
    then 
        # 4.3.0 Build: I20120411-2034
        SUBJECT="${eclipseStream} ${buildType}-Build: ${buildId} $EXTRA_SUBJECT_STRING"
    else
        # 4.3.0 Build: I20120411-2034
        SUBJECT="${BUILD_KIND} based ${eclipseStream} ${buildType}-Build: ${buildId} $EXTRA_SUBJECT_STRING"
    fi

    # override in buildeclipse.shsource if doing local tests
    TO=${TO:-"platform-releng-dev@eclipse.org"}

    # for initial testing, only to me -- change as desired after initial testing. 
    if [[ "${buildId}" =~ [PYX] ]]
    then 
        TO="david_williams@us.ibm.com"
    fi

    FROM=${FROM:-"e4Builder@eclipse.org"}

    # make sure reply to goes back to the list
    # I'm not positive this is required for mailing list?
    # does anything "from" list, automatically get reply-to: list?
    #REPLYTO="platform-releng-dev@eclipse.org"
    #we could? to "fix up" TODIR since it's in file form, not URL
    # URLTODIR=${TODIR##*${DOWNLOAD_ROOT}}

    message1=""

    #TODO: later can use sed, form a proper list
    if [[ -n "${POM_UPDATES}" ]]
    then 
        message1="$message1 <p>POM Update Required: ${downloadURL}/pom_updates/</p>\n"
    fi

    message1="$message1 <p>Eclipse downloads: ${downloadURL}</p>\n"


    # Do not include repo, if build failed
    if [[ -z "${BUILD_FAILED}" ]]
    then 
        message1="$message1 <p>Software site repository: http://${SITE_HOST}/eclipse/updates/${eclipseStreamMajor}.${eclipseStreamMinor}-${buildType}-builds</p>\n"
    fi

    # Do not include Equinox, if build failed, or if patch or experimental build
    if [[ -z "${BUILD_FAILED}" && ! "${buildId}" =~ [PYX]  ]]
    then 
        message1="$message1 <p>Equinox downloads: http://${SITE_HOST}/equinox/drops/${buildId}</p>\n"
    fi

    if [[ "${BUILD_KIND}" == "CBI" && "${buildId}" =~ [NMI] ]]
    then 
        (
        echo "To: ${TO}"
        echo "From: ${FROM}"
        echo "MIME-Version: 1.0"
        echo "Content-Type: text/html; charset=utf-8"
        echo "Subject: ${SUBJECT}"
        echo "<html><body>"
        echo -e "${message1}"
        echo "</body></html>"
        ) | /usr/lib/sendmail -t
    elif [[ "${BUILD_KIND}" == "CBI" && "${buildId}" =~ [PYX] ]]
    then
        (
        echo "To: ${TO}"
        echo "From: ${FROM}"
        echo "MIME-Version: 1.0"
        echo "Content-Type: text/html; charset=utf-8"
        echo "Subject: Experimental: ${SUBJECT}"
        echo "<html><body>"
        echo -e "${message1}"
        echo "</body></html>"
        ) | /usr/lib/sendmail -t
    fi
    echo "mail sent for $eclipseStream $buildType-build $buildId"
    return 0
}


# start tests function
function startTests()
{
    echo "startTests()"
    eclipseStreamMajor=$1
    buildType=$2
    eclipseStream=$3
    buildId=$4
    BUILD_KIND=$5
    EBUILDER_HASH=$6
    if [[ -z "${EBUILDER_HASH}" ]]
    then
        printf "\n\n\t%s\n\n" "ERROR: Must provide builder (or aggregator) hash as fourth argumnet, for this function $(basename $0)"
        return 1;
    fi

    echo "eclipseStreamMajor: $eclipseStreamMajor"
    echo "buildType: $buildType"
    echo "eclipseStream: $eclipseStream"
    echo "buildId: $buildId"
    echo "BUILD_KIND: $BUILD_KIND"
    echo "EBUILDER_HASH: $EBUILDER_HASH"
    if [[ "${BUILD_KIND}" == 'CBI' ]]
    then 
        BUILD_ROOT=${BUILD_HOME}/${eclipseStreamMajor}${buildType}
        eclipsebuilder=eclipse.platform.releng.aggregator/production/testScripts
        dlFromPath=$( dlFromPath $eclipseStream $buildId $BUILD_KIND )
        echo "DEBUG CBI dlFromPath: $dlFromPath"
        buildDropDir=${BUILD_ROOT}/siteDir/$dlFromPath/${buildId}
        echo "DEBGUG CBI buildDropDir: $buildDropDir"
        builderDropDir=${buildDropDir}/${eclipsebuilder}
        echo "DEBUG: CBI builderDropDir: ${builderDropDir}"
    elif [[ "${BUILD_KIND}" == 'PDE' ]]
    then
        BUILD_ROOT=/shared/eclipse/eclipse${eclipseStreamMajor}${buildType}
        buildDir=${BUILD_ROOT}/build
        supportDir=${buildDir}/supportDir
        eclipsebuilder=org.eclipse.releng.eclipsebuilder
        builderDir=${supportDir}/$eclipsebuilder
        # assumed in fixed location, for now, for PDE builds
        builderDropDir=/shared/eclipse/sdk/promotion
        echo "DEBUG: PDE builderDropDir for PDE: ${builderDropDir}"
        dlFromPath=$( dlFromPath $eclipseStream $buildId $BUILD_KIND )
        echo "DEBUG: PDE dlFromPath: $dlFromPath"
        buildDropDir=${BUILD_ROOT}/siteDir/$dlFromPath/${buildId}
        echo "DEBUG: PDE builderDropDir: ${builderDropDir}"
    else
        echo "ERROR. Unrecognized value of BUILD_KIND: $BUILD_KIND"
    fi  

    # finally, execute 
    ${builderDropDir}/startTests.sh ${eclipseStream} ${buildId} ${BUILD_KIND} ${EBUILDER_HASH}
}

# this funtion currently syncs local repo on build machine, and adds 
# it to composite, on download server.
function syncRepoSite ()
{
    eclipseStream=$1
    if [[ -z "${eclipseStream}" ]]
    then
        printf "\n\n\t%s\n\n" "ERROR: Must provide eclipseStream as first argumnet, for this function $(basename $0)"  >&2
        return 1;
    fi


    buildType=$2
    if [[ -z "${buildType}" ]]
    then
        printf "\n\n\t%s\n\n" "ERROR: Must provide buildType as second argumnet, for this function $(basename $0)" >&2
        return 1;
    fi

    BUILD_KIND=$3
    if [[ -z "${BUILD_KIND}" ]]
    then
        printf "\n\n\t%s\n\n" "ERROR: Must provide BUILD_KIND as third argumnet, for this function $(basename $0)" >&2
        return 1;
    fi

    # contrary to intuition (and previous behavior, bash 3.1) do NOT use quotes around right side of expression.
    if [[ "${eclipseStream}" =~ ^([[:digit:]]+)\.([[:digit:]]+)\.([[:digit:]]+)$ ]]
    then
        eclipseStreamMajor=${BASH_REMATCH[1]}
        eclipseStreamMinor=${BASH_REMATCH[2]}
        eclipseStreamService=${BASH_REMATCH[3]}
    else
        echo "eclipseStream, $eclipseStream, must contain major, minor, and service versions, such as 4.2.0" >&2
        return 1
    fi

    fromDir=$(updateSiteOnBuildDir "$eclipseStream" "$buildId" "$BUILD_KIND") 
    toDir=$(updateSiteOnDL "$eclipseStream" "$buildId" "$BUILD_KIND") 
    #toDir="/home/data/httpd/download.eclipse.org/eclipse/updates/4.3-builds"

    if [[ -n "${fromDir}" && -d "${fromDir}" && -n "${toDir}" && -d "${toDir}" ]]
    then
        rsync --times --omit-dir-times --recursive "${fromDir}" "${toDir}"
        RC=$?
    else
        RC=9
    fi

    if [[ $RC != 0 ]] 
    then
        echo "ERROR: rsync failed. RC: $RC" >&2
        echo "   In syncRepoSite" >&2
        echo "fromDir: $fromDir" >&2
        echo "toDir: $toDir" >&2
        return $RC
    fi
    # update composite!
    # add ${buildId} to {toDir}

    # runAntRunner requires basebuilder to be installed at drop site, so we'll check here if it exists yet, 
    # and if not, fetch it.

    dropFromBuildDir=$( dropFromBuildDir "$eclipseStream" "$buildId" "$BUILD_KIND" )
    EBuilderDir=$dropFromBuildDir/eclipse.platform.releng.aggregator/eclipse.platform.releng.tychoeclipsebuilder

    # assume ant is on the path
    ant -f $EBuilderDir/eclipse/getBaseBuilderAndTools.xml -DWORKSPACE=$dropFromBuildDir

    ${SCRIPTDIR}/runAntRunner.sh ${buildId} ${eclipseStream} ${SCRIPTDIR}/addToComposite.xml addToComposite -Drepodir=${toDir} -Dcomplocation=${buildId}
    RC=$?
    return $RC
}



# this is the single script to call that "does it all" to promote build
# to update site, drop site, update index page on downlaods, and send mail to list.
# it requires four arguments
#    eclipseStream (e.g. 4.2 or 3.8)
#    buildId       (e.g. N20120415-2015)
#    BUILD_KIND    (CBI or special value)
#    EBUILDER_HASH (SHA1 HASH or branch of eclipse builder to used

if [[ $# < 4 ]]
then
    # usage:
    scriptname=$(basename $0)
    printf "\n\t%s\n" "This script, $scriptname requires four arguments, in order: "
    printf "\t\t%s\t%s\n" "eclipseStream" "(e.g. 4.2.2 or 3.8.2) "
    printf "\t\t%s\t%s\n" "buildId" "(e.g. N20120415-2015) "
    printf "\t\t%s\t%s\n" "BUILD_KIND" "(e.g. PDE or special cased value) "
    printf "\t\t%s\t%s\n" "EBUILDER_HASH" "(SHA1 HASH for eclipe builder used) "
    printf "\t%s\n" "for example,"
    printf "\t%s\n\n" "./$scriptname 4.2 N N20120415-2015 CBI master"
    exit 1
fi

echo "Starting $0"

# = = = = = = = = =  argument checks
eclipseStream=$1
if [[ -z "${eclipseStream}" ]]
then
    printf "\n\n\t%s\n\n" "ERROR: Must provide eclipseStream as first argumnet, for this function $(basename $0)"
    exit 1
fi
echo "eclipseStream: $eclipseStream"

buildId=$2
if [[ -z "${buildId}" ]]
then
    printf "\n\n\t%s\n\n" "ERROR: Must provide buildId as second argumnet, for this function $(basename $0)"
    exit 1
fi
echo "buildId: $buildId"

BUILD_KIND=$3
if [[ -z "${BUILD_KIND}" ]]
then
    printf "\n\n\t%s\n\n" "ERROR: Must provide BUILD_KIND as third argumnet, for this function $(basename $0)"
    exit 1
fi
echo "BUILD_KIND: $BUILD_KIND"

#TODO: asssume master for now, if unspecified. But should tighten up to through error as scripts get finished. 
EBUILDER_HASH=$4
if [[ -z "${EBUILDER_HASH}" ]]
then
    printf "\n\n\t%s\n\n" "WARNING: Must provide builder (or aggregator) hash as fourth argumnet, for this function, $0"
    #printf "\n\n\t%s\n\n" "ERROR: Must provide builder (or aggregator) hash as fourth argumnet, for this function, $0"
    #exit 1;
fi
echo "EBUILDER_HASH: $EBUILDER_HASH"

# we get all build variables here. Currently need it to check if BUILD_FAILED is defined. 
# if BUILD FAILED, we still "publish", but dont' update bad repo nor start tests
BUILD_ENV_FILE=$5 
source $BUILD_ENV_FILE

eclipseStreamMajor=${eclipseStream:0:1}
buildType=${buildId:0:1}
echo "buildType: $buildType"

# contrary to intuition (and previous behavior, bash 3.1) do NOT use quotes around right side of expression.
if [[ "${eclipseStream}" =~ ^([[:digit:]]+)\.([[:digit:]]+)\.([[:digit:]]+)$ ]]
then
    eclipseStreamMajor=${BASH_REMATCH[1]}
    eclipseStreamMinor=${BASH_REMATCH[2]}
    eclipseStreamService=${BASH_REMATCH[3]}
else
    echo "eclipseStream, $eclipseStream, must contain major, minor, and service versions, such as 4.2.0"
    exit 1
fi

echo "eclipseStream: $eclipseStream"
echo "eclipseStreamMajor: $eclipseStreamMajor"
echo "eclipseStreamMinor: $eclipseStreamMinor"
echo "eclipseStreamService: $eclipseStreamService"
echo "BUILD_KIND: $BUILD_KIND"
echo "buildType: $buildType"
echo "BUILD_ENV_FILE: $BUILD_ENV_FILE"

# = = = = = = = = = 
# compute dirctiory on build machine
dropFromBuildDir=$( dropFromBuildDir "$eclipseStream" "$buildId" "$BUILD_KIND" )
echo "dropFromBuildDir: $dropFromBuildDir"
if [[ ! -d "${dropFromBuildDir}" ]]
then 
    echo "ERROR: the expected dropFromBuildDir (drop directory) did not exist"
    echo "       drop directory: ${dropFromBuildDir}"
    exit 1
fi
SCRIPTDIR=$( dirname $0 )
echo "SCRIPTDIR: ${SCRIPTDIR}"
${SCRIPTDIR}/getEBuilder.sh "${BUILD_KIND}" "${EBUILDER_HASH}" "${dropFromBuildDir}"

# if build failed, don't promote repo
if [[ -z "$BUILD_FAILED" ]]
then 
    syncRepoSite "$eclipseStream" "$buildType" "$BUILD_KIND" 
    rccode=$?
    if [[ $rccode != 0 ]]
    then
        printf "\n\n\t%s\n\n"  "ERROR: something went wrong putting repo on download site. Rest of promoting build halted."
        exit 1
    fi
else 
    echo "Repository site not updated since BUILD FAILED"
fi

# We still update drop location, even if failed, just to get the logs up there on downloads
syncDropLocation "$eclipseStream" "$buildId" "$BUILD_KIND" "${EBUILDER_HASH}"
rccode=$?
if [[ $rccode != 0 ]]
then
    printf "\n\n\t%s\n\n" "ERROR occurred during promotion to download server, so halted promotion and did not send mail."
    exit 1
fi

# if build failed, don't run tests ... they'll fail right away
if [[ -z "$BUILD_FAILED" ]]
then 
    # if update to downloads succeeded, start the unit tests on Hudson
    startTests $eclipseStreamMajor $buildType $eclipseStream $buildId $BUILD_KIND ${EBUILDER_HASH}
fi

sendPromoteMail "$eclipseStream" "$buildId" "$BUILD_KIND" "$BUILD_FAILED"
rccode=$?
if [[ $rccode != 0 ]]
then
    printf "\n\n\t%s\n\n" "ERROR occurred during sending final mail to list"
    exit 1
fi

exit 0

