#!/bin/bash

#*******************************************************************************
# Copyright (c) 2019 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Kit Lo - initial API and implementation
#*******************************************************************************

if [ $# -ne 1 ]; then
  echo USAGE: $0 env_file
  exit 1
fi

source $CJE_ROOT/scripts/common-functions.shsource
source $1

REPO_DIR=$CJE_ROOT/gitCache/eclipse.platform.releng.aggregator
BUILD_DIR=$CJE_ROOT/$DROP_DIR/$BUILD_ID
mkdir $CJE_ROOT/tmp

cd $REPO_DIR
mvn --update-snapshots org.eclipse.tycho:tycho-versions-plugin:2.0.0-SNAPSHOT:update-pom \
  -Dmaven.repo.local=$LOCAL_REPO \
  -Djava.io.tmpdir=$CJE_ROOT/tmp \
  -DaggregatorBuild=true \
  -DbuildTimestamp=$TIMESTAMP \
  -DbuildType=$BUILD_TYPE \
  -DbuildId=$BUILD_ID \
  -Declipse-p2-repo.url=NOT_FOR_PRODUCTION_USE

RC=$?
if [[ $RC != 0 ]]
then
  echo "ERROR: tycho-versions-plugin:update-pom returned non-zero return code: $RC" >&2
else
  changes=$( git status --short -uno | cut -c4- )
  if [ -z "$changes" ]; then
    echo "INFO: No changes in pom versions" >&2
    RC=0
  else
    echo "INFO: Changes in pom versions: $changes" >&2
    RC=0
  fi
fi

pushd "$REPO_DIR"
POM_UPDATES_SUBJECT=" "
POM_UPDATES=" "
mkdir -p "$BUILD_DIR"/pom_updates
git submodule foreach "if (git status -s -uno | grep pom.xml >/dev/null ); then git diff >$BUILD_DIR/pom_updates/\$name.diff; fi "
pushd "$BUILD_DIR"/pom_updates
nDiffs=$( ls -1 $BUILD_DIR/pom_updates/*.diff | wc -l )
# do not create index.html if no diffs to display, as our PHP DL page knows
# not to display link if index.html is not present.
if (( $nDiffs > 0 ))
then
    POM_UPDATES=""
    echo "<html>"  >index.html
    echo "<head>"  >>index.html
    echo "<title>POM version report for $BUILD_ID</title>"  >>index.html
    echo "</head>"  >>index.html
    echo "<body>"  >>index.html
    echo "<h1>POM version report for $BUILD_ID</h1>"  >>index.html
    echo "<p>These repositories need patches to bring their pom.xml files up to the correct version.</p>"  >>index.html
    echo "<ul>"  >>index.html

    for f in *.diff; do
        FNAME=$( basename $f .diff )
        echo "<li><a href=\"$f\">$FNAME</a></li>" >> index.html
    done
    echo "</ul>" >> index.html
    echo "</html>" >> index.html
    POM_UPDATES="Check POM Updates made by tycho's pom-updater plugin:<br>    https://download.eclipse.org/eclipse/downloads/drops4/${BUILD_ID}/pom_updates/<br><br>"
    POM_UPDATES_SUBJECT=" - POM Updates Required"
fi
popd
popd
# we write to property files, for later use in email message
fn-write-property POM_UPDATES_BODY "\"${POM_UPDATES}\""
fn-write-property POM_UPDATES_SUBJECT "\"${POM_UPDATES_SUBJECT}\""

