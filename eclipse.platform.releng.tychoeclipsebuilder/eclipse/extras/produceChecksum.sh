# !/bin/sh

#array of zipfiles
zipfiles=`ls *.zip`

for zipfile in ${zipfiles}
do
  echo [md5] ${zipfile}
  md5sum -b ${zipfile} > checksum/${zipfile}.md5
  echo [sha1] ${zipfile}
  sha1sum -b ${zipfile} > checksum/${zipfile}.sha1
  echo [sha512] ${zipfile}
  sha512sum -b ${zipfile} > checksum/${zipfile}.sha512
done

#array of tar.gzip files
gzipfiles=`ls *.gz`

for gzipfile in ${gzipfiles}
do
  echo [md5] ${gzipfile}
  md5sum -b ${gzipfile} > checksum/${gzipfile}.md5
  echo [sha1] ${gzipfile}
  sha1sum -b ${gzipfile} > checksum/${gzipfile}.sha1
  echo [sha512] ${gzipfile}
  sha512sum -b ${gzipfile} > checksum/${gzipfile}.sha512
done


#array of .jar files
jarfiles=`ls *.jar`

for jarfile in ${jarfiles}
do
  echo [md5] ${jarfile}
  md5sum -b ${jarfile} > checksum/${jarfile}.md5
  echo [sha1] ${jarfile}
  sha1sum -b ${jarfile} > checksum/${jarfile}.sha1
  echo [sha512] ${jarfile}
  sha512sum -b ${jarfile} > checksum/${jarfile}.sha512
done
