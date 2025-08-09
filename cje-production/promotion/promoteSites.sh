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

# Main promotion scripts starts here

#Take backup of current build
LOCAL_EP_DIR=${WORKSPACE}/eclipse
LOCAL_EQ_DIR=${WORKSPACE}/equinox
mkdir -p ${LOCAL_EP_DIR}
mkdir -p ${LOCAL_EQ_DIR}

pushd ${LOCAL_EP_DIR}
scp -r genie.releng@projects-storage.eclipse.org:${BUILDMACHINE_BASE_DL}/${DROP_ID} .
popd

pushd ${LOCAL_EQ_DIR}
scp -r genie.releng@projects-storage.eclipse.org:${BUILDMACHINE_BASE_EQ}/${DROP_ID_EQ} .
popd

# ### Begins the point of making modifications to the build ###
if [[ "${DL_TYPE}" != "R" ]]
then
  if [[ "${CHECKPOINT}" =~ $RCPATH ]]
  then
    echo -e "\$NEWS_ID = \"${BUILD_MAJOR}.${BUILD_MINOR}\";" >> "${LOCAL_EP_DIR}/${DROP_ID}/buildproperties.php"
    echo -e "\$ACK_ID = \"${BUILD_MAJOR}.${BUILD_MINOR}\";" >> "${LOCAL_EP_DIR}/${DROP_ID}/buildproperties.php"
    echo -e "\$README_ID = \"${BUILD_MAJOR}.${BUILD_MINOR}\";" >> "${LOCAL_EP_DIR}/${DROP_ID}/buildproperties.php"
  fi
else
  printf "\tINFO: %s\n" "But, we did create NEWS_ID, ACK_ID and README_ID and added to buildproperties.php, since doing Release promote."
  echo -e "\$NEWS_ID = \"${BUILD_MAJOR}.${BUILD_MINOR}\";" >> "${LOCAL_EP_DIR}/${DROP_ID}/buildproperties.php"
  echo -e "\$ACK_ID = \"${BUILD_MAJOR}.${BUILD_MINOR}\";" >> "${LOCAL_EP_DIR}/${DROP_ID}/buildproperties.php"
  echo -e "\$README_ID = \"${BUILD_MAJOR}.${BUILD_MINOR}\";" >> "${LOCAL_EP_DIR}/${DROP_ID}/buildproperties.php"
fi



# SIGNOFF_BUG should not be defined if there are no JUnit failures to investigate and explain
if [[ -n "${SIGNOFF_BUG}" ]]
then
  echo -e "<p>Any unit test failures below have been investigated and found to be test-related and do not affect the quality of the build.\nSee the sign-off page <a href=\"https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/${SIGNOFF_BUG}\">(issue ${SIGNOFF_BUG})</a> for details.</p>" > "${LOCAL_EP_DIR}/${DROP_ID}/testNotes.html"
fi

# promote equinox
pushd ${LOCAL_EQ_DIR}

  renameBuild ${DROP_ID_EQ} ${BUILD_LABEL_EQ} ${DL_DROP_ID_EQ} ${DL_LABEL}
  if [[ "${HIDE_SITE}" == "true" ]]
  then
    touch ${DL_DROP_ID_EQ}/buildHidden
  fi

  printf "\n\t%s\n" "Promoting Equinox"
  scp -r ${LOCAL_EQ_DIR}/${DL_DROP_ID_EQ} genie.releng@projects-storage.eclipse.org:${BUILDMACHINE_BASE_EQ}/

  if [[ "${DL_TYPE}" == "R" ]]
  then
    printf "\n\t%s\n" "Creating archive"
    scp -r ${LOCAL_EQ_DIR}/${DL_DROP_ID_EQ} genie.releng@projects-storage.eclipse.org:/home/data/httpd/archive.eclipse.org/equinox/drops/
  fi

popd

# Promote Eclipse platform
pushd ${LOCAL_EP_DIR}

  renameBuild ${DROP_ID} ${BUILD_LABEL} ${DL_DROP_ID} ${DL_LABEL}
  if [[ "${HIDE_SITE}" == "true" ]]
  then
    touch ${DL_DROP_ID}/buildHidden
  fi

  printf "\n\t%s\n" "Promoting Platform"
  scp -r ${LOCAL_EP_DIR}/${DL_DROP_ID} genie.releng@projects-storage.eclipse.org:${BUILDMACHINE_BASE_DL}/

  if [[ "${DL_TYPE}" == "R" ]]
  then
    printf "\n\t%s\n" "Creating archive"
    scp -r ${LOCAL_EP_DIR}/${DL_DROP_ID} genie.releng@projects-storage.eclipse.org:/home/data/httpd/archive.eclipse.org/eclipse/downloads/drops4/
  fi

popd
