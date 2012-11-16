#!/bin/bash
#

REPOSITORIES_TXT="$1"; shift
name="$1"; shift

VAL=$( grep "^${name}:" "$REPOSITORIES_TXT" | cut -f2 -d" ")
if [ -z "$VAL" ]; then
	echo No tag or branch specified for $name
	exit
fi

git fetch

if [ -z "$(git tag -l $VAL)"]; then
	echo Updating branch $VAL
	git checkout $VAL
	git pull
else
	echo Updating to tag $VAL
	git checkout $VAL
fi
