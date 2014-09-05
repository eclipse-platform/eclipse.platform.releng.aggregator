#!/usr/bin/env bash

# directly gets a fresh copy of sdk directory from eclipsebuilder
# need to manually check and make sure nothing is running or will
# be running soon.

# codifying the branch (or tag) to use, so it can be set/chagned in one place
branch=master
initScriptTag="h=$branch"

# to use a tag instead of branch, would be tag=X, such as
# tag=vI20120417-0700, or in full form
# http://git.eclipse.org/c/platform/eclipse.platform.releng.eclipsebuilder.git/plain/scripts/wgetFresh.sh?tag=vI20120417-0700

# = = = = = = =

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

cd /shared/eclipse
checkForErrorExit $? "could not change directory to /shared/eclipse."

# remove if exists from previous (failed) run
if [[ -e tempeb ]]
then
  rm -fr tempeb/
  checkForErrorExit $? "Could not remove contents of tempeb"
else
  mkdir  tempeb
  checkForErrorExit $? "could not mkdir tempeb"
fi

# ditto
if [[ -e master.zip ]]
then
  rm master.zip
  checkForErrorExit $? "Could not remove master.zip"
fi

#wget -O master.zip http://git.eclipse.org/c/platform/eclipse.platform.releng.eclipsebuilder.git/snapshot/master.zip
wget -O master.zip http://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/snapshot/master.zip
checkForErrorExit $? "could not get aggregator?!"

unzip -q ${branch}.zip -d tempeb
checkForErrorExit $? "could not unzip master?!"

# save a copy to diff with (and to revert to if needed)
# after first moving any previous copies.
# will need to manually cleanup dated backups occasionally
if [[ -d sdkTempSave ]]
then
  NOWDATE=$( date -u +%Y%m%d%H%M )
  NEWNAME=sdkTempSave${NOWDATE}
  mv sdkTempSave ${NEWNAME}
  checkForErrorExit $? "could not mv sdkTempSave to ${NEWNAME}"
fi

# It won't exist, if first time running script, for example.
if [[ -e sdk ]]
then
  mv sdk sdkTempSave
  checkForErrorExit $? "could not mv sdk to sdkTempSave"
fi

rsync -r tempeb/master/production/sdk/ sdk
checkForErrorExit $? "could not rsync -r tempeb/master/production/sdk/ to sdk"

# won't be a sdkTempSave, if first time script is ran, for example.
if [[ -e sdkTempSave ]]
then
  if [[ -e sdkdiffout.txt ]]
  then
    # note positive why, but I've seen us get here, but NOWDATE not defined yet.
    # But could happen from various scenerios of deleting files or directories involved.
    if [[ -z "${NOWDATE}" ]]
    then
        NOWDATE=$( date -u +%Y%m%d%H%M )
    fi
    mv sdkdiffout.txt sdkdiffout${NOWDATE}.txt
    checkForErrorExit $? "could not mv sdkdiffout.txt to sdkdiffout${NOWDATE}.txt"
  fi
  diff -r sdk sdkTempSave > sdkdiffout.txt
  checkForErrorExit $? "could not run diff"
fi

find /shared/eclipse/sdk -name "*.sh" -exec chmod -c +x '{}' \;
checkForErrorExit $? "could not run find"

# cleanup
rm master.zip
checkForErrorExit $? "could not cleanup (rm) master.zip"
rm -fr tempeb
checkForErrorExit $? "could not cleanup (rm) tempeb"

if [[ -e sdkdiffout.txt ]]
then
  echo -e "\n\tNormal exit. Check sdkdiffout.txt to confirm expected differences were obtained.\n"
else
  echo -e "\n\tNormal exit."
fi

