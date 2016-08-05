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
# This promoteSites.sh script is primarily to collect (and validate) the needed values, 
# and then calls promoteSitesCore.sh to initiate the main work (which calls 
# many other scripts). 

# This has been designed to work "on Hudson" or from terminal console -- 
# but does assume the builds and build repository on on "/shared/eclipse/...". 
# On Hudson, the job must be defined as a parameterized job, with the parameters 
# in this file defined with the same names as in this file. 
# From a command line, the script can be ran, or tested, with something similar to 
#  DROP_ID=I20160602-0112 CHECKPOINT=RC4 SIGNOFF_BUG="" TRAIN_NAME=Neon STREAM=4.6.0 DL_TYPE=S DRYRUN=false ./promoteSites.sh


# These first three variables DROP_ID, CHECKPOINT, and SIGNOFF_BUG 
# change each time. 

# DROP_ID is the name (build id) of the build we are promoting. 
# That is, the FROM build. The TO name is computed from it, 
# and a few other variables. 
if [[ -z "${DROP_ID}" ]]
then
  echo -e "\n\t[ERROR] DROP_ID must be defined for ${0##*/}"
  exit 1
else
  export DROP_ID
  echo -e "\n\t[INFO] DROP_ID: $DROP_ID"  
fi

# CHECKPOINT is the code for either milestone (M1, M2, ...) 
# or release candidate (RC1, RC2, ...). 
# It should be empty for the final release.
if [[ -z "${CHECKPOINT}" ]]
then
  echo -e "\n\t[WARNING] CHECKPOINT was blank in ${0##*/}"
else
  export CHECKPOINT
  echo -e "\n\t[INFO] CHECKPOINT: $CHECKPOINT"  
fi

# This SIGNOFF_BUG should not be defined, if there are no errors in JUnit tests.
if [[ -z "${SIGNOFF_BUG}" ]]
then
  echo -e "\n\t[INFO] SIGNOFF_BUG was not defined. That is valid if no Unit Tests failures but otherwise should be defined."
  echo -e "\t\tCan be added by hand to buildproperties.php"
  exit 1
else
  export SIGNOFF_BUG
  echo -e "\t\t[INFO] SIGNOFF_BUG: $SIGNOFF_BUG"
fi

# These remaining variables change less often, but do change
# for different development phases and streams.

# TRAIN_NAME is used for two things: 
# naming repo (that is, it's internal property name) and 
# the equinox download pages.
if [[ -z "${TRAIN_NAME}" ]]
then
  echo -e "\n\t[ERROR] TRAIN_NAME must be defined for ${0##*/}"
  exit 1
else
  export TRAIN_NAME
  echo -e "\n\t[INFO] TRAIN_NAME: $TRAIN_NAME"  
fi

# STREAM is the three digit release number, such as 4.7.0 or 4.6.1.
# STREAM=4.6.0
if [[ -z "${STREAM}" ]]
then
  echo -e "\n\t[ERROR] STREAM must be defined for ${0##*/}"
  exit 1
else
  export STREAM
  echo -e "\n\t[INFO] STREAM: $STREAM"  
fi

# DL_TYPE ("download type") is the build type we are naming 
# the build *TO*
# For Maintenance, it is always 'M' (from M-build) until it's 'R'.
# for main line (master) code, it is always 'S' (from I-build) until it's 'R'
#export DL_TYPE=S
#export DL_TYPE=R
#export DL_TYPE=M
if [[ -z "${DL_TYPE}" ]]
then
  echo -e "\n\t[ERROR] DL_TYPE must be defined for ${0##*/}"
  exit 1
else
  # Could probably define default - or validate! - based on first letter of DROP_ID
  # M --> M
  # I --> S
  export DL_TYPE
  echo -e "\n\t[INFO] DL_TYPE: $DL_TYPE"  
fi

# These are generic templates. Normally, in Hudson fields, can customize.
# But if nothing is assigned, will use these generic ones.
if [[ -z "${INITIAL_MAIL_LINES}" ]]
then
  export INITIAL_MAIL_LINES="We are pleased to announce that ${TRAIN_NAME} ${CHECKPOINT} is available for download and updates."
else
  export INITIAL_MAIL_LINES
fi

if [[ -z "${CLOSING_MAIL_LINES}" ]]
then
  export CLOSING_MAIL_LINES="Thank you to everyone who made this checkpoint possible."
else
  export CLOSING_MAIL_LINES
fi

echo -e "\n\t[INFO] INITIAL_MAIL_LINES: $INITIAL_MAIL_LINES"  
echo -e "\n\t[INFO] CLOSING_MAIL_LINES: $CLOSING_MAIL_LINES"  


# DRYRUN should default it to do dry run first, 
# to sanity check files and labels created. And 
# then set to false to do the real thing.
# Hudson will pass in "true" or "false" depending on if 
# checkbox if checked, or not, so if we find no value, 
# assume "true".
if [[ -z "${DRYRUN}" ]]
then
  export DRYRUN=true
  echo -e "\n\t[INFO] DRYRUN found undefined, so set it to 'true'"
else
  export DEBUG
  echo -e "\n\t[INFO] DRYRUN was $DRYRUN"
fi


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

# = = = = = = = Things past here seldom need to be updated

# This variable is for preparation to run on Hudson. Instead of from terminal. 
# If ran on Hudson, it is assumed the aggregator is checked out into a directory named
# "sdk". And, NOTE, there are some places in the scripts where /shared/eclipse
# is still required, such as "finding the build" for "finding the aggregator" for tagging.

if [[ -z "${WORKSPACE}" ]]
then
  export UTILITIES_HOME=/shared/eclipse
  export WORKSPACE=/shared/eclipse
else
  export UTILITIES_HOME=${WORKSPACE}/utilities/production
fi

export PROMOTE_IMPL=${UTILITIES_HOME}/sdk/promoteStableRelease/promoteImpl

# One problem with old cl_site value is it "dirties" the working tree.
#export CL_SITE=${CL_SITE:-${UTILITIES_HOME}/sdk/promoteStableRelease/promote${TRAIN_NAME}}
# stage 2 directory should be "outside" the normal working tree
export STAGE2DIRSEG=stage2output${TRAIN_NAME}${CHECKPOINT}
export CL_SITE=${WORKSPACE}/${STAGE2DIRSEG}
mkdir -p "${CL_SITE}"

"${PROMOTE_IMPL}/promoteSitesCore.sh" 2>&1 | tee ${CL_SITE}/stage1PromotionLog.txt
