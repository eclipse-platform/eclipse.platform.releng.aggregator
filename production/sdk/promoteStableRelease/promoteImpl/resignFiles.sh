#!/usr/bin/env bash

#Since the names of the files in the checksum files changed, 
#the file-of-checksums must be resigned to be valid. Otherwise, 
#appears "tampered with" (since it has been tampered with :) 

# During promote, this file is called from "renameBuild.sh"

echo "[DEBUG] Re-producing GPG signatures starting."

# in early scripts we cd to the parent of the directory 
# we are changing. dirname is normally "DROP_ID".
renameDir=${renameDir:-"${PWD}/${dirname}"}
equinoxPattern="^.*equinox.*$"
eclipsePattern="^.*eclipse.*$"
if [[ "${renameDir}" =~ $equinoxPattern ]]
then
  client="equinox"
  buildLabel="${BUILD_LABEL_EQ}"
elif [[ "${renameDir}" =~ $eclipsePattern ]]
then
  client="eclipse"
  buildLabel="${BUILD_LABEL}"
else
  echo "\n\t[ERROR]: Unknown client: ${client} in ${0##*/}\n"
  exit 1
fi
# note, at this point, the file name itself is still the old filename. 
# will will be renamed in a later stage.
allCheckSumsSHA256=checksum/${client}-${buildLabel}-SUMSSHA256
allCheckSumsSHA512=checksum/${client}-${buildLabel}-SUMSSHA512

key_passphrase_file=${key_passphrase_file:-${HOME}/${client}-dev.passphrase}

signer=${signer:-${client}-dev@eclipse.org}
signature_file256=${allCheckSumsSHA256}.asc
signature_file512=${allCheckSumsSHA512}.asc
fileToSign256=${allCheckSumsSHA256}
fileToSign512=${allCheckSumsSHA512}

cat ${key_passphrase_file} | gpg --local-user ${signer} --sign --armor --output ${signature_file256} --batch --yes --passphrase-fd 0 --detach-sig ${fileToSign256}
cat ${key_passphrase_file} | gpg --local-user ${signer} --sign --armor --output ${signature_file512} --batch --yes --passphrase-fd 0 --detach-sig ${fileToSign512}


echo "[DEBUG] Re-producing GPG signatures ended."