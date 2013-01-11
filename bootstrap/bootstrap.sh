#!/usr/bin/env bash

# Utility to get basic "startup" files for Platform's CBI Build

function usage () 
{
    printf "\n\t%s\n" "$( basename $0 ) <branch> <buildtype> <eclipseStream> where"
    printf "\t\t%s\t%s\n" "branch:" "branch to build, such as master, R4_2_mainenance, R3_8_maintenance"
    printf "\t\t%s\t%s\n" "buildtype:" "M, I, N"
    printf "\t\t%s\t%s\n" "eclipseStream: " "eclipse release being built, such as 4.3.0, 4.2.2, 3.8.2"
}

if [[ $# != 3 ]] 
then
    usage
    exit 1
fi

branch="${1}"
buildtype="${2}"
eclipseStream="${3}"

BUILD_HOME=${BUILD_HOME:-/shared/eclipse/builds}

reponame=eclipse.platform.releng.aggregator

#branch=master
#buildtype=N
#eclipseStream=4.3.0

#branch=master
#buildtype=I
#eclipseStream=4.3.0

#branch=R3_8_maintenance
#buildtype=M
#eclipseStream=3.8.2

#branch=R4_2_maintenance
#buildtype=M
#eclipseStream=4.2.2


# contrary to intuition (and previous behavior, bash 3.1) do NOT use quotes around right side of expression.
if [[ "${eclipseStream}" =~ ([[:digit:]]*)\.([[:digit:]]*)\.([[:digit:]]*) ]]
then
    eclipseStreamMajor=${BASH_REMATCH[1]}
    eclipseStreamMinor=${BASH_REMATCH[2]}
    eclipseStreamService=${BASH_REMATCH[3]}
else
    echo "eclipseStream, $eclipseStream, must contain major, minor, and service versions, such as 4.3.0"
    echo "    but found ${eclipseStream}"
    exit 1
fi

buildRoot=${buildRoot:-${BUILD_HOME}/${eclipseStreamMajor}${eclipseStreamMinor}${eclipseStreamService}${buildtype}}

echo "eclipseStream: $eclipseStream"
echo "eclipseStreamMajor: $eclipseStreamMajor"
echo "eclipseStreamMinor: $eclipseStreamMinor"
echo "eclipseStreamService: $eclipseStreamService"
echo "buildType: $buildType"
echo "buildRoot: $buildRoot"

# remove, if exists, from previous run
rm scripts.zip 2>/dev/null
rm ${reponame}-${branch} 2>/dev/null

# eventually may want to use tagged version of scripts, if not aggregator
# then the CGit URL for one file would have the form:
# http://git.eclipse.org/c/platform/eclipse.platform.releng.eclipsebuilder.git/plain/scripts/wgetFresh.sh?tag=vI20120417-0700
# (not sure about "repo" ... I think can just put in tag, for branch?) 

wget  --no-verbose -O scripts.zip http://git.eclipse.org/c/platform/${reponame}.git/snapshot/${reponame}-${branch}.zip 2>&1;
rc=$?
if [[ $rc != 0 ]]
then
    echo "wget failed: $rc"
    exit 1
fi

#remove any previous versions, to make sure completely fresh
rm -fr $buildRoot/scripts 2>/dev/null

mkdir -p $buildRoot
if [[ $? != 0 ]] 
then
    echo "Exiting, since could not make $buildRoot, as expected."
    exit 1
fi

# We only need the scripts directory, for this phase
unzip -o scripts.zip ${reponame}-${branch}/scripts* 
if [[ $? != 0 ]] 
then
    echo "Exiting, since could not unzip, as expected."
    exit 1
fi


mv ${reponame}-${branch}/scripts $buildRoot

if [[ $? != 0 ]] 
then
    echo "Exiting, since could not move, as expected."
    exit 1
fi

chmod +x $buildRoot/scripts/*.sh

if [[ $? != 0 ]] 
then
    echo "Could not chmod of scripts to executable. Running under wrong id?"
    exit 1
fi

rm scripts.zip
rm -fr ${reponame}-${branch} 
