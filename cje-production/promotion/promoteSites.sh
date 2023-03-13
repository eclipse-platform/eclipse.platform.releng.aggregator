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

function createBaseBuilder ()
{
  epRelDir=$(ssh genie.releng@projects-storage.eclipse.org ls -d --format=single-column ${BUILDMACHINE_BASE_DL}/R-*|sort|tail -1)
  BASEBUILDER_DIR=${WORKSPACE}/basebuilder
  export BASEBUILDER_DIR
  mkdir -p ${BASEBUILDER_DIR}
  mkdir -p ${WORKSPACE}/tempEclipse
  pushd ${WORKSPACE}/tempEclipse
    scp genie.releng@projects-storage.eclipse.org:${epRelDir}/eclipse-platform-*-linux-gtk-x86_64.tar.gz eclipse-platform.tar.gz
    tar xvzf eclipse-platform.tar.gz
    ${WORKSPACE}/tempEclipse/eclipse/eclipse -nosplash \
        -debug -consolelog -data ${WORKSPACE}/workspace-toolsinstall \
        -application org.eclipse.equinox.p2.director \
        -repository "https://download.eclipse.org/eclipse/updates/latest/","https://download.eclipse.org/eclipse/updates/buildtools/",${WEBTOOLS_REPO} \
        -installIU org.eclipse.platform.ide,org.eclipse.pde.api.tools,org.eclipse.releng.build.tools.feature.feature.group,org.eclipse.wtp.releng.tools.feature.feature.group \
        -destination ${BASEBUILDER_DIR} \
        -profile SDKProfile
  popd
  export ECLIPSE_EXE=${BASEBUILDER_DIR}/eclipse
  rm -rf ${WORKSPACE}/tempEclipse
}

function addRepoProperties ()
{
  APP_NAME=org.eclipse.wtp.releng.tools.addRepoProperties 
  devworkspace=${devworkspace:-${WORKSPACE}/workspaceAddRepoProperties}

  REPO=$1
  REPO_TYPE=$2
  BUILD_ID=$3

  createBaseBuilder
  MIRRORURL=/eclipse/updates/${REPO_TYPE}/${BUILD_ID} 
  MIRRORURL_ARG="https://www.eclipse.org/downloads/download.php?format=xml&file=${MIRRORURL}"

  ART_REPO_NAME="Eclipse Project Repository for ${TRAIN_NAME}"
  CON_REPO_NAME="Eclipse Project Repository for ${TRAIN_NAME}"

  MIRRORS_URL_ARG=-Dp2MirrorsURL=${MIRRORURL_ARG}
  ART_REPO_ARG=-DartifactRepoDirectory=${REPO}
  CON_REPO_ARG=-DmetadataRepoDirectory=${REPO}
  ART_REPO_NAME_ARG=-Dp2ArtifactRepositoryName=\"${ART_REPO_NAME}\"
  CON_REPO_NAME_ARG=-Dp2MetadataRepositoryName=\"${CON_REPO_NAME}\"

  ${ECLIPSE_EXE} --launcher.suppressErrors -nosplash -consolelog -debug -data ${devworkspace} -application ${APP_NAME} -vmargs ${MIRRORS_URL_ARG} -Dp2ArtifactRepositoryName="${ART_REPO_NAME}" -Dp2MetadataRepositoryName="${CON_REPO_NAME}" ${ART_REPO_ARG} ${CON_REPO_ARG}
}

function createXZ
{
  BUILDMACHINE_SITE=$1

  CONTENT_JAR_FILE="${BUILDMACHINE_SITE}/content.jar"
  if [[ ! -e "${CONTENT_JAR_FILE}" ]]
  then
    echo -e "\n\tERROR: content.jar file did not exist at ${BUILDMACHINE_SITE}."
    return 1
  fi
  ARTIFACTS_JAR_FILE="${BUILDMACHINE_SITE}/artifacts.jar"
  if [[ ! -e "${ARTIFACTS_JAR_FILE}" ]]
  then
    echo -e "\n\tERROR: artifacts.jar file did not exist at ${BUILDMACHINE_SITE}."
    return 1
  fi

  # Notice we overwrite the XML files, if they already exists.
  unzip -q -o "${CONTENT_JAR_FILE}" -d "${BUILDMACHINE_SITE}"
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo "ERROR: could not unzip ${CONTENT_JAR_FILE}."
    return $RC
  fi
  # Notice we overwrite the XML files, if they already exists.
  unzip -q -o "${ARTIFACTS_JAR_FILE}" -d "${BUILDMACHINE_SITE}"
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo "ERROR: could not unzip ${ARTIFACTS_JAR_FILE}."
    return $RC
  fi

  CONTENT_XML_FILE="${BUILDMACHINE_SITE}/content.xml"
  ARTIFACTS_XML_FILE="${BUILDMACHINE_SITE}/artifacts.xml"
  # We will check the content.xml and artifacts.xml files really exists. In some strange world, the jars could contain something else.
  if [[ ! -e "${CONTENT_XML_FILE}" || ! -e "${ARTIFACTS_XML_FILE}" ]]
  then
    echo -e "\n\tERROR: content.xml or artifacts.xml file did not exist as expected at ${BUILDMACHINE_SITE}."
    return 1
  fi

  # finally, compress them, using "extra effort"
  # Notice we use "force" to over write any existing file, presumably there from a previous run?
  XZ_EXE=$(which xz)
  if [[ $? != 0 || -z "${XZ_EXE}" ]]
  then
    echo -e "\n\tERROR: xz executable did not exist."
    return 1
  fi
  echo -e "\n\tXZ compression of ${CONTENT_XML_FILE} ... "
  $XZ_EXE -e --force "${CONTENT_XML_FILE}"
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo "ERROR: could not compress, using $XZ_EXE -e ${CONTENT_XML_FILE}."
    return $RC
  fi

  echo -e "\tXZ compression of ${ARTIFACTS_XML_FILE} ... "
  $XZ_EXE -e --force "${ARTIFACTS_XML_FILE}"
  RC=$?
  if [[ $RC != 0 ]]
  then
    echo "ERROR: could not compress, using $XZ_EXE -e ${ARTIFACTS_XML_FILE}."
    return $RC
  fi


  # Notice we just write over any existing p2.index file.
  # May want to make backup of this and other files, for production use.
  P2_INDEX_FILE="${BUILDMACHINE_SITE}/p2.index"
  echo "version=1" > "${P2_INDEX_FILE}"
  echo "metadata.repository.factory.order= content.xml.xz,content.xml,!" >> "${P2_INDEX_FILE}"
  echo "artifact.repository.factory.order= artifacts.xml.xz,artifacts.xml,!" >> "${P2_INDEX_FILE}"
  echo -e "\tCreated ${P2_INDEX_FILE}"

  return 0
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

# Extract WEBTOOLS_REPO and other variables from buildproperties.shsource for the build
wget -O ${WORKSPACE}/buildproperties.shsource https://download.eclipse.org/eclipse/downloads/drops4/${DROP_ID}/buildproperties.shsource
source ${WORKSPACE}/buildproperties.shsource

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

# for I builds, stable and RCs go to in milestones
# for M builds, even RCs also go in <version>-M-builds
case ${DL_TYPE} in
  "S" )
    export REPO_SITE_SEGMENT=${BUILD_MAJOR}.${BUILD_MINOR}milestones
    if [[ "${CHECKPOINT}" =~ ^M.*$ ]]
    then
      export NEWS_ID=${BUILD_MAJOR}.${BUILD_MINOR}
    fi
    # except for RC4. Since it it intended to be "final" we include the variables
    # just like they were for an "R" build. (See bug 495252).
    # to accomidate "respins", we use a regex so that adding an "a" or "b",
    # will still match.
    RCPATH=^RC.[abcd]?$
    if [[ "${CHECKPOINT}" =~  $RCPATH ]]
    then
      export NEWS_ID=${BUILD_MAJOR}.${BUILD_MINOR}
      export ACK_ID=${BUILD_MAJOR}.${BUILD_MINOR}
      export README_ID=${BUILD_MAJOR}.${BUILD_MINOR}
    fi
    ;;
  "R" )
    export REPO_SITE_SEGMENT=${BUILD_MAJOR}.${BUILD_MINOR}
    export NEWS_ID=${BUILD_MAJOR}.${BUILD_MINOR}
    export ACK_ID=${BUILD_MAJOR}.${BUILD_MINOR}
    export README_ID=${BUILD_MAJOR}.${BUILD_MINOR}
    ;;
  *)
    echo -e "\n\tERROR: case statement for repo output did not match any pattern."
    echo -e   "\t       Not written to handle DL_TYPE of ${DL_TYPE}\n"
    exit 1
esac

export HIDE_SITE=true
# Build machine locations (would very seldom change)
export BUILD_ROOT=${BUILD_ROOT:-/home/data/httpd/download.eclipse.org}
export BUILD_REPO_ORIGINAL=${BUILD_MAJOR}.${BUILD_MINOR}-${REPO_BUILD_TYPE}-builds
export BUILDMACHINE_BASE_SITE=${BUILD_ROOT}/eclipse/updates/${BUILD_REPO_ORIGINAL}

export BUILDMACHINE_BASE_DL=${BUILD_ROOT}/eclipse/downloads/drops4
export BUILDMACHINE_BASE_EQ=${BUILD_ROOT}/equinox/drops

# Eclipse Drop Site (final segment)
export ECLIPSE_DL_DROP_DIR_SEGMENT=${DL_TYPE}-${DL_LABEL}-${BUILD_TIMESTAMP}
# Equinox Drop Site (final segment)
export EQUINOX_DL_DROP_DIR_SEGMENT=${DL_TYPE}-${DL_LABEL_EQ}-${BUILD_TIMESTAMP}

export INITIAL_MAIL_LINES="We are pleased to announce that ${TRAIN_NAME} ${CHECKPOINT} is available for download and updates."
export CLOSING_MAIL_LINES="Thank you to everyone who made this checkpoint possible."

echo -e "\n\t[INFO] INITIAL_MAIL_LINES: $INITIAL_MAIL_LINES"  
echo -e "\n\t[INFO] CLOSING_MAIL_LINES: $CLOSING_MAIL_LINES"  

printf "\n\t%s\n\n" "Promoted on: $( date )" > "${CL_SITE}/checklist.txt"
printf "\n\t%20s%25s" "DROP_ID" "$DROP_ID" >> "${CL_SITE}/checklist.txt"
printf "\n\t%20s%25s" "BUILD_LABEL" "$BUILD_LABEL" >> "${CL_SITE}/checklist.txt"
printf "\n\t%20s%25s" "DROP_ID_EQ" "$DROP_ID_EQ" >> "${CL_SITE}/checklist.txt"
printf "\n\t%20s%25s" "BUILD_LABEL_EQ" "$BUILD_LABEL_EQ" >> "${CL_SITE}/checklist.txt"
printf "\n" >> "${CL_SITE}/checklist.txt"
printf "\n\t%20s%25s" "DL_TYPE" "$DL_TYPE" >> "${CL_SITE}/checklist.txt"
printf "\n\t%20s%25s" "DL_LABEL" "$DL_LABEL" >> "${CL_SITE}/checklist.txt"
printf "\n\t%20s%25s" "DL_LABEL_EQ" "$DL_LABEL_EQ" >> "${CL_SITE}/checklist.txt"
printf "\n\t%20s%25s" "ECLIPSE_DL_DROP_DIR_SEGMENT" "$ECLIPSE_DL_DROP_DIR_SEGMENT" >> "${CL_SITE}/checklist.txt"
printf "\n\t%20s%25s" "EQUINOX_DL_DROP_DIR_SEGMENT" "$EQUINOX_DL_DROP_DIR_SEGMENT" >> "${CL_SITE}/checklist.txt"
printf "\n\t%20s%25s" "REPO_SITE_SEGMENT" "$REPO_SITE_SEGMENT" >> "${CL_SITE}/checklist.txt"
printf "\n\t%20s%25s\n" "HIDE_SITE" "${HIDE_SITE}" >> "${CL_SITE}/checklist.txt"

printf "\t%s\n" "Eclipse downloads:" >> "${CL_SITE}/checklist.txt"
printf "\t%s\n\n" "https://download.eclipse.org/eclipse/downloads/drops4/${ECLIPSE_DL_DROP_DIR_SEGMENT}/" >> "${CL_SITE}/checklist.txt"

if [[ "${DL_TYPE}" == "R" ]]
then
  printf "\t%s\n" "Update existing (non-production) installs:" >> "${CL_SITE}/checklist.txt"
  printf "\t%s\n\n" "https://download.eclipse.org/eclipse/updates/${REPO_SITE_SEGMENT}/" >> "${CL_SITE}/checklist.txt"

  printf "\t%s\n" "Specific repository good for building against:" >> "${CL_SITE}/checklist.txt"
  printf "\t%s\n\n" "https://download.eclipse.org/eclipse/updates/${REPO_SITE_SEGMENT}/${ECLIPSE_DL_DROP_DIR_SEGMENT}/" >> "${CL_SITE}/checklist.txt"
else
  printf "\t%s\n" "Update existing (non-production) installs:" >> "${CL_SITE}/checklist.txt"
  printf "\t%s\n\n" "https://download.eclipse.org/eclipse/updates/${BUILD_REPO_ORIGINAL}/" >> "${CL_SITE}/checklist.txt"

  printf "\t%s\n" "Specific repository good for building against:" >> "${CL_SITE}/checklist.txt"
  printf "\t%s\n\n" "https://download.eclipse.org/eclipse/updates/${BUILD_REPO_ORIGINAL}/${DROP_ID}/" >> "${CL_SITE}/checklist.txt"
fi

printf "\t%s\n" "Equinox specific downloads:" >> "${CL_SITE}/checklist.txt"
printf "\t%s\n\n" "https://download.eclipse.org/equinox/drops/${EQUINOX_DL_DROP_DIR_SEGMENT}/" >> "${CL_SITE}/checklist.txt"

# mail template

# start with empty line, and '>' to be sure re-created if exists already.
printf "\n" > "${CL_SITE}/mailtemplate.txt"

printf "\n%s\n\n" "${INITIAL_MAIL_LINES}" >> "${CL_SITE}/mailtemplate.txt"

printf "\t%s\n" "Eclipse downloads:" >> "${CL_SITE}/mailtemplate.txt"
printf "\t%s\n\n" "https://download.eclipse.org/eclipse/downloads/drops4/${ECLIPSE_DL_DROP_DIR_SEGMENT}/" >> "${CL_SITE}/mailtemplate.txt"

printf "\t%s\n" "New and Noteworthy:" >> "${CL_SITE}/mailtemplate.txt"
printf "\t%s\n\n" "https://www.eclipse.org/eclipse/news/${NEWS_ID}/" >> "${CL_SITE}/mailtemplate.txt"

if [[ "${DL_TYPE}" == "R" ]]
then
  printf "\t%s\n" "Update existing (non-production) installs:" >> "${CL_SITE}/mailtemplate.txt"
  printf "\t%s\n\n" "https://download.eclipse.org/eclipse/updates/${REPO_SITE_SEGMENT}/" >> "${CL_SITE}/mailtemplate.txt"

  printf "\t%s\n" "Specific repository good for building against:" >> "${CL_SITE}/mailtemplate.txt"
  printf "\t%s\n\n" "https://download.eclipse.org/eclipse/updates/${REPO_SITE_SEGMENT}/${ECLIPSE_DL_DROP_DIR_SEGMENT}/" >> "${CL_SITE}/mailtemplate.txt"
else
  printf "\t%s\n" "Update existing (non-production) installs:" >> "${CL_SITE}/mailtemplate.txt"
  printf "\t%s\n\n" "https://download.eclipse.org/eclipse/updates/${BUILD_REPO_ORIGINAL}/" >> "${CL_SITE}/mailtemplate.txt"

  printf "\t%s\n" "Specific repository good for building against:" >> "${CL_SITE}/mailtemplate.txt"
  printf "\t%s\n\n" "https://download.eclipse.org/eclipse/updates/${BUILD_REPO_ORIGINAL}/${DROP_ID}/" >> "${CL_SITE}/mailtemplate.txt"
fi

printf "\t%s\n" "Equinox specific downloads:" >> "${CL_SITE}/mailtemplate.txt"
printf "\t%s\n\n" "https://download.eclipse.org/equinox/drops/${EQUINOX_DL_DROP_DIR_SEGMENT}/" >> "${CL_SITE}/mailtemplate.txt"

printf "\n\n%s\n" "${CLOSING_MAIL_LINES}" >> "${CL_SITE}/mailtemplate.txt"

#Take backup of current build
LOCAL_EP_DIR=${WORKSPACE}/eclipse
LOCAL_EQ_DIR=${WORKSPACE}/equinox
LOCAL_REPO=${WORKSPACE}/updates
mkdir -p ${LOCAL_EP_DIR}
mkdir -p ${LOCAL_EQ_DIR}
mkdir -p ${LOCAL_REPO}

pushd ${LOCAL_EP_DIR}
scp -r genie.releng@projects-storage.eclipse.org:${BUILDMACHINE_BASE_DL}/${DROP_ID} .
popd

pushd ${LOCAL_EQ_DIR}
scp -r genie.releng@projects-storage.eclipse.org:${BUILDMACHINE_BASE_EQ}/${DROP_ID_EQ} .
popd

pushd ${LOCAL_REPO}
scp -r genie.releng@projects-storage.eclipse.org:${BUILDMACHINE_BASE_SITE}/${REPO_ID} .
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
  scp -r ${LOCAL_EQ_DIR}/${DL_DROP_ID_EQ} genie.releng@projects-storage.eclipse.org:/home/data/httpd/download.eclipse.org/equinox/drops/

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
  scp -r ${LOCAL_EP_DIR}/${DL_DROP_ID} genie.releng@projects-storage.eclipse.org:/home/data/httpd/download.eclipse.org/eclipse/downloads/drops4/

  if [[ "${DL_TYPE}" == "R" ]]
  then
    printf "\n\t%s\n" "Creating archive"
    scp -r ${LOCAL_EP_DIR}/${DL_DROP_ID} genie.releng@projects-storage.eclipse.org:/home/data/httpd/archive.eclipse.org/eclipse/downloads/drops4/
  fi

popd


#Promote Repository
if [[ "${DL_TYPE}" == "R" ]]
then
  pushd ${LOCAL_REPO}
    BUILDMACHINE_SITE=${LOCAL_REPO}/${REPO_ID}
    addRepoProperties ${BUILDMACHINE_SITE} ${REPO_SITE_SEGMENT} ${DL_DROP_ID}
    createXZ ${BUILDMACHINE_SITE}
    mv ${REPO_ID} ${DL_DROP_ID}
    scp -r ${LOCAL_REPO}/${DL_DROP_ID} genie.releng@projects-storage.eclipse.org:/home/data/httpd/download.eclipse.org/eclipse/updates/${REPO_SITE_SEGMENT}
  popd
fi
