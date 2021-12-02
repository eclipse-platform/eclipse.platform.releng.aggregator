#!/bin/bash

#import gpg keys
gpg --batch --import "${KEYRING}"
for fpr in $(gpg --list-keys --with-colons  | awk -F: '/fpr:/ {print $10}' | sort -u);
do
  echo -e "5\ny\n" |  gpg --batch --command-fd 0 --expert --edit-key "${fpr}" trust;
done