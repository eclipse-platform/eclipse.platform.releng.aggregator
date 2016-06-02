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

# Utility function to convert repo site metadata files to XZ compressed files. 
# One use case is to invoke with something similar to
#
# find . -maxdepth 3  -name content.jar  -execdir convertxz.sh '{}' \;

source ${HOME}/bin/createXZ.shsource
# add -noforce if we should leave existing xml.xz files alone. (such as from a cron job, 
# that repeatedly checks a directory tree via 'find', etc.
createXZ ${PWD} -
