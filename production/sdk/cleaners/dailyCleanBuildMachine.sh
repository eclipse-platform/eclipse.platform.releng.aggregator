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

# Utility to clean build machine

# DISABLED will prevent builds of that build type to not be cleaned up.
# This is needed such as during the 2 or 3 weeks that we have a release
# candidate, but do not publically make visible until the Simultaneious
# Release. In those cases, we want to keep the originally named build around,
# as well as any renamed versions of it. Valid values are the standard one
# character "build types" we use (I,M,N,P). Leave blank (or commented out)
# if nothing should be disabled.

DISABLED="M"

function removeOldPromotionScripts ()
{
    echo -e "\n\tRemove old promotion scripts."
    find /shared/eclipse/promotion/queue -name "RAN*" -ctime +4 -ls -exec rm '{}' \;
    find /shared/eclipse/promotion/queue -name "TEST*" -ctime +1 -ls -exec rm '{}' \;
    find /shared/eclipse/promotion/queue -name "ERROR*" -ctime +4 -ls -exec rm '{}' \;
    find /shared/eclipse/equinox/promotion/queue -name "RAN*" -ctime +4 -ls -exec rm '{}' \;
    find /shared/eclipse/equinox/promotion/queue -name "TEST*" -ctime +1 -ls -exec rm '{}' \;
    find /shared/eclipse/equinox/promotion/queue -name "ERROR*" -ctime +4 -ls -exec rm '{}' \;
    # The job on Hudson that creates these files also cleans them up when over 2 days old.
    # find /shared/eclipse/testjobqueue -name "RAN*" -ctime +3 -ls -exec rm '{}' \;
    # find /shared/eclipse/testjobqueue -name "TEST*" -ctime +1 -ls -exec rm '{}' \;
    # find /shared/eclipse/testjobqueue -name "ERROR*" -ctime +4 -ls -exec rm '{}' \;
}

function removeOldDirectories ()
{
    rootdir=$1
    ctimeAge=$2
    pattern=$3
    echo -e "\n\tCleaning rootdir: ${rootdir}"
    if [[ -e "${rootdir}" ]]
    then
        # leave at least one, on build machine.
        # TODO: this may work most of the time, since ran daily, but the correct
        # fix is work with the list. This will delete all, on some occasions.
        # Also, for build machine, it is really only I and M builds that we want to leave one in place.
        count=$( find "${rootdir}" -maxdepth 1 -type d -ctime ${ctimeAge} -name "${pattern}"  | wc -l )
        if [[ $count -gt 1 ]]
        then
            find "${rootdir}" -maxdepth 1 -type d -ctime ${ctimeAge} -name "${pattern}" -ls -exec rm -fr '{}' \;
        fi
    else
        echo -e "\t\tINFO: rootdir did not exist."
    fi
}

function removeBuildFamily ()
{
    buildmachine=$1
    major=$2
    minor=$3
    days=$4
    buildType=$5

    if [[ ${buildType} =~ .*[${DISABLED}].* ]]
    then
        echo -e "\n\t${buildType} was on the DISABLED list, so not cleaned up\n"
    else
        basedir="/shared/eclipse/${buildmachine}/${major}${buildType}/siteDir"
        chkdir="eclipse/downloads/drops4"
        removeOldDirectories "${basedir}/${chkdir}" "${days}" "${buildType}20*"
        chkdir="equinox/drops"
        removeOldDirectories "${basedir}/${chkdir}" "${days}" "${buildType}20*"
        chkdir="updates/${major}.${minor}-${buildType}-builds"
        removeOldDirectories "${basedir}/${chkdir}" "${days}" "${buildType}20*"
    fi
}

function cleanBuildMachine ()
{

    buildmachine=$1

    echo -e "\n\tDaily clean of ${buildmachine} build machine on $(date )\n"
    echo -e "\tRemember to "turn off" when M build or I build needs to be deferred promoted,"
    echo -e "\tsuch as for "quiet week".\n"


    INUSE_BEFORE=$(nice -12 du /shared/eclipse/${buildmachine} -sh)

    major=4

    minor=7
    days="+4"
    buildType=P
    removeBuildFamily ${buildmachine} ${major} ${minor} ${days} ${buildType}

    minor=7
    days="+2"
    buildType=N
    removeBuildFamily ${buildmachine} ${major} ${minor} ${days} ${buildType}

    minor=7
    days="+4"
    buildType=I
    removeBuildFamily ${buildmachine} ${major} ${minor} ${days} ${buildType}

    minor=6
    days="+4"
    buildType=M
    removeBuildFamily ${buildmachine} ${major} ${minor} ${days} ${buildType}

    # This function cleaned up promotion queues on 
    # /shared/eclipse/
    # which shoudl no longer be needed, since we "promote" from 
    # Hudson, which cleans itself up, so to speak. 
    # so we can remove this function. Will simply comment it 
    # out, for now. 
    # removeOldPromotionScripts

    INUSE_AFTER=$(nice -12 du /shared/eclipse/${buildmachine} -sh)

    echo -e "\n\tDisk used before cleaning: $INUSE_BEFORE"
    echo -e "\tDisk used after cleaning: $INUSE_AFTER"
}

cleanBuildMachine builds


