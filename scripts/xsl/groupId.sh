#!/bin/bash

ECLIPSE=/opt/local/e4-self/R421/eclipse/eclipse

if [ $# -gt 0 ]; then
	ECLIPSE="$1" ; shift
fi

find * -name pom.xml -print0 | xargs -0 grep eclipse-plugin | cut -f1 -d: | sort -u >/tmp/t1_$$.txt

for POM in $( cat /tmp/t1_$$.txt ); do
$ECLIPSE -noSplash \
-application org.eclipse.ant.core.antRunner -v \
-buildfile run-xsl.xml  \
-Dfile.sheet="fix-pom.xsl" \
-Dfile.in="$(pwd)/$POM" \
-Dfile.out="$(pwd)/${POM}.out"
xmllint --format "${POM}.out" >"${POM}"
rm "${POM}.out"
done

find * -name pom.xml -print0 | xargs -0 grep eclipse-feature | cut -f1 -d: | sort -u >/tmp/t1_$$.txt

for POM in $( cat /tmp/t1_$$.txt ); do
$ECLIPSE -noSplash \
-application org.eclipse.ant.core.antRunner -v \
-buildfile run-xsl.xml  \
-Dfile.sheet="fix-feature-pom.xsl" \
-Dfile.in="$(pwd)/$POM" \
-Dfile.out="$(pwd)/${POM}.out"
xmllint --format "${POM}.out" >"${POM}"
rm "${POM}.out"
done


