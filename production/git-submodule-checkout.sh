#!/bin/bash
#

REPOSITORIES_TXT="$1"; shift
name="$1"; shift

VAL=$( grep "^${name}:" "$REPOSITORIES_TXT" | cut -f2 -d" ")

 # Here we count on $BUILD_TYPE being exported. TODO: make parameter later? 
if [[ -n "$BUILD_TYPE" && "$BUILD_TYPE" == "N" ]] 
then
    echo "INFO: Branch forced to master, instead of $VAL, since doing N-Build"
    VAL="master"
fi


if [ -z "$VAL" ]; then
	echo No tag or branch specified for $name
	exit
fi

git fetch

if [ -z "$(git tag -l $VAL)" ]; then
	echo Updating branch $VAL
	git checkout $VAL
	git pull
else
	echo Updating to tag $VAL
	git checkout $VAL
fi
