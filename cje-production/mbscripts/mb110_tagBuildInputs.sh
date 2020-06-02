#!/bin/bash

#*******************************************************************************
# Copyright (c) 2019, 2020 IBM Corporation and others.
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

reportDate=$(TZ="America/New_York" date +%s)
reportTimestamp=$(TZ="America/New_York" date +%Y%m%d-%H%M --date='@'$reportDate)
gitLogFile=$CJE_ROOT/$DROP_DIR/$BUILD_ID/gitLog.html
mkdir -p $CJE_ROOT/$DROP_DIR/$BUILD_ID

epUpdateDir=/home/data/httpd/download.eclipse.org/eclipse/updates
updateSiteRootPath=${epUpdateDir}/${STREAMMajor}.${STREAMMinor}-${BUILD_TYPE}-builds

# try to find the last tag of the current build type that is available as a promoted build
# by checking the most recent 5 tags and seeing if an update site for it exists
lastTagList=$(git tag --list "${BUILD_TYPE}*" | tail -n5)
lastTag=
for lt in $lastTagList ; do
  if ssh genie.releng@projects-storage.eclipse.org test -d ${updateSiteRootPath}/${lt} ; then
    lastTag=$lt
  fi
done
# if no build is promoted yet, then just fallback to the last tag of the current build type
if [[ -z "$lastTag" ]] ; then
  lastTag=$(git describe --tags --match "${BUILD_TYPE}*" --abbrev=0)
fi

pushd $CJE_ROOT/$AGG_DIR

# git tagging
git commit -m "Build input for build $BUILD_ID"
if [[ $? -eq 0 ]]
then
	git push origin HEAD
fi

git submodule foreach "if grep \"^\${name}:\" ../../../streams/repositories_$PATCH_OR_BRANCH_LABEL.txt > /dev/null; then git tag $BUILD_ID; git push --verbose origin $BUILD_ID; else echo Skipping \$name; fi || :"
git tag $BUILD_ID
git push --verbose origin $BUILD_ID

# git logging
if [[ -n "$lastTag" ]]; then
  tmpGitLog=$CJE_ROOT/$TMP_DIR/gitLog.txt
  echo -e "<h2>Git log from $lastTag (previous) to $BUILD_ID (current)</h2>" > $gitLogFile
  echo -e "<h2>The tagging, and this report, were done at about $reportTimestamp</h2>" >> $gitLogFile
  git log $lastTag..$BUILD_ID --date=short --format=format:"<tr><td class=\"datecell\">%cd</td><td class=\"commitcell\"><a href=\"https://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/commit/?id=%H\">%s</a></td><td class=\"authorcell\">%aN</td></tr>" > $tmpGitLog
  tmpFileSize=$(stat -c%s $tmpGitLog)
  if [ $tmpFileSize -ne 0 ]; then
    echo "<table width=\"80%\"><tbody> <tr><th class=\"cell\" colspan=\"3\">Repository: eclipse.platform.releng.aggregator</th></tr>" >> $gitLogFile
    echo "<tr> <th class=\"datecell\">Date</th> <th class=\"commitcell\">Commit message</th> <th class=\"authorcell\">Author</th> </tr>" >> $gitLogFile
    cat $tmpGitLog >> $gitLogFile
    echo "</tbody></table><br><br>" >> $gitLogFile
    echo >> $gitLogFile
  fi
  git submodule --quiet foreach "comp=\$(echo \$path|cut -d. -f2);git log $lastTag..$BUILD_ID --date=short --format=format:\"<tr><td class=\\\"datecell\\\">%cd</td><td class=\\\"commitcell\\\"><a href=\\\"https://git.eclipse.org/c/\$comp/\$path.git/commit/?id=%H\\\">%s</a></td><td class=\\\"authorcell\\\">%aN</td></tr>\">$tmpGitLog;FILESIZE=\$(stat -c%s $tmpGitLog);if [ \$FILESIZE -ne 0 ]; then echo \"<table width=\\\"80%\\\"><tbody> <tr><th class=\\\"cell\\\" colspan=\\\"3\\\">Repository: \$path</th></tr>\";echo \"<tr> <th class=\\\"datecell\\\">Date</th> <th class=\\\"commitcell\\\">Commit message</th> <th class=\\\"authorcell\\\">Author</th> </tr>\";cat $tmpGitLog;echo \"</tbody></table><br><br>\";echo;fi" >> $gitLogFile
else
  echo -e "\n\tGit log not generated because a reasonable previous tag could not be found." > $gitLogFile
fi

popd
