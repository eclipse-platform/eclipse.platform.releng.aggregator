#!/bin/bash
#

DIR=$( dirname $0 )
NEW_VER=4.4.0-SNAPSHOT
if [ $# -eq 1 ]; then
  NEW_VER="$1"
fi

find * -name pom.xml -print0 >/tmp/files.txt

cat /tmp/files.txt | xargs -0 grep packaging.pom..packaging | cut -f1 -d: | sort -u >/tmp/t1_$$.txt
cat /tmp/files.txt | xargs -0 grep packaging.eclipse-repository..packaging | cut -f1 -d: | sort -u >>/tmp/t1_$$.txt

for POM in $( cat /tmp/t1_$$.txt ); do
  xsltproc --stringparam new-version "$NEW_VER" -o "${POM}.out" $DIR/fix-pom-version.xsl "${POM}"
  mv "${POM}.out" "${POM}"
done

find * -name pom.xml | sort -u >/tmp/t1_$$.txt

for POM in $( cat /tmp/t1_$$.txt ); do
  xsltproc --stringparam new-version "$NEW_VER" -o "${POM}.out" $DIR/fix-pom-parent-version.xsl "${POM}"
  mv "${POM}.out" "${POM}"
done

