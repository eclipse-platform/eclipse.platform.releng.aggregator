#!/usr/bin/env bash

# BUILD_ID is original I (or M) build on build machine that is being promoted
BUILD_ID=M20130131-0800
# DL_LABEL takes form of 3.8M7, 3.8RC4, 3.8, etc.
DL_LABEL=3.8.2
# DROP_TYPE either R for Releases
DROP_TYPE=R

./promoteRepo.sh ${BUILD_ID} ${DL_LABEL} ${DROP_TYPE}
