#!/usr/bin/env bash
#*******************************************************************************
# Copyright (c) 2016 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     David Williams - initial API and implementation
#*******************************************************************************

source build_eclipse_org.shsource
source build-functions.shsource
export buildDirectory=$( fn-build-dir "$BUILD_ROOT" "$BUILD_ID" "$STREAM" )
mkdir -p $buildDirectory
echo buildDirectory: $buildDirectory

test=1
checkNArgs 3 3
if [[ $? == 0 ]]; then echo "test $test passed"; else echo "test $test failed"; fi

test=2
checkNArgs 2 3
if [[ $? != 0 ]]; then echo "test $test passed"; else echo "test $test failed"; fi

test=3
checkNArgs 3 1 3
if [[ $? == 0 ]]; then echo "test $test passed"; else echo "test $test failed"; fi

test=4
checkNArgs 2 1 3
if [[ $? == 0 ]]; then echo "test $test passed"; else echo "test $test failed"; fi

test=5
checkNArgs 3 1 2
if [[ $? != 0 ]]; then echo "test $test passed"; else echo "test $test failed"; fi
