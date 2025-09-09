#!/bin/bash -e

#*******************************************************************************
# Copyright (c) 2019, 2025 IBM Corporation and others.
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

if [[ -z "${WORKSPACE}" ]]; then
	MVN_ARGS=""
else
	MVN_ARGS="-Pbree-libs -Peclipse-sign"
fi

mkdir -p $CJE_ROOT/$TMP_DIR
cd $CJE_ROOT/gitCache/eclipse.platform.releng.aggregator
mvn clean install -pl :eclipse-sdk-prereqs,:org.eclipse.jdt.core.compiler.batch \
	-DlocalEcjVersion=99.99 \
	-Dmaven.repo.local=$LOCAL_REPO -DcompilerBaselineMode=disable -DcompilerBaselineReplace=none

mvn clean verify -DskipTests=true ${MVN_ARGS} \
  -Dtycho.debug.artifactcomparator \
  -Dtycho.localArtifacts=ignore \
  -Dcbi.jarsigner.continueOnFail=true \
  -Dtycho.pgp.signer=bc -Dtycho.pgp.signer.bc.secretKeys="${KEYRING}" \
  -Djgit.dirtyWorkingTree=error \
  -Dmaven.repo.local=$LOCAL_REPO \
  -Djava.io.tmpdir=$CJE_ROOT/$TMP_DIR \
  -DbuildTimestamp=$TIMESTAMP \
  -DbuildType=$BUILD_TYPE \
  -DbuildId=$BUILD_ID \
  -Dcbi-ecj-version=99.99 \
  -e \
  -T 1C
