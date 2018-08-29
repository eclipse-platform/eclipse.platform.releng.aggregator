#!/usr/bin/env bash
#*******************************************************************************
# Copyright (c) 2016 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     David Williams - initial API and implementation
#*******************************************************************************

#Since the names of the files in the checksum files changed,
#the file-of-checksums must be resigned to be valid. Otherwise,
#appears "tampered with" (since it has been tampered with :)

# During promote, this file is called from "renameBuild.sh"

echo "[DEBUG] Re-producing GPG signatures starting."

# in early scripts we cd to the parent of the directory
# we are changing. We need "whole name" (at least PWD)
# to find proper client. Note: "buildLabel" is almost always
# the same for equinox and eclipse, at this point. But we
# set to their "global" values, just in case that changes.
renameDir=${renameDir:-"${PWD}/${newdirname}"}
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
  echo -e "\n\t[ERROR]: Unknown client: ${client} in ${0##*/}\n"
  exit 1
fi

echo -e "\n\t == properties just before GPG signing ==\n"
echo -e "\t\t client: $client"
echo -e "\t\tPWD: $PWD"
printf "\t\toldname: ${oldname}\n"
printf "\t\toldlabel: ${oldlabel}\n"
printf "\t\tnewdirname: ${newdirname}\n"
printf "\t\tnewlabel: ${newlabel}\n"
printf "\t\tdirname: ${dirname}\n\n"

# note, at this point, the file name itself is still the old filename.
# will will be renamed in a later stage. but we should change only
# the files in "newdirname" (set and exported from rename build).
allCheckSumsSHA256=${dirname}/checksum/${client}-${buildLabel}-SUMSSHA256
allCheckSumsSHA512=${dirname}/checksum/${client}-${buildLabel}-SUMSSHA512

key_passphrase_file=${key_passphrase_file:-${HOME}/${client}-dev.passphrase}

signer=${signer:-${client}-dev@eclipse.org}
signature_file256=${allCheckSumsSHA256}.asc
signature_file512=${allCheckSumsSHA512}.asc
fileToSign256=${allCheckSumsSHA256}
fileToSign512=${allCheckSumsSHA512}

cat ${key_passphrase_file} | gpg --local-user ${signer} --sign --armor --output ${signature_file256} --batch --yes --passphrase-fd 0 --detach-sig ${fileToSign256}
cat ${key_passphrase_file} | gpg --local-user ${signer} --sign --armor --output ${signature_file512} --batch --yes --passphrase-fd 0 --detach-sig ${fileToSign512}


echo "[DEBUG] Re-producing GPG signatures ended."
