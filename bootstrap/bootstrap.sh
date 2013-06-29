#!/usr/bin/env bash

# Utility to get basic "startup" files for Platform's CBI Build

function usage () 
{
    printf "\n\t%s\n" "$( basename $0 ) <BRANCH> <BUILD_TYPE> <STREAM> where"
    printf "\t\t%s\t%s\n" "BRANCH:" "BRANCH to build, such as master, R4_2_mainenance, R3_8_maintenance"
    printf "\t\t%s\t%s\n" "BUILD_TYPE:" "M, I, N"
    printf "\t\t%s\t%s\n" "STREAM: " "eclipse release being built, such as 4.3.0, 4.2.2, 3.8.2"
}

if [[ $# != 3 ]] 
then
    usage
    exit 1
fi

BRANCH=${BRANCH:-"${1}"}
BUILD_TYPE=${BUILD_TYPE:-"${2}"}
STREAM=${STREAM:-"${3}"}

BUILD_HOME=${BUILD_HOME:-/shared/eclipse/builds}

reponame=eclipse.platform.releng.aggregator

# contrary to intuition (and previous behavior, bash 3.1) do NOT use quotes around right side of expression.
if [[ "${STREAM}" =~ ([[:digit:]]*)\.([[:digit:]]*)\.([[:digit:]]*) ]]
then
    STREAMMajor=${BASH_REMATCH[1]}
    STREAMMinor=${BASH_REMATCH[2]}
    STREAMService=${BASH_REMATCH[3]}
else
    echo "STREAM must contain major, minor, and service versions, such as 4.3.0"
    echo "    but found ${STREAM}"
    exit 1
fi

if [[ ! "${BUILD_TYPE}" =~ [IMN] ]]
then
    echo "BUILD_TYPE must I,M, or N"
    echo "    but found ${BUILD_TYPE}"
    exit 1
fi

BUILD_ROOT=${BUILD_ROOT:-${BUILD_HOME}/${STREAMMajor}${BUILD_TYPE}}
echo "Exporting production scripts ... "
echo "  STREAM: $STREAM"
echo "  STREAMMajor: $STREAMMajor"
echo "  STREAMMinor: $STREAMMinor"
echo "  STREAMService: $STREAMService"
echo "  BUILD_TYPE: $BUILD_TYPE"
echo "  BUILD_ROOT: $BUILD_ROOT"
echo "  BUILD_HOME: $BUILD_HOME"
echo "  PRODUCTION_SCRIPTS_DIR: $PRODUCTION_SCRIPTS_DIR"

# We put these in build_root in case two builds get started at once. 
# (such as N and I build, both from master).  
TEMPZIPFILE=${STREAMMajor}${BUILD_TYPE}tempscripts.zip
TEMPFETCH=${reponame}-${BRANCH}
TEMPFETCHFOLDER=${BUILD_ROOT}/${TEMPFETCH}

# remove, if exists, from previous run
rm ${TEMPZIPFILE} 2>/dev/null
rm ${TEMPFETCHFOLDER} 2>/dev/null

echo "TEMPZIPFILE: ${TEMPZIPFILE}"
echo "TEMPFETCH: ${TEMPFETCH}"
echo "TEMPFETCHFOLDER ${TEMPFETCHFOLDER}"

# eventually may want to use tagged version of production scripts, if not aggregator
# then the CGit URL for one file would have the form:
# http://git.eclipse.org/c/platform/eclipse.platform.releng.eclipsebuilder.git/plain/production/wgetFresh.sh?tag=vI20120417-0700
# (not sure about "repo" ... I think can just put in tag, for BRANCH?) 

wget  --no-verbose -O ${TEMPZIPFILE} http://git.eclipse.org/c/platform/${reponame}.git/snapshot/${reponame}-${BRANCH}.zip 2>&1;
rc=$?
if [[ $rc != 0 ]]
then
    echo "wget failed: $rc"
    exit 1
fi

#remove any previous versions, to make sure completely fresh
rm -fr $BUILD_ROOT/${PRODUCTION_SCRIPTS_DIR} 2>/dev/null

# Make dir in case first run (note: this must be after the above "removes", 
# or else first time through they will remove this directory itself. 
mkdir -p ${BUILD_ROOT}
if [[ $? != 0 ]] 
then
    echo "Exiting, since could not make $BUILD_ROOT, as expected."
    exit 1
fi


# We only need the production scripts directory, for this phase
unzip  -o ${TEMPZIPFILE} ${TEMPFETCH}/${PRODUCTION_SCRIPTS_DIR}* -d ${BUILD_ROOT} > zipout.txt 
if [[ $? != 0 ]] 
then
    echo "Exiting, since could not unzip, as expected."
    exit 1
fi

# mv whole script directory "up" so directly under build_root, in constant place
mv ${BUILD_ROOT}/${TEMPFETCH}/${PRODUCTION_SCRIPTS_DIR} ${BUILD_ROOT}

if [[ $? != 0 ]] 
then
    echo "Exiting, since could not move, as expected."
    exit 1
fi

chmod +x ${BUILD_ROOT}/${PRODUCTION_SCRIPTS_DIR}/*.sh

if [[ $? != 0 ]] 
then
    echo "Could not chmod of production scripts to executable. Running under wrong id?"
    exit 1
fi

rm ${TEMPZIPFILE}
rm -fr ${TEMPFETCHFOLDER} 
