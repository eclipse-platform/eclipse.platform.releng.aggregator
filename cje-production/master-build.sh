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
#     Sravan Kumar Lakkimsetti - initial API and implementation
#*******************************************************************************

source $WORKSPACE/cje-production/scripts/common-functions.shsource

pushd mbscripts
for i in $(ls | sort)
do
  fn-run-command ./$i $WORKSPACE/cje-production/buildproperties.shsource
done
popd