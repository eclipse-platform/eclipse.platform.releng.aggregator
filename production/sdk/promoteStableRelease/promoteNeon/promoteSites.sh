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
export DROP_ID=I20160525-2000

# CHECKPOINT is the code for either milestone (M1, M2, ...) 
# or release candidate (RC1, RC2, ...). 
# It should be empty for the final release.
export CHECKPOINT=RC3

# This SIGNOFF_BUG should not be defined, if there are no errors in JUnit tests.
export SIGNOFF_BUG=494618

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

# NOTE: Normally these variables are computed automatically later in 
# the scripts. We provide these commented out "templates" here, since sometimes we may 
# want to include them at non-standard times, for example, we may want to 
# include "NEWS_ID" in an RC builds, whereas normally we do not. But for S and R 
# promotions, NEWS_ID is computed and included automatically. Note: per bug 495252
# we started including them automatically for S-*RC4 builds as well.
#export NEWS_ID=${BUILD_MAJOR}.${BUILD_MINOR}
#export ACK_ID=${BUILD_MAJOR}.${BUILD_MINOR}
#export README_ID=${BUILD_MAJOR}.${BUILD_MINOR}

export CL_SITE=${CL_SITE:-/shared/eclipse/sdk/promoteStableRelease/promote${TRAIN_NAME}}

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
# Note: this function has not been well tested. It usually does
# no harm, just to redo everything, unless changes made on download
# server drop site only.
#export INDEX_ONLY=false

# = = = = = = = Things past here seldom need to be updated
export PROMOTE_IMPL=/shared/eclipse/sdk/promoteStableRelease/promoteImpl
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
