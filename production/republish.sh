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


# This script is NOT part of production builds. It is an assist in "republishing" 
# results, such as if there is a connection problem, or a script needs to be fixed to 
# publish correctly. Even if this script "works", it does not change the files on 
# download server, they much be "re-synch'd" manually. 

# This script is only and "assist" to republish. It is not fool proof (and not well tested) 
# and its exact use depends on what ever the problem was. 

BUILD_ID=I20160527-2118
BUILD_ROOT=/shared/eclipse/builds
BUILD_MAJOR=4
BUILD_TYPE=I
postingDirectory=${BUILD_ROOT}/${BUILD_MAJOR}${BUILD_TYPE}/siteDir/eclipse/downloads/drops4
buildDirectory="${postingDirectory}/${BUILD_ID}"
echo "BUILD_ID: $BUILD_ID" 1>&2

# basebuilder (and tools) in following dir needs usually to be removed if connection problmes, or changes made
rm -vfr "${buildDirectory}/org.eclipse.releng.basebuilder"

# aggregator needs to be removed if changes to scripts there. 
# The zip needs to be republished do "downloads" if re-running tests involved (rare) but seldom hurts
rm -vfr "${buildDirectory}"/eclipse.platform.releng.aggregator
rm -vfr "${buildDirectory}"/eclipse.platform.releng.aggregator*.zip

# In some cases (rarely) may have to remove index.php (it will not be recreated by default)
# rm "${buildDirectory}/index.php"

# In some (rare) cases may have to remove compilelogs (will not be recreated by default).
# location?

# In some (most) cases may have to do a "git pull" on aggregator, to get a fix (but not recursively!)
pushd "${BUILD_ROOT}/${BUILD_MAJOR}${BUILD_TYPE}/gitCache/eclipse.platform.releng.aggregator" 1>&2
git pull
newHASH=$(git rev-parse HEAD)
echo "newHASH (after pull): $newHASH" 1>&2
popd 1>&2

# If git pull is done, need to update EBUILDER_HASH in buildproperties.shsource
oldPattern="\(export EBUILDER_HASH=\"\)\(.*\)\(\"\)"
replacePattern="\1${newHASH}\3"
replaceDirCommand="s!${oldPattern}!${replacePattern}!g" 
echo "[DEBUG] replaceCommnad: ${replaceDirCommand}" 1>&2
sed  -e "${replaceDirCommand}" "${buildDirectory}/buildproperties.shsource" > "${buildDirectory}/buildproperties.shsourceTEMPNEW"
RC=$?
if [[ $RC != 0 ]]
then 
  echo "sed returned non-zero return code: $RC" 1>&2
  exit $RC
else
  cp --backup=numbered "${buildDirectory}/buildproperties.shsourceTEMPNEW" "${buildDirectory}/buildproperties.shsource"
  #TODO check return code of cp
  echo "replaced original buildproperties.shsource (after backing up original)"
fi

# sometimes, but rearly, the production directory may need to be replaced changes made.
export SCRIPT_PATH="${BUILD_ROOT}/${BUILD_MAJOR}${BUILD_TYPE}/production"
# now republish! 
${SCRIPT_PATH}/publish-eclipse.sh ${buildDirectory}/buildproperties.shsource 2>&1 | tee republishout.txt


