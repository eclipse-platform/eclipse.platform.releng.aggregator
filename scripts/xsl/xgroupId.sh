#!/bin/bash

ECLIPSE=/opt/local/e4-self/R421/eclipse/eclipse

if [ $# -gt 0 ]; then
	ECLIPSE="$1" ; shift
fi

find * -name pom.xml -print0 | xargs -0 grep eclipse-plugin | cut -f1 -d: | sort -u >/tmp/t1_$$.txt

for POM in $( cat /tmp/t1_$$.txt ); do
xsltproc -o "${POM}.out" fix-pom.xsl "${POM}"
mv "${POM}.out" "${POM}"
done

find * -name pom.xml -print0 | xargs -0 grep eclipse-feature | cut -f1 -d: | sort -u >/tmp/t1_$$.txt

for POM in $( cat /tmp/t1_$$.txt ); do
xsltproc -o "${POM}.out" fix-feature-pom.xsl "${POM}"
mv "${POM}.out" "${POM}"
done


find * -name pom.xml -print0 | xargs -0 grep eclipse-test-plugin | cut -f1 -d: | sort -u >/tmp/t1_$$.txt

for POM in $( cat /tmp/t1_$$.txt ); do
xsltproc -o "${POM}.out" fix-pom.xsl "${POM}"
mv "${POM}.out" "${POM}"
done


