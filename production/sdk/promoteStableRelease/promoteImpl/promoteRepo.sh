#!/usr/bin/env bash

DROP_ID=$1
DL_LABEL=$2
REPO_SITE_SEGMENT=$3
HIDE_SITE=$4

function usage ()
{
  printf "\n\tUsage: %s DROP_ID DL_LABEL REPO_SITE_SEGMENT HIDE_SITE" $(basename $0) >&2
  printf "\n\t\t%s\t%s" "DROP_ID " "such as I20121031-2000." >&2
  printf "\n\t\t%s\t%s" "DL_LABEL " "such as 4.4M3." >&2
  printf "\n\t\t%s\t%s" "REPO_SITE_SEGMENT " "such as 4.4milestones, 4.4, etc." >&2
  printf "\n\t\t%s\t%s" "HIDE_SITE " "true or false." >&2
}

if [[ -z "${DROP_ID}" || -z "${DL_LABEL}" || -z "${REPO_SITE_SEGMENT}" || -z "${HIDE_SITE}" ]]
then
  printf "\n\n\t%s\n\n" "ERROR: arguments missing in call to $( basename $0 )." >&2
  usage
  exit 1
fi


DL_SITE_ID=${DL_TYPE}-${DL_LABEL}-${BUILD_TIMESTAMP}

BUILDMACHINE_SITE=${BUILDMACHINE_BASE_SITE}/${DROP_ID}

DLMACHINE_BASE_SITE=/home/data/httpd/download.eclipse.org/eclipse/updates/${REPO_SITE_SEGMENT}
# just in case first time
mkdir -p $DLMACHINE_BASE_SITE

DLMACHINE_SITE=${DLMACHINE_BASE_SITE}/${DL_SITE_ID}
# just in case first time
mkdir -p $DLMACHINE_SITE

source ${PROMOTE_IMPL}/promoteUtilities.shsource
findEclipseExe ${DL_SITE_ID}
RC=$?
if [[ $RC == 0 ]]
then
  ${PROMOTE_IMPL}/addRepoProperties.sh ${BUILDMACHINE_SITE} ${REPO_SITE_SEGMENT} ${DL_SITE_ID}
else
  echo "ERROR: could not run add repo properties. Add manually."
fi

printf "\n\t%s\n" "rsync build machine repo site, to downloads repo site."
# remember, need trailing slash since going from existing directories
# contents to new directories contents
rsync -r "${BUILDMACHINE_SITE}/"  "${DLMACHINE_SITE}"

if [[ "${HIDE_SITE}" != "true" ]]
then
  ${PROMOTE_IMPL}/runAntRunner.sh ${PROMOTE_IMPL}/addToComposite.xml addToComposite -Drepodir=${DLMACHINE_BASE_SITE} -Dcomplocation=${DL_SITE_ID}
else
  echo "#!/usr/bin/env bash" > deferedCompositeAdd.sh
  echo "export JAVA_CMD=$JAVA_CMD" >> deferedCompositeAdd.sh
  echo "export JAVA_EXEC_DIR=${JAVA_EXEC_DIR}" >> deferedCompositeAdd.sh
  echo "export ECLIPSE_EXE=${ECLIPSE_EXE}" >> deferedCompositeAdd.sh
  echo "${PROMOTE_IMPL}/runAntRunner.sh ${PROMOTE_IMPL}/addToComposite.xml addToComposite -Drepodir=${DLMACHINE_BASE_SITE} -Dcomplocation=${DL_SITE_ID}" >> deferedCompositeAdd.sh
  chmod +x deferedCompositeAdd.sh
  echo "Remember to add to composite, deferedCompositeAdd.sh, since HIDE_SITE was ${HIDE_SITE}" >> "${CL_SITE}/checklist.txt"
fi
