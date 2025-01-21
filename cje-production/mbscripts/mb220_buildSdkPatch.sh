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

if [[ -z "${WORKSPACE}" ]]
then
	MVN_ARGS=""
else
	MVN_ARGS="-Pbree-libs -Peclipse-sign"
fi

mkdir -p $CJE_ROOT/$TMP_DIR
cd $CJE_ROOT/gitCache/eclipse.platform.releng.aggregator
if [ -z "$PATCH_BUILD" ];then
	mvn clean install -pl :eclipse-sdk-prereqs,:org.eclipse.jdt.core.compiler.batch \
		-DlocalEcjVersion=99.99 \
		-Dmaven.repo.local=$LOCAL_REPO -DcompilerBaselineMode=disable -DcompilerBaselineReplace=none
	MVN_ARGS="${MVN_ARGS} -Dcbi-ecj-version=99.99"
else
	MVN_ARGS="${MVN_ARGS} -f eclipse.platform.releng.tychoeclipsebuilder/${PATCH_OR_BRANCH_LABEL}/pom.xml -P${PATCH_OR_BRANCH_LABEL}"
fi

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
  -Declipse-p2-repo.url=NOT_FOR_PRODUCTION_USE \
  -e \
  -T 1C \
  ${JAVA_DOC_TOOL}
