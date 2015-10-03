#!/usr/bin/env bash

if [[ -z "${WORKSPACE}" ]]
then
  echo -e "\n\tERROR: WORKSPACE variable was not defined"
  exit 1
else
  if [[ ! -d "${WORKSPACE}" ]]
  then
    echo -e "\n\tERROR: WORKSPACE was defined, but did not exist?"
    echo -e "\t\tIt was defined as ${WORKSPACE}"
    exit 1
  else
    echo -e "\n\tINFO: WORKSPACE was defined as ${WORKSPACE}"
    echo -e "\t\tWill delete contents, for clean run"
    MaxLoops=15
    SleepTime=60
    currentLoop=0
    nFilesOrDirs=$( find "${WORKSPACE}" -mindepth 1 -maxdepth 1 | wc -l )
    while [[ ${nFilesOrDirs} > 0 ]]
    do
      currentLoop=$(( ${currentLoop} + 1 ))
      if [[ ${currentLoop} -gt ${MaxLoops} ]]
      then
        echo -e "\n\tERROR: Number of re-try loops, ${currentLoop}, exceeded maximum, ${MaxLoops}. "
        echo -e " \t\tPossibly due to files still being used by another process?"
        exit 1
        break
      fi
      echo -e "\tcurrentLoop: ${currentLoop}   nFilesOrDirs:  ${nFilesOrDirs}"
      find "${WORKSPACE}" -mindepth 1 -maxdepth 1 -execdir rm -fr '{}' \;
      nFilesOrDirs=$( find "${WORKSPACE}" -mindepth 1 -maxdepth 1 | wc -l )
      if [[ ${nFilesOrDirs} -gt 0 ]]
      then
        sleep ${SleepTime}
      fi
    done
  fi
fi
echo -e "\t... ending cleaning"

exit 0


