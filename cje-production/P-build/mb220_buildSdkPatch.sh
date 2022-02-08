#!/bin/bash

#*******************************************************************************
# Copyright (c) 2021 IBM Corporation and others.
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
set -e

if [ $# -ne 1 ]; then
  echo USAGE: $0 env_file
  exit 1
fi

source $CJE_ROOT/scripts/common-functions.shsource
source $1

if [[ -z "${WORKSPACE}" ]]
then
	MVN_ARGS=""
else
	MVN_ARGS="-Pbree-libs -Peclipse-sign"
fi

mkdir -p $CJE_ROOT/$TMP_DIR

cd $CJE_ROOT/gitCache/eclipse.platform.releng.aggregator
mvn -f eclipse.platform.releng.tychoeclipsebuilder/${PATCH_OR_BRANCH_LABEL}/pom.xml \
  clean verify -P${PATCH_OR_BRANCH_LABEL} \
  -DskipTests=true ${MVN_ARGS} \
  -Dtycho.debug.artifactcomparator \
  -Dtycho.localArtifacts=ignore \
  -Dcbi.jarsigner.continueOnFail=true \
  -Djgit.dirtyWorkingTree=error \
  -Dmaven.repo.local=$LOCAL_REPO \
  -Djava.io.tmpdir=$CJE_ROOT/$TMP_DIR \
  -DaggregatorBuild=true \
  -DbuildTimestamp=$TIMESTAMP \
  -DbuildType=$BUILD_TYPE \
  -DbuildId=$BUILD_ID \
  -Declipse-p2-repo.url=NOT_FOR_PRODUCTION_USE \
  ${JAVA_DOC_TOOL}
