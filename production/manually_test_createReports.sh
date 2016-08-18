#!/usr/bin/env bash
#*******************************************************************************
# Copyright (c) 2016 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     David Williams - initial API and implementation
#*******************************************************************************

# This is a small utility to test "createReports.sh" since it depends on 
# only a few variables (and many assumptions!) that it can be run 
# standalone. 

# typically only BUILD_ID needs to be changed for a new test. 
export BUILD_ID=I20160816-1015

export BUILD_HOME=/shared/eclipse/builds
export TMP_DIR=/shared/eclipse/tmp
# for testing, asssume createReports.sh is in same directory as 
# manually_test_createReports.sh (Does not matter which directory 
# they are in). 
./createReports.sh
RC=$?
if [[ $RC != 0 ]] 
then 
    echo -e "\n\t[ERROR] createReports.sh returned non-zero error code: $RC"
fi


