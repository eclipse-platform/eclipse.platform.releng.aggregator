#!/usr/bin/env bash

DROP_ID=$1
DL_LABEL=$2
HIDE_SITE=$3

function usage ()
{
  printf "\n\tUsage: %s DROP_ID DL_LABEL HIDE_SITE" $(basename $0) >&2
  printf "\n\t\t%s\t%s" "DROP_ID " "such as I20121031-2000." >&2
  printf "\n\t\t%s\t%s" "DL_LABEL " "such as 4.4M3." >&2
  printf "\n\t\t%s\t%s" "HIDE_SITE " "true or false." >&2
}

if [[ -z "${DROP_ID}" || -z "${DL_LABEL}" || -z "${HIDE_SITE}" ]]
then
  printf "\n\n\t%s\n\n" "ERROR: arguments missing in call to $( basename $0 )" >&2
  usage
  exit 1
fi

DL_DROP_ID=${DL_TYPE}-${DL_LABEL}-${BUILD_TIMESTAMP}

cd ${BUILDMACHINE_BASE_DL}
cp /shared/eclipse/sdk/renameBuild.sh ${PWD}

if [[ ! "${INDEX_ONLY}" == "true" ]]
then
  printf "\n\n\t%s\n" "Promoting Eclipse site."
else
  printf "\n\n\t%s\n" "Promoting Eclipse Index site."
fi

if [[ ! "${INDEX_ONLY}" == "true" ]]
then
  printf "\n\t%s\n\t%s to \n\t%s\n" "Making backup copy of original ..." "$DROP_ID" "${DROP_ID}ORIG"
  if [[ -e  ${DROP_ID} ]]
  then
    rsync -r ${DROP_ID}/ ${DROP_ID}ORIG
  else
    echo -e "\n\tERROR: the directory ${DROP_ID} does not exist\n"
    exit 1
  fi
  printf "\n\t%s\n" "Doing rename of original."

  # if DL_DROP_ID already exists, it is from a previous run we are re-doing, do,
  # we'll remove first, to make sure it's cleaning re-done.
  if [[ -d ${DL_DROP_ID} ]]
  then
    echo -e "\n\tWARNING: found and will remove existing, previous, version of ${DL_DROP_ID}"
    rm -fr ${DL_DROP_ID}
    RC=$?
    if [[ $RC != 0 ]]
    then
      echo -e "/n/tERROR: Could not remove previous (failed) version of DL_DROP_ID, ${DL_DROP_ID}"
      exit 1
    fi
  fi
else
  # just copy over what's there.
  # TODO: earlier, we could check to be sure the directory we expect really does exist.
  printf "\n\t%s\n" "Making copy (update) of original on top of previous renamed version. "
  if [[ -e  ${DROP_ID} ]]
  then
    rsync -ru ${DROP_ID}/ ${DL_DROP_ID}/
  else
    echo -e "\n\tERROR: the directory ${DROP_ID} does not exist\n"
    exit 1
  fi
fi

if [[ ! "${INDEX_ONLY}" == "true" ]]
then
  # rename old dir to new dir
./renameBuild.sh ${DROP_ID} ${DL_DROP_ID} ${DL_LABEL}
else
  # If indexing only, we still need to run "renamed" just to pick up "renames" in test results., but in "new" directory
  ./renameBuild.sh ${DROP_ID} ${DL_DROP_ID} ${DL_LABEL} ${DL_DROP_ID}
fi

if [[ ! "${INDEX_ONLY}" == "true" ]]
then
  printf "\n\t%s\n" "Moving backup copy back to original, since INDEX_ONLY was not defined."
  mv ${DROP_ID}ORIG ${DROP_ID}
else
  printf "\n\t%s\n" "Nothing to move back to original, since never copied to ORIG, since INDEX_ONLY was ${INDEX_ONLY}"
fi

rm renameBuild.sh

# keep hidden, initially, both to confirm all is correct,
# and in theory could wait a bit to get a mirror or two
# (in some cases).
if [[ "${HIDE_SITE}" == "true" ]]
then
  touch ${DL_DROP_ID}/buildHidden
  if [[ $? != 0 ]]
  then
    echo "touch failed. Exiting."
    exit 1
  fi
  echo "Remember to remove 'buildHidden' file, and re-run updateIndexes.sh since HIDE_SITE was ${HIDE_SITE}." >> "${CL_SITE}/checklist.txt"
else
  echo "HIDE_SITE value was ${HIDE_SITE}"
  if [[ -e ${DL_DROP_ID}/buildHidden ]]
  then
    mv ${DL_DROP_ID}/buildHidden ${DL_DROP_ID}/buildHiddenFOUND
    echo "Found existing 'buildHidden' file, and renamed it to 'buildHiddenFOUND' since 'HIDE_SITE' was ${HIDE_SITE}"
  fi
fi

if [[ "${DL_TYPE}" =~ [SR] ]]
then
  # as a matter of routine, turn "test color" to green, if not already
  touch ${DL_DROP_ID}/overrideTestColor

  # and turn on "news flag"
  touch ${DL_DROP_ID}/news
fi

printf "\n\t%s\n" "rsync to downloads."
if [[ "${INDEX_ONLY}" == "true" ]]
then
  printf "\n\t%s\n" "Will do --update only, since updating index."
  UPDATE_ARG="--update"
else
  UPDATE_ARE=
fi

# Here we can rsync with committer id. For Equinox, we have to create a promotion file.
rsync ${UPDATE_ARG} --recursive --prune-empty-dirs --exclude="*apitoolingreference/*" --exclude="*org.eclipse.releng.basebuilder/*" --exclude="*eclipse.platform.releng.aggregator/*" --exclude="*repository/*" --exclude="*workspace-*/*" ${DL_DROP_ID} /home/data/httpd/download.eclipse.org/eclipse/downloads/drops4/
rccode=$?
if [ $rccode -eq 0 ]
then
  if [[ "${HIDE_SITE}" != "true" ]]
  then
    printf "\n\t%s\n" "Update main overall download index page so it shows new build."
    source /shared/eclipse/sdk/updateIndexFilesFunction.shsource
    updateIndex
  fi
else
  printf "\n\n\t%s\n\n" "ERROR: rsync failed. rccode: $rccode" >&2
  exit $rccode
fi

