#!/usr/bin/env bash
echo "Producing checksums"
if [[ -z "${SCRIPT_PATH}" ]]
then
  echo -e "\n\tWARNING: SCRIPT_PATH not defined in ${0##*/}"
else
  source "${SCRIPT_PATH}/bashUtilities.shsource"
  checkSumStart="$(date +%s )"
fi

allCheckSumsSHA256=checksum/SUMSSHA256.txt
allCheckSumsSHA512=checksum/SUMSSHA512.txt

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
  echo [sha256] ${zipfile}
  sha256sum -b ${zipfile} | tee checksum/${zipfile}.sha256 >>${allCheckSumsSHA256}
  echo [sha512] ${zipfile}
  sha512sum -b ${zipfile} | tee checksum/${zipfile}.sha512  >>${allCheckSumsSHA512}
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
  # which publishing equinox DL site. We use a simple heuristic to add "Eclipse" or "Equinox" to the output message.
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
