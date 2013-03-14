#!/bin/bash
#

DIR=$( dirname $0 )

find * -name pom.xml -print0 | xargs -0 grep packaging.pom..packaging | cut -f1 -d: | sort -u >/tmp/t1_$$.txt

for POM in $( cat /tmp/t1_$$.txt ); do
xsltproc -o "${POM}.out" $DIR/fix-pom-version.xsl "${POM}"
mv "${POM}.out" "${POM}"
done

find * -name pom.xml | sort -u >/tmp/t1_$$.txt

for POM in $( cat /tmp/t1_$$.txt ); do
xsltproc -o "${POM}.out" $DIR/fix-pom-parent-version.xsl "${POM}"
mv "${POM}.out" "${POM}"
done

