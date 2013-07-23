#!/bin/bash
#


LOG=$(pwd)/log_$( date +%Y%m%d%H%M%S ).txt

exec >>$LOG 2>&1

LREPO=$(pwd)/../localMavenRepo
export JAVA_HOME=/opt/local/jdk1.7.0-latest
TMP_DIR=$(pwd)/../tmp
mkdir -p $TMP_DIR
export MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=256M -Djava.io.tmpdir=${TMP_DIR}"
export MAVEN_PATH=/opt/local/apache-maven-3.1.0/bin
export PATH=$JAVA_HOME/bin:$MAVEN_PATH:$PATH

DIR=$( dirname $0 )
NEW_VER=4.4.0-SNAPSHOT
if [ $# -eq 1 ]; then
	NEW_VER="$1"
fi

find * -name pom.xml -print0 | xargs -0 grep packaging.pom..packaging | cut -f1 -d: | sort -u >/tmp/t1_$$.txt
find * -name pom.xml -print0 | xargs -0 grep packaging.eclipse-repository..packaging | cut -f1 -d: | sort -u >>/tmp/t1_$$.txt

for POM in $( cat /tmp/t1_$$.txt ); do
    # this doesn't update everything like I hoped
    # and fails for a small number of bundles.
    # it doesn't deal well with the children of the parent poms
    mvn -Pbuild-individual-bundles \
    versions:set \
    -DnewVersion=$NEW_VER \
    -DgenerateBackupPoms=false \
    -f $POM \
    -Dmaven.repo.local=$LREPO
done


