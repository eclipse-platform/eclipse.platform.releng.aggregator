#!/usr/bin/env bash

# need to execute this from "git tree", so, if needed, first run
# git clone git://git.eclipse.org/gitroot/platform/eclipse.platform.releng.git
# then change to appropriate directory:
# cd eclipse.platform.releng/tests/eclipse-releng-test-parent/

# remember to get a pristine repo, will need to do something like the following:
# git clean -f -d -x
# git reset --hard HEAD

# By default, clean local repo. But, in some cases may want to 
# comment out, to not always remove it.
if [[ -e /shared/eclipse/tmp/localMavenRepo ]]
then
  rm -fr /shared/eclipse/tmp/localMavenRepo
fi

mvn clean verify  -X -e -Pbree-libs --fail-fast -V \
 -DskipTests=true \
 -Dmaven.repo.local=/shared/eclipse/tmp/localMavenRepo \
 -Dtycho.debug.artifactcomparator \
 -DcontinueOnFail=true \
 -Djgit.dirtyWorkingTree=error \
 -DbuildTimestamp=20151005-1111 \
 -DbuildType=N \
 -DbuildId=N20151005-1111 \
 -Declipse-p2-repo.url=NOT_FOR_PRODUCTION_USE \
 -DforceContextQualifier=N20151005-1111 \
 -Declipse.javadoc=/shared/common/jdk1.8.0_x64-latest/bin/javadoc

