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
#     Sravan Lakkimsetti - initial API and implementation
#*******************************************************************************
reSign ()
{
  fileName=$1
  mkdir tempLauncher
  pushd tempLauncher

  zipfile=$(basename $fileName)
  cp ../${fileName} ${zipfile}.zip
  unzip ${zipfile}.zip
  #sign files
  copy ../../win-sign/winSignPom.xml pom.xml
  mvn clean verify

  zip ${zipfile}new.zip *.exe
  cp ${zipfile}new.zip ../${fileName}
  popd
  rm -r tempLauncher
}

pwd

for i in $(ls target/repository/binary/*win32*)
do
  reSign $i
done
