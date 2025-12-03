#!/bin/bash -x
#*******************************************************************************
# Copyright (c) 2017, 2025 IBM Corporation and others.
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

if [ $# -ne 1 ]; then
  echo USAGE: $0 client
  exit 1
fi
client=$1

echo "[DEBUG] Producing checksums starting"
echo "[DEBUG] current directory: ${PWD}"

mkdir checksum

allCheckSumsSHA512=checksum/${client}-${BUILD_ID}-SUMSSHA512
fileExtensionsToHash='zip dmg gz tar.xz jar'

#  Remove the "all" files, here at beginning if they all ready exist,
#  so that subsequent calls can all use append (i.e. ">>")

rm -f ${allCheckSumsSHA512}

for extension in ${fileExtensionsToHash}; do
  files=$(ls *.${extension})
  for file in ${files}; do
    # There is one zip file to not list, eclipse.platform.releng.aggregator-<hash>.zip, which is merely
    # a collected utility scripts used to run unit tests.
    aggrPattern="^eclipse.platform.releng.aggregator.*.zip$"
    if [[ ! "${file}" =~ $aggrPattern ]]; then
      echo [sha512] ${file}
      sha512sum -b ${file} | tee checksum/${file}.sha512 >>${allCheckSumsSHA512}
    fi
  done
done

# We'll always try to sign checksum files, if passphrase file exists
echo "[DEBUG] Producing GPG signatures starting."
set -e
if [ ! -z "${KEYRING_PASSPHRASE}" ]
then
    #import gpg keys in fresh gpg-homedir
    gpg_home="${WORKSPACE}/tools/${client}/gpg/"
    mkdir -p ${gpg_home}
    alias gpg='gpg --homedir "${gpg_home}"'
    gpg --batch --import "${KEYRING}"
    for fpr in $(gpg --list-keys --with-colons | awk -F: '/fpr:/ {print $10}' | sort -u); do
      echo -e "5\ny\n" | gpg --batch --command-fd 0 --expert --edit-key "${fpr}" trust;
    done
    
    gpg --detach-sign --armor --output ${allCheckSumsSHA512}.asc --batch --pinentry-mode loopback --passphrase-fd 0 ${allCheckSumsSHA512} <<< "${KEYRING_PASSPHRASE}"
else
    # We don't treat as ERROR since would be normal in a "local build".
    # But, would be an ERROR in production build so could be improved.
    echo -e "\n\t[WARNING] The key_passphrase_file did not exist or was not readable.\n"
fi
echo "[DEBUG] Producing checksums ended normally"
