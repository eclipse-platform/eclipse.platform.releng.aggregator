#!/usr/bin/env bash
echo "[DEBUG] Producing checksums starting"
echo "[DEBUG] current directory: ${PWD}"
if [[ -z "${SCRIPT_PATH}" ]]
then
  echo -e "\n\tWARNING: SCRIPT_PATH not defined in ${0##*/}"
else
  source "${SCRIPT_PATH}/bashUtilities.shsource"
  checkSumStart="$(date +%s )"
fi

# unclear if this script has access to build ID or not.
# Note: if it does, and becomes part of DL, then "promote script" will
# need to be modified to change the name.
if [[ -z "${BUILD_ID}" ]]
then
  allCheckSumsSHA256=checksum/SUMSSHA256
  allCheckSumsSHA512=checksum/SUMSSHA512
else
  allCheckSumsSHA256=checksum/${BUILD_ID}-SUMSSHA256
  allCheckSumsSHA512=checksum/${BUILD_ID}-SUMSSHA512
fi

#  Remove the "all" files, here at beginning if they all ready exist,
#  so that subsequent calls can all use append (i.e. ">>")

if [[ -e ${allCheckSumsSHA256} ]]
then
  rm ${allCheckSumsSHA256}
fi
if [[ -e ${allCheckSumsSHA512} ]]
then
  rm ${allCheckSumsSHA512}
fi

#array of zipfiles
zipfiles=`ls *.zip`

for zipfile in ${zipfiles}
do
  # There is one zip file to not list, eclipse.platform.releng.aggregator-<hash>.zip, which is merely
  # a collected utility scripts used to run unit tests.
  aggrPattern="^eclipse.platform.releng.aggregator.*.zip$"
  if [[ ! "${zipfile}" =~ $aggrPattern ]]
  then
    echo [sha256] ${zipfile}
    sha256sum -b ${zipfile} | tee checksum/${zipfile}.sha256 >>${allCheckSumsSHA256}
    echo [sha512] ${zipfile}
    sha512sum -b ${zipfile} | tee checksum/${zipfile}.sha512  >>${allCheckSumsSHA512}
  fi
done

#array of tar.gzip files
gzipfiles=`ls *.gz`

for gzipfile in ${gzipfiles}
do
  echo [sha256] ${gzipfile}
  sha256sum -b ${gzipfile} | tee checksum/${gzipfile}.sha256 >>${allCheckSumsSHA256}
  echo [sha512] ${gzipfile}
  sha512sum -b ${gzipfile} | tee checksum/${gzipfile}.sha512 >>${allCheckSumsSHA512}
done


#array of .jar files
jarfiles=`ls *.jar`

for jarfile in ${jarfiles}
do
  echo [sha256] ${jarfile}
  sha256sum -b ${jarfile} | tee checksum/${jarfile}.sha256 >>${allCheckSumsSHA256}
  echo [sha512] ${jarfile}
  sha512sum -b ${jarfile} | tee checksum/${jarfile}.sha512 >>${allCheckSumsSHA512}
done

if [[ -n "${SCRIPT_PATH}" ]]
then
  # This checkSums script is called twice, once while publishing Eclipse DL site, again
  # when publishing Equinox DL site. We use a simple heuristic to add "Eclipse" or "Equinox" to the output message.
  currentDirectory="${PWD}"
  equinoxPattern="^.*equinox.*$"
  eclipsePattern="^.*eclipse.*$"
  if [[ "${currentDirectory}" =~ $equinoxPattern ]]
  then
    area="Equinox"
  elif [[ "${currentDirectory}" =~ $eclipsePattern ]]
  then
    area="Eclipse"
  else
    area="[WARNING]: Unknown area"
  fi
  checkSumEnd="$(date +%s )"
  elapsedTime $checkSumStart $checkSumEnd "${area} Elapsed Time computing checksums"
fi
echo "[DEBUG] Producing checksums ended normally"
