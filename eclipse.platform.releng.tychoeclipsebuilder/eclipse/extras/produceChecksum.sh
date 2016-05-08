#!/usr/bin/env bash

source "${SCRIPT_PATH}/bashUtilities.shsource"
checkSumStart="$(date +%s )"
allCheckSumsMD5=checksum/MD5SUMS
allCheckSumsSHA1=checksum/SHA1SUMS
allCheckSumsSHA256=checksum/SHA256SUMS
allCheckSumsSHA512=checksum/SHA512SUMS

// Remove the "all" files, here at beginning if they all ready exist, 
// so that subsequent calls can all use append (i.e. ">>") 
if [[ -e ${allCheckSumsMD5} ]] 
then
  rm ${allCheckSumsMD5}
fi
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
  echo [md5] ${zipfile}
  md5sum -b ${zipfile} | tee checksum/${zipfile}.md5 >>${allCheckSumsMD5}
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
  echo [md5] ${gzipfile}
  md5sum -b ${gzipfile} | tee checksum/${gzipfile}.md5 >>${allCheckSumsMD5}
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
  echo [md5] ${jarfile}
  md5sum -b ${jarfile} | tee checksum/${jarfile}.md5 >>${allCheckSumsMD5}
  echo [sha1] ${jarfile}
  sha1sum -b ${jarfile} | tee checksum/${jarfile}.sha1 >>${allCheckSumsSHA1}
  echo [sha256] ${jarfile}
  sha256sum -b ${jarfile} | tee checksum/${jarfile}.sha256 >>${allCheckSumsSHA256}
  echo [sha512] ${jarfile}
  sha512sum -b ${jarfile} | tee checksum/${jarfile}.sha512 >>${allCheckSumsSHA512}
done
checkSumEnd="$(date +%s )"
elapsedTime $checkSumStart $checkSumEnd "Elapsed Time Checksums"

