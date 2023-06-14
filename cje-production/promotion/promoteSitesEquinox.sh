#!/bin/bash -x
#*******************************************************************************
# Copyright (c) 2021 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Sravan Kumar Lakkimsetti - initial API and implementation
#*******************************************************************************

function renamefile ()
{
  # file name is input parameter
  # if the file name ends in gif, do not rename (due to performance analysis gifs).
  if [[ ! $1 =~ .*\.gif  ]]
  then
    if [[ $1 =~ (.*)($oldlabel)(.*) ]]
    then
      echo "changing $1 to ${BASH_REMATCH[1]}$newlabel${BASH_REMATCH[3]}"
      mv "$1" "${BASH_REMATCH[1]}$newlabel${BASH_REMATCH[3]}"
    fi
  fi
}

function renameBuild ()
{
  oldname=$1
  oldlabel=$2
  newdirname=$3
  newlabel=$4
  export dirname=$5
  if [[ -z "${dirname}" ]]
  then
    export dirname=$oldname
  fi
  printf "\n\tInput to renameBuild:\n"
  printf "\t\toldname: ${oldname}\n"
  printf "\t\toldlabel: ${oldlabel}\n"
  printf "\t\tnewdirname: ${newdirname}\n"
  printf "\t\tnewlabel: ${newlabel}\n"
  printf "\t\tdirname: ${dirname}\n\n"

  echo "Renaming build $oldname to $newdirname with $newlabel"

  # save original copy, for easy "compare" or "diff"
  mkdir -p ${dirname}/ORIG
  for file in ${dirname}/buildproperties.*
  do
    cp --backup=numbered "${file}" "${dirname}/ORIG/"
  done

  fromString="EQ_BUILD_DIR_SEG = \"${oldname}\""
  toString="EQ_BUILD_DIR_SEG = \"${EQUINOX_DL_DROP_DIR_SEGMENT}\""
  replaceDirCommand="s!${fromString}!${toString}!g"
  echo "replaceDirCommand: $replaceDirCommand"
  perl -w -pi -e "${replaceDirCommand}" ${dirname}/buildproperties.php

  # ===============================
  # specific "replace" to make sure checksums URLs are correct for eclipse
  fromString="BUILD_DIR_SEG = \"${oldname}\""
  toString="BUILD_DIR_SEG = \"${ECLIPSE_DL_DROP_DIR_SEGMENT}\""
  replaceDirCommand="s!${fromString}!${toString}!g"
  echo "replaceDirCommand: $replaceDirCommand"
  perl -w -pi -e "${replaceDirCommand}" ${dirname}/buildproperties.php



  replaceDirCommand="s!/${oldname}/!/${newdirname}/!g"
  perl -w -pi -e "${replaceDirCommand}" ${dirname}/buildproperties.php

  fromString=$oldlabel
  toString=$newlabel
  replaceCommand="s!${fromString}!${toString}!g"
  perl -w -pi -e "${replaceCommand}" ${dirname}/buildproperties.php
  # ===============================
  # It appears THIS is the one required ... changing label, inside files,
  # not "directory name" as above.
  perl -w -pi -e "${replaceCommand}" ${dirname}/checksum/*

  # ===============================
  # Integration --> Stable
  # Integration --> Release Candidate
  # Integration --> Release
  # These are for cases where used in headers, titles, etc.

  OLD_BUILD_TYPE=${BUILD_TYPE}
  # ===============================
  if [[ "${oldlabel}" =~ .*RC.* ]]
  then
    oldString="Release Candidate Build"
  elif [[ "${OLD_BUILD_TYPE}" == "I" ]]
  then
    oldString="Integration Build"
  elif [[ "${OLD_BUILD_TYPE}" == "S" ]]
  then
    oldString="Stable Build"
  else
    echo -e "ERROR: Unexpected OLD_BUILD_TYPE: ${OLD_BUILD_TYPE}"
    exit 1
  fi

  if [[ "${newlabel}" =~ .*RC.* ]]
  then
    newString="Release Candidate Build"
  elif [[ "${DL_TYPE}" == "R" ]]
  then
    newString="Release Build"
  elif [[ "${DL_TYPE}" == "S" ]]
  then
    newString="Stable Build"
  else
    echo -e "ERROR: Unexpected DL_TYPE: ${DL_TYPE}"
    exit 1
  fi

  echo -e "\n\tReplacing ${oldString} with ${newString} in ${dirname}/buildproperties.php\n"

  replaceBuildNameCommand="s!${oldString}!${newString}!g"
  # quotes are critical here, since strings contain spaces!
  perl -w -pi -e "${replaceBuildNameCommand}" ${dirname}/buildproperties.php

  oldString="BUILD_TYPE = \"${OLD_BUILD_TYPE}\""
  # We export explicitly what new TYPE should be, from promoteSites.sh script.
  newString="BUILD_TYPE = \"${DL_TYPE}\""

  replaceBuildNameCommand="s!${oldString}!${newString}!g"
  # quotes are critical here, since strings contain spaces!
  perl -w -pi -e "${replaceBuildNameCommand}" ${dirname}/buildproperties.php

  oldString="BUILD_ID = \"${BUILD_LABEL}\""
  # We export explicitly what new TYPE should be, from promoteSites.sh script.
  newString="BUILD_ID = \"${DL_LABEL}\""

  replaceBuildNameCommand="s!${oldString}!${newString}!g"
  # quotes are critical here, since strings contain spaces!
  perl -w -pi -e "${replaceBuildNameCommand}" ${dirname}/buildproperties.php

  if [[ "${oldlabel}" =~ .*RC.* ]]
  then
    oldString="BUILD_TYPE_NAME = \"Release Candidate\""
  elif [[ $OLD_BUILD_TYPE == "I" ]]
  then
    oldString="BUILD_TYPE_NAME = \"Integration\""
  elif [[ $OLD_BUILD_TYPE == "S" ]]
  then
    oldString="BUILD_TYPE_NAME = \"Stable\""
  else
    echo -e "\n\tERROR: Unexpected OLD_BUILD_TYPE value. ${OLD_BUILD_TYPE}, in $0."
    exit 1
  fi

  if [[ "${newlabel}" =~ .*RC.* ]]
  then
    newString="BUILD_TYPE_NAME = \"Release Candidate\""
  elif [[ "${DL_TYPE}" == "R" ]]
  then
    newString="BUILD_TYPE_NAME = \"Release\""
  elif [[ "${DL_TYPE}" == "S" ]]
  then
    newString="BUILD_TYPE_NAME = \"Stable\""
  else
    echo -e "\n\tERROR: Unexpected DL_TYPE value, ${DL_TYPE}, in $0."
    exit 1
  fi

  echo -e "\n\tReplacing ${oldString} with ${newString} in ${dirname}/buildproperties.php\n"

  replaceBuildNameCommand="s!${oldString}!${newString}!g"
  # quotes are critical here, since strings might contain spaces!
  perl -w -pi -e "${replaceBuildNameCommand}" ${dirname}/buildproperties.php

  echo -e "\n\tMove old directory, $oldname, to new directory, $newdirname. (before file renames)\n"
  mv $oldname $newdirname

  echo -e "\n\tRename files in new directory, ./${newdirname}, to new name."
  echo -e "\tLooking for file names containing oldlabel: \"*${oldlabel}*\""
  nFiles=$(find ./${newdirname} -mindepth 1 -maxdepth 2 -name "*${oldlabel}*" -print | wc -l)
  echo -e "\n\t $nFiles files found to rename.\n"

  for file in $(find ./${newdirname} -mindepth 1 -maxdepth 2 -name "*${oldlabel}*" -print)
  do
    renamefile $file
  done
}

DROP_ID=$(echo $DROP_ID|tr -d ' ')

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
CHECKPOINT=$(echo $CHECKPOINT|tr -d ' ')
if [[ -z "${CHECKPOINT}" ]]
then
  echo -e "\n\t[WARNING] CHECKPOINT was blank in ${0##*/}"
else
  export CHECKPOINT
  echo -e "\n\t[INFO] CHECKPOINT: $CHECKPOINT"  
fi

# This SIGNOFF_BUG should not be defined, if there are no errors in JUnit tests.
SIGNOFF_BUG=$(echo $SIGNOFF_BUG|tr -d ' ')
if [[ -z "${SIGNOFF_BUG}" ]]
then
  echo -e "\n\t[WARNING] SIGNOFF_BUG was not defined. That is valid if no Unit Tests failures but otherwise should be defined."
  echo -e "\t\tCan be added by hand to buildproperties.php in drop site, if in fact there were errors, and simply forgot to specify."
else
  export SIGNOFF_BUG
  echo -e "\t\t[INFO] SIGNOFF_BUG: $SIGNOFF_BUG"
fi


TRAIN_NAME=$(echo $TRAIN_NAME|tr -d ' ')
if [[ -z "${TRAIN_NAME}" ]]
then
  echo -e "\n\t[ERROR] TRAIN_NAME must be defined for ${0##*/}"
  exit 1
else
  export TRAIN_NAME
  echo -e "\n\t[INFO] TRAIN_NAME: $TRAIN_NAME"  
fi

# STREAM is the three digit release number, such as 4.7.0 or 4.6.1.
STREAM=$(echo $STREAM|tr -d ' ')
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
# for main line (master) code, it is always 'S' (from I-build) until it's 'R'
#export DL_TYPE=S
#export DL_TYPE=R
DL_TYPE=$(echo $DL_TYPE|tr -d ' ')
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

SSH_PREFIX="ssh genie.releng@projects-storage.eclipse.org"
export STAGE2DIRSEG=stage2output${TRAIN_NAME}${CHECKPOINT}
export CL_SITE=${WORKSPACE}/${STAGE2DIRSEG}
mkdir -p "${CL_SITE}"

# Main promotion scripts starts here

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

# regex section
# BUILD_TYPE is the prefix of the build --
# that is, for what we are renaming the build FROM
PATTERN="^([MI])([[:digit:]]{8})-([[:digit:]]{4})$"
if [[ "${DROP_ID}" =~ $PATTERN ]]
then
  export BUILD_TYPE=${BASH_REMATCH[1]}
  export REPO_BUILD_TYPE=${BUILD_TYPE}
  export BUILD_TIMESTAMP=${BASH_REMATCH[2]}${BASH_REMATCH[3]}
  # Label and ID are the same, in this case
  export BUILD_LABEL=$DROP_ID
  export BUILD_LABEL_EQ=$DROP_ID
  export DROP_ID_EQ=$DROP_ID
  export REPO_ID=$DROP_ID
else
  PATTERN="^(S)-([[:digit:]]{1})\.([[:digit:]]{2})(.*)-([[:digit:]]{8})([[:digit:]]{4})$"
  if [[ "${DROP_ID}" =~ $PATTERN ]]
  then
    export BUILD_TYPE=${BASH_REMATCH[1]}
    export REPO_BUILD_TYPE=I
    export BUILD_TIMESTAMP=${BASH_REMATCH[5]}${BASH_REMATCH[6]}
    # Label and ID are the same, in this case
    export BUILD_LABEL=${BASH_REMATCH[2]}.${BASH_REMATCH[3]}${BASH_REMATCH[4]}
    export BUILD_LABEL_EQ=${BASH_REMATCH[2]}.${BASH_REMATCH[3]}${BASH_REMATCH[4]}
    export DROP_ID_EQ=$DROP_ID
    export REPO_ID=I${BASH_REMATCH[5]}-${BASH_REMATCH[6]}
  else
    echo -e "\n\tERROR: DROP_ID, ${DROP_ID}, did not match any expected pattern."
    exit 1
  fi
fi

# For initial releases, do not include service in label
if [[ "${BUILD_SERVICE}" == "0" ]]
then
  export DL_LABEL=${BUILD_MAJOR}.${BUILD_MINOR}${CHECKPOINT}
else
  export DL_LABEL=${BUILD_MAJOR}.${BUILD_MINOR}.${BUILD_SERVICE}${CHECKPOINT}
fi

export DL_LABEL_EQ=${DL_LABEL}

# This is DL_DROP_ID for Eclipse. The one for equinox has DL_LABEL_EQ in middle.
export DL_DROP_ID=${DL_TYPE}-${DL_LABEL}-${BUILD_TIMESTAMP}
export DL_DROP_ID_EQ=${DL_TYPE}-${DL_LABEL_EQ}-${BUILD_TIMESTAMP}


export HIDE_SITE=true
# Build machine locations (would very seldom change)
export BUILD_ROOT=${BUILD_ROOT:-/home/data/httpd/download.eclipse.org}
export BUILDMACHINE_BASE_EQ=${BUILD_ROOT}/equinox/drops

# Equinox Drop Site (final segment)
export EQUINOX_DL_DROP_DIR_SEGMENT=${DL_TYPE}-${DL_LABEL_EQ}-${BUILD_TIMESTAMP}



#Take backup of current build
LOCAL_EQ_DIR=${WORKSPACE}/equinox
mkdir -p ${LOCAL_EQ_DIR}



pushd ${LOCAL_EQ_DIR}
scp -r genie.releng@projects-storage.eclipse.org:${BUILDMACHINE_BASE_EQ}/${DROP_ID_EQ} .
popd


# promote equinox
pushd ${LOCAL_EQ_DIR}

  renameBuild ${DROP_ID_EQ} ${BUILD_LABEL_EQ} ${DL_DROP_ID_EQ} ${DL_LABEL}
  if [[ "${HIDE_SITE}" == "true" ]]
  then
    touch ${DL_DROP_ID_EQ}/buildHidden
  fi

  printf "\n\t%s\n" "Promoting Equinox"
  scp -r ${LOCAL_EQ_DIR}/${DL_DROP_ID_EQ} genie.releng@projects-storage.eclipse.org:/home/data/httpd/download.eclipse.org/equinox/drops/

  if [[ "${DL_TYPE}" == "R" ]]
  then
    printf "\n\t%s\n" "Creating archive"
    scp -r ${LOCAL_EQ_DIR}/${DL_DROP_ID_EQ} genie.releng@projects-storage.eclipse.org:/home/data/httpd/archive.eclipse.org/equinox/drops/
  fi

popd
