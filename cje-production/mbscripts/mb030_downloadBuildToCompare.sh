#!/bin/bash -x

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

mkdir -p $CJE_ROOT/$TMP_DIR
pushd $CJE_ROOT/$TMP_DIR
$CJE_ROOT/$BASEBUILDER_DIR/eclipse -nosplash -application org.eclipse.equinox.p2.artifact.repository.mirrorApplication -source https://$DOWNLOAD_HOST/eclipse/updates/$PREVIOUS_RELEASE_VERSION/$PREVIOUS_RELEASE_ID/ -destination file:$CJE_ROOT/$TMP_DIR/$PREVIOUS_RELEASE_ID/
popd
