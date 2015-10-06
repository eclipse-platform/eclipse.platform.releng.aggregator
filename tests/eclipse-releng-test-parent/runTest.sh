#!/usr/bin/env bash

# need to execute this from "git tree", so, if needed, first run
# git clone git://git.eclipse.org/gitroot/platform/eclipse.platform.releng.git
# then change to appropriate directory:
# cd eclipse.platform.releng/tests/eclipse-releng-test-parent/

mvn clean verify  -X -e -Pbree-libs --fail-fast -V \
 -DskipTests=true -Dmaven.repo.local=/shared/eclipse/builds/4N/localMavenRepo \
 -Dtycho.debug.artifactcomparator -DcontinueOnFail=true -Djgit.dirtyWorkingTree=error \
 -DbuildTimestamp=20151005-0945 -DbuildType=N -DbuildId=N20151005-0945 \
 -Declipse-p2-repo.url=NOT_FOR_PRODUCTION_USE -DforceContextQualifier=N20151005-0945 \
 -Declipse.javadoc=/shared/common/jdk1.8.0_x64-latest/bin/javadoc

