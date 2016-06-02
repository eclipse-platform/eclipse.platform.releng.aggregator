#!/usr/bin/env bash
#*******************************************************************************
# Copyright (c) 2013-2016 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     David Williams - initial API and implementation
#*******************************************************************************

# Utility to rename build and "promote" it to DL Server.


# DRYRUN should default it to do dry run first, 
# to sanity check files and labels created. And 
# then comment out to do the real thing.
export DRYRUN=dry-run

# These first three variables DROP_ID, CHECKPOINT, and SIGNOFF_BUG 
# change each time. 

# DROP_ID is the name of the build we are promoting. 
# That is, the FROM build. The TO name is computed from it, 
# and a few other variables, below. 
export DROP_ID=I20160602-0112

# CHECKPOINT is the code for either milestone (M1, M2, ...) 
# or release candidate (RC1, RC2, ...). 
# It should be empty for the final release.
export CHECKPOINT=RC4

# This SIGNOFF_BUG should not be defined, if there are no errors in JUnit tests.
export SIGNOFF_BUG=495252

# These remaining variables change less often, but do change
# for different development phases and streams.

# TRAIN_NAME is used for two things: 
# naming repo (that is, it's internal property name) and 
# the equinox download pages.
export TRAIN_NAME=Neon

# STREAM is the three digit release number, such as 4.7.0 or 4.6.1.
STREAM=4.6.0

# DL_TYPE ("download type") is the build type we are naming 
# the build *TO*
# For Maintenance, it is always 'M' (from M-build) until it's 'R'.
# for main line (master) code, it is always 'S' (from I-build) until it's 'R'
export DL_TYPE=S
#export DL_TYPE=R
#export DL_TYPE=M

# These are generic templates. Normally, in Hudson fields, can customize.
export INITIAL_MAIL_LINES="We are pleased to announce that ${TRAIN_NAME} ${CHECKPOINT} is available for download and updates."
export CLOSING_MAIL_LINES="Thank you to everyone who made this checkpoint possible."


# = = = = = = = This section/concept requires work
# Ordinarily, BUILD_LABEL (for Eclipse) and Equinox are the same. 
# But if we are promoting an "RC" site, then may be different, since 
# they were already promoted once.
# TODO: See "regex section" below to see if can be computed, even 
# in the RC case. That is where we set the ordinary case, 
# but RC case is not done yet.
#export DROP_ID_EQ=M-Mars.1RC3-201509040015
#export BUILD_LABEL_EQ=Mars.1RC3

# = = = = = = = This section/concept requires work
# INDEX_ONLY means that everything has been promoted once already
# and we merely want to "rename" and "promote" any new unit tests
# or performance tests that have completed since the initial promotion.
# Some ways it differs: If set, existing "site" (on build machine) is
# not deleted first. Only the main Eclipse site is effected, not
# equinox, not update site. None of the "deferred" stuff is set.
#export INDEX_ONLY=true
# We only ever check for 'true'
# Note: this function has not been well tested. It usually does
# no harm, just to redo everything, unless changes made on download
# server drop site only.
#export INDEX_ONLY=false


# This variable is for preparation to run on Hudson. Instead of from terminal. 
# If ran on Hudson, it is assumed the aggregator is checked out into a directory named
# "sdk". And, NOTE, there are some places in the scripts where /shared/eclipse
# is still required, such as "finding the build" for "finding the aggregator" for tagging.
export WORKSPACE=${WORKSPACE:-/shared/eclipse}

export PROMOTE_IMPL=${WORKSPACE}/sdk/promoteStableRelease/promoteImpl

# = = = = = = = Things past here seldom need to be updated
# and should be moved into promoteSitesCore.sh 


# One problem with old cl_site value is it "dirties" the working tree.
#export CL_SITE=${CL_SITE:-${WORKSPACE}/sdk/promoteStableRelease/promote${TRAIN_NAME}}
# stage 2 directory should be "outside" the normal working tree
export STAGE2DIRSEG=stage2output${TRAIN_NAME}${CHECKPOINT}
export CL_SITE=${WORKSPACE}/${STAGE2DIRSEG}
mkdir -p "${CL_SITE}"


# Currently this isn't used much, but is intended to be for "advanced debugging".
export TRACE_LOG=${CL_SITE}/traceLog.txt

if [[ "${STREAM}" =~ ^([[:digit:]]+)\.([[:digit:]]+)\.([[:digit:]]+)$ ]]
then
  export BUILD_MAJOR=${BASH_REMATCH[1]}
  export BUILD_MINOR=${BASH_REMATCH[2]}
  export BUILD_SERVICE=${BASH_REMATCH[3]}
else
  echo "STREAM must contain major, minor, and service versions, such as 4.3.0"
  echo "    but found ${STREAM}"
  exit 1
fi


# remove any of the scripts we create, such as for 'dry-run'.
# Notice the "verbose", but "ignore non-existent files"
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

"${PROMOTE_IMPL}/promoteSitesCore.sh" 2>&1 | tee ${CL_SITE}/stage1PromotionLog.txt
