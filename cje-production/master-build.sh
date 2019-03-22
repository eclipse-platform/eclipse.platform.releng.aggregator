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

export CJE_ROOT=${CJE_ROOT:-`pwd`}
source $CJE_ROOT/scripts/common-functions.shsource

chmod -R +x .

pushd mbscripts
for i in $(ls | sort)
do
  fn-run-command ./$i $CJE_ROOT/buildproperties.shsource
done
popd
