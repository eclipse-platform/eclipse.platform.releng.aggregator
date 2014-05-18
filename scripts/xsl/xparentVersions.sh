#!/bin/bash
#

DIR=$( dirname $0 )
NEW_VER=0.14.0-SNAPSHOT
if [ $# -eq 1 ]; then
  NEW_VER="$1"
fi

find * -name pom.xml -print0 | xargs -0 grep "^<project>" | cut -f1 -d: | sort -u >/tmp/t1_$$.txt

for POM in $( cat /tmp/t1_$$.txt ); do
  sed 's!<project>!<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">!g' ${POM} >/tmp/out_$$.txt
  mv /tmp/out_$$.txt ${POM}
done


find * -name pom.xml -print0 | xargs -0 grep packaging.pom..packaging | cut -f1 -d: | sort -u >/tmp/t1_$$.txt
find * -name pom.xml -print0 | xargs -0 grep packaging.eclipse-repository..packaging | cut -f1 -d: | sort -u >>/tmp/t1_$$.txt

for POM in $( cat /tmp/t1_$$.txt ); do
  xsltproc --stringparam new-version "$NEW_VER" -o "${POM}.out" $DIR/fix-pom-version.xsl "${POM}"
  mv "${POM}.out" "${POM}"
done

find * -name pom.xml | sort -u >/tmp/t1_$$.txt

for POM in $( cat /tmp/t1_$$.txt ); do
  xsltproc --stringparam new-version "$NEW_VER" -o "${POM}.out" $DIR/fix-pom-parent-version.xsl "${POM}"
  mv "${POM}.out" "${POM}"
done

find * -name pom.xml -print0 | xargs -0 grep packaging.eclipse-plugin | cut -f1 -d: | sort -u >/tmp/t1_$$.txt
find * -name pom.xml -print0 | xargs -0 grep packaging.eclipse-feature | cut -f1 -d: | sort -u >>/tmp/t1_$$.txt
find * -name pom.xml -print0 | xargs -0 grep packaging.eclipse-test-plugin | cut -f1 -d: | sort -u >>/tmp/t1_$$.txt


for POM in $( cat /tmp/t1_$$.txt ); do
  POM_DIR=$( dirname $POM )
  cat >${POM_DIR}/forceQualifierUpdate.txt <<EOF
  # To force a version qualifier update add the bug here
  Bug 403352 - Update all parent versions to match our build stream
EOF
done

