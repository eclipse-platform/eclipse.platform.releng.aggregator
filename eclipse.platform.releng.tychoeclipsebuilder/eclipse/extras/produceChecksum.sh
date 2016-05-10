#!/usr/bin/env bash
echo "Producing checksums"
if [[ -z "${SCRIPT_PATH}" ]]
then
  echo -e "\n\tWARNING: SCRIPT_PATH not defined in ${0##*/}"
fi
source "${SCRIPT_PATH}/bashUtilities.shsource"
checkSumStart="$(date +%s )"

allCheckSumsSHA1=checksum/SUMSSHA1.txt
allCheckSumsSHA256=checksum/SUMSSHA256.txt
allCheckSumsSHA512=checksum/SUMSSHA512.txt

#  Remove the "all" files, here at beginning if they all ready exist, 
#  so that subsequent calls can all use append (i.e. ">>") 

if [[ -e ${allCheckSumsSHA1} ]] 
then
  rm ${allCheckSumsSHA1}
fi
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
  echo [sha1] ${zipfile}
  sha1sum -b ${zipfile} | tee checksum/${zipfile}.sha1 >>${allCheckSumsSHA1}
  echo [sha256] ${zipfile}
  sha256sum -b ${zipfile} | tee checksum/${zipfile}.sha256 >>${allCheckSumsSHA256}
  echo [sha512] ${zipfile}
  sha512sum -b ${zipfile} | tee checksum/${zipfile}.sha512  >>${allCheckSumsSHA512}
done

#array of tar.gzip files
gzipfiles=`ls *.gz`

for gzipfile in ${gzipfiles}
do
  echo [sha1] ${gzipfile}
  sha1sum -b ${gzipfile} | tee checksum/${gzipfile}.sha1 >>${allCheckSumsSHA1}
  echo [sha256] ${gzipfile}
  sha256sum -b ${gzipfile} | tee checksum/${gzipfile}.sha256 >>${allCheckSumsSHA256}
  echo [sha512] ${gzipfile}
  sha512sum -b ${gzipfile} | tee checksum/${gzipfile}.sha512 >>${allCheckSumsSHA512}
done


#array of .jar files
jarfiles=`ls *.jar`

for jarfile in ${jarfiles}
do
  echo [sha1] ${jarfile}
  sha1sum -b ${jarfile} | tee checksum/${jarfile}.sha1 >>${allCheckSumsSHA1}
  echo [sha256] ${jarfile}
  sha256sum -b ${jarfile} | tee checksum/${jarfile}.sha256 >>${allCheckSumsSHA256}
  echo [sha512] ${jarfile}
  sha512sum -b ${jarfile} | tee checksum/${jarfile}.sha512 >>${allCheckSumsSHA512}
done

checkSumEnd="$(date +%s )"
elapsedTime $checkSumStart $checkSumEnd "Elapsed Time Checksums"

