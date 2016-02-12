#!/usr/bin/env bash

# Utility to rename build and "promote" it to DL Server.

EXPORT DRYRUN=dry-run

# DROP_ID is the name of the build we are promoting. 
# That is, the FROM build. The TO name is computed from it, 
# and a few other variables, below. 
export DROP_ID=I20160128-2000

# checkpoint means either milestone or release candidate
# should be empty for final release
export CHECKPOINT=M5
# Used in naming repo and equinox download pages.
export TRAIN_NAME=Neon

export BUILD_MAJOR=4
export BUILD_MINOR=6
export BUILD_SERVICE=0

# These are what precedes main drop directory name -- 
# that is, for what we are naming the build TO
# For Maintenance, it's always 'M' (from M-build) until it's 'R'.
# for main line code, it's 'S' (from I-build) until it's 'R'
export DL_TYPE=S
#export DL_TYPE=R
#export DL_TYPE=M

export CL_SITE=${PWD}

# Ordinarily, BUILD_LABEL (for Eclipse) and Equinox are the same. 
# But if we are promoting an "RC" site, then may be different, since 
# they were already promoted once.
# TODO: See "regex section" below to see if can be computed, even 
# in the RC case. That is where we set the ordinary case, 
# but RC case is not done yet.
#export DROP_ID_EQ=M-Mars.1RC3-201509040015
#export BUILD_LABEL_EQ=Mars.1RC3

# INDEX_ONLY means that everything has been promoted once already
# and we merely want to "rename" and "promote" any new unit tests
# or performance tests that have completed since the initial promotion.
# Some ways it differs: If set, existing "site" (on build machine) is
# not deleted first. Only the main Eclipse site is effected, not
# equinox, not update site. None of the "deferred" stuff is set.
#export INDEX_ONLY=true
# We only ever check for 'true'
#export INDEX_ONLY=false

# = = = = = = = Things past here seldom need to be updated
export PROMOTE_IMPL=/shared/eclipse/sdk/promoteStableRelease/promoteImpl
export TRACE_LOG=${CL_SITE}/traceLog.txt

# remove any of the scripts we create, such as for 'dry-run', since some of them, 
# such as 'checklist' are normally appended to, not to mention better to start off 
# clean. Notice the "verbose", but "ignore non-existent files"
rm -vf ${CL_SITE}/*.txt ${CL_SITE}/deferred*

# regex section
# BUILD_TYPE is the prefix of the build -- 
# that is, for what we are renaming the build FROM
RCPATTERN="^([MI])-(${BUILD_MAJOR}\.${BUILD_MINOR}\.${BUILD_SERVICE}RC[12345]{1}[abcd]?)-([[:digit:]]{12})$"
PATTERN="^([MI])([[:digit:]]{8})-([[:digit:]]{4})$"
if [[ "${DROP_ID}" =~ $RCPATTERN ]]
then
  export BUILD_TYPE=${BASH_REMATCH[1]}
  export BUILD_LABEL=${BASH_REMATCH[2]}
  export BUILD_TIMESTAMP=${BASH_REMATCH[3]}
elif [[ "${DROP_ID}" =~ $PATTERN ]]
then
  export BUILD_TYPE=${BASH_REMATCH[1]}
  export BUILD_TIMESTAMP=${BASH_REMATCH[2]}${BASH_REMATCH[3]}
  # Label and ID are the same, in this case
  export BUILD_LABEL=$DROP_ID
  export BUILD_LABEL_EQ=$DROP_ID
  export DROP_ID_EQ=$DROP_ID
else 
  echo -e "\n\tERROR: DROP_ID, ${DROP_ID}, did not match any expected pattern."
  exit 1
fi

"${PROMOTE_IMPL}/promoteSitesCore.sh"
