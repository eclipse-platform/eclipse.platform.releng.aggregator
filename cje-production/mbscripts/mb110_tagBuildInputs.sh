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

reportDate=$(date +%s)
reportTimestamp=$(date +%Y%m%d-%H%M --date='@'$reportDate)
gitLogFile=$CJE_ROOT/$DROP_DIR/$BUILD_ID/gitLog.html
mkdir -p $CJE_ROOT/$DROP_DIR/$BUILD_ID

# set lastTag
lastTag=$(git describe --tags --match "${BUILD_TYPE}*" --abbrev=0)

pushd $CJE_ROOT/$AGG_DIR

# git tagging
# disable git push for now
#git submodule foreach "if grep \"^\${name}:\" ../../../streams/repositories_$PATCH_OR_BRANCH_LABEL.txt > /dev/null; then git tag $BUILD_ID; git push --verbose origin $BUILD_ID; else echo Skipping \$name; fi || :"
git submodule foreach "if grep \"^\${name}:\" ../../../streams/repositories_$PATCH_OR_BRANCH_LABEL.txt > /dev/null; then git tag $BUILD_ID; else echo Skipping \$name; fi || :"
git tag $BUILD_ID
# disable git push for now
#git push --verbose origin $BUILD_ID

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
