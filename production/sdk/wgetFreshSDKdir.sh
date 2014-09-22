#!/usr/bin/env bash

# directly gets a fresh copy of sdk directory from eclipsebuilder
# need to manually check and make sure nothing is running or will
# be running soon.

source localBuildProperties.shsource 2>/dev/null

# Normally, we should be "working in" /shared/eclipse ... the parent of 'sdk' directory.
export WORK_DIR=${WORK_DIR:-${PWD}}

# codifying the branch (or tag) to use, so it can be set/chagned in one place
branch=master
initScriptTag="h=$branch"

# to use a tag instead of branch, would be tag=X, such as
# tag=vI20120417-0700, or in full form
# http://${GIT_HOST}/c/platform/eclipse.platform.releng.eclipsebuilder.git/plain/scripts/wgetFresh.sh?tag=vI20120417-0700

# = = = = = = =

function errorExit ()
{
  MSG=$1
  RETURN_CODE=$2
  # We will count no message as a warning, but, is intended for caller to provide, 
  # so is technically a programming error. 
  if [[ -z "{MSG}" ]]
  then
      printf "/n/tWARNING: /t%s" "Call to errorExit provided no message"  
      MSG="No message provided."
   fi
   // May be legitimate not to provide "exit status", in which case we just use '1'.
   // TODO: Deluxe version would check for positive integer between 0 and 255
   if [[ -z "${RETURN_CODE}" ]] 
   then
     $RETURN_CODE=1
   fi
   # Here is whole purpose of this method.
   printf "\n\tERROR: \t%s" "${MSG} Exit Status: ${RETURN_CODE}"
}

function checkForErrorExit ()
{
  # arg 1 must be return code, $?
  # arg 2 (remaining line) can be message to print before exiting due to non-zero exit code
  exitCode=$1
  shift
  message="$*"
  if [[ -z "${exitCode}" ]]
  then
    echo -e "\n\tPROGRAM ERROR: checkForErrorExit called with no arguments\n"
    exit 1
  fi
  if [[ -z "${message}" ]]
  then
    echo -e "\n\tWARNING: checkForErrorExit called without message\n"
    message="(Calling program provided no message)"
  fi
  if [[ "${TEST_MODE}" == "true" ]]
  then
    echo -e "\t\tTest mode: exitCode: $exitCode \t message: ${message}"
  fi
  if [[ ${exitCode} -lt 0 ]]
  then
    # This is just a "fact of the way bash works" ... but, hard to debug if not expecting it.
    echo -e "\n\t WARNING: exitCode was less than 0, ${exitCode}, so actual value will be different. $(( ${exitCode} & 255 )) \n"
    exitCode=$(( ${exitCode} & 255 ))
  fi
  if [[ ${exitCode} -gt 255 ]]
  then
    # This is just a "fact of the way bash works" ... but, hard to debug if not expecting it.
    echo -e "\n\t WARNING: exitCode was greater than 255, ${exitCode}, so value on exit will be modulo 256. $(( ${exitCode} % 256 )) \n"
    exitCode=$(( ${exitCode} % 256 ))
  fi
  if [[ ! ${exitCode} =~ ^[0-9]+$ ]]
  then
    echo -e "\n\t WARNING: exitCode was not all digits.\n\t Arbitrarily set exitCode to 1 (and may have unintended results).\n"
    exitCode=1
  fi
  if [[ ${exitCode} != 0 ]]
  then
    if [[ "${TEST_MODE}" == "true" ]]
    then
      echo -e "\n\t ERROR. exit code: ${exitCode}  ${message} \tTest mode: otherwise would have exited with ${exitCode}"
    else
      echo -e "\n\t ERROR. exit code: ${exitCode}  ${message}\n"
      exit ${exitCode}
    fi
  else
    if [[ "${TEST_MODE}" == "true" ]]
    then
      echo -e "\t\tTest mode: returned 0 (no exit)"
    fi
    return 0
  fi
}

# = = = = =

if [[ "$1" == "-t" ]]
then
  TEST_MODE=true
  echo -e "\n\tStarting self test mode, since '-t' specified. Will exit when done with tests.\n"
  checkForErrorExit 0 #no message case
  checkForErrorExit 0 "Normal zero case."
  checkForErrorExit 0 "Normal zero as string case."
  checkForErrorExit abcd "String, not numeric case."
  checkForErrorExit 255 "Number exactly 255 cases."
  checkForErrorExit -1 "Negative number case."
  checkForErrorExit -2 "Negative number case."
  checkForErrorExit 512 "Number greater than 255 case."

  echo -e "\n\tTest mode completed normally.\n"
  exit 0
fi

cd "${WORK_DIR}"
checkForErrorExit $? "could not change directory parent of sdk, ${WORK_DIR}."

# as a sanity check, we make sure WORK_DIR is defined to be something, and not equal to "/" or ${HOME}
# since some "removes" either won't work, or risk removing things we do not intend.
if [[ -z "${WORK_DIR}" ]]
then
   errorExit "WORK_DIR was not defined."
fi
if [[ "${WORK_DIR}" == "/" || "${WORK_DIR}" == "${HOME}" ]]
then
   errorExit "WORK_DIR inappropriately defined as ${WORK_DIR}"
fi

# remove if exists from previous (failed) run
if [[ -e "${WORK_DIR}/tempeb" ]]
then
  rm -fr "${WORK_DIR}/tempeb"
  checkForErrorExit $? "Could not remove directory tempeb"
else
  mkdir -p "${WORK_DIR}/tempeb"
  checkForErrorExit $? "could not mkdir tempeb"
fi

# ditto
if [[ -e "${WORK_DIR}/master.zip" ]]
then
  rm "${WORK_DIR}/master.zip"
  checkForErrorExit $? "Could not remove master.zip"
fi

if [[ -z "${GIT_HOST}" ]]
then
   GIT_HOST=git.eclipse.org
fi

wget --no-verbose --no-cache -O "${WORK_DIR}/master.zip" http://${GIT_HOST}/c/platform/eclipse.platform.releng.aggregator.git/snapshot/master.zip 2>&1;
checkForErrorExit $? "could not get aggregator?!"

unzip -q "${WORK_DIR}/master.zip" -d "${WORK_DIR}/tempeb"
checkForErrorExit $? "could not unzip master?!"

# save a copy to diff with (and to revert to if needed)
# after first moving any previous copies.
# will need to manually cleanup dated backups occasionally
if [[ -d "${WORK_DIR}/sdkTempSave" ]]
then
  NOWDATE=$( date -u +%Y%m%d%H%M )
  NEWNAME=sdkTempSave${NOWDATE}
  mv "${WORK_DIR}/sdkTempSave" "${WORK_DIR}/${NEWNAME}"
  checkForErrorExit $? "could not mv sdkTempSave to ${NEWNAME}"
fi

# It won't exist, if first time running script, for example.
if [[ -e sdk ]]
then
  mv "${WORK_DIR}/sdk" "${WORK_DIR}/sdkTempSave"
  checkForErrorExit $? "could not mv sdk to sdkTempSave"
fi

rsync -r "${WORK_DIR}/tempeb/master/production/sdk/" "${WORK_DIR}/sdk"
checkForErrorExit $? "could not rsync -r ${WORK_DIR}/tempeb/master/production/sdk/ to ${WORK_DIR}/sdk"

# there won't be an sdkTempSave, if first time script is ran, for example.
if [[ -e "${WORK_DIR}/sdkTempSave" ]]
then
  if [[ -e "${WORK_DIR}/sdkdiffout.txt" ]]
  then
    # not positive why, but I've seen us get here, but NOWDATE not defined yet.
    # But could happen from various scenerios of deleting files or directories involved.
    if [[ -z "${NOWDATE}" ]]
    then
      NOWDATE=$( date -u +%Y%m%d%H%M )
    fi
    mv "${WORK_DIR}/sdkdiffout.txt" "${WORK_DIR}/sdkdiffout${NOWDATE}.txt"
    checkForErrorExit $? "could not mv sdkdiffout.txt to sdkdiffout${NOWDATE}.txt"
  fi
  diff -r ""${WORK_DIR}/sdk" "${WORK_DIR}/sdkTempSave" > "${WORK_DIR}/sdkdiffout.txt"
  # It's normal for diff to return '1', if differences are found. returns '0' if no differences found.
  # No need to 'exit' for either '0' or '1'.
  # Even '2' may or may not be ok, See "info diff".
  # So, we'll not check return codes for 'diff'.
  # checkForErrorExit $? "could not run diff"
fi

find "${WORK_DIR}/sdk" -name "*.sh" -exec chmod -c +x '{}' \;
checkForErrorExit $? "could not run find"

# cleanup
rm "${WORK_DIR}/master.zip"
checkForErrorExit $? "could not cleanup (rm) master.zip"
rm -fr "${WORK_DIR}/tempeb"
checkForErrorExit $? "could not cleanup (rm) tempeb"

if [[ -e "${WORK_DIR}/sdkdiffout.txt" ]]
then
  echo -e "\n\tNormal exit. Check sdkdiffout.txt to confirm expected differences were obtained.\n"
else
  echo -e "\n\tNormal exit."
fi

