#!/usr/bin/env bash

mvn clean verify  -X -e -Pbree-libs --fail-fast -V \
 -DskipTests=true -Dmaven.repo.local=/shared/eclipse/builds/4N/localMavenRepo \
 -Dtycho.debug.artifactcomparator -DcontinueOnFail=true -Djgit.dirtyWorkingTree=error \
 -DbuildTimestamp=20151005-0945 -DbuildType=N -DbuildId=N20151005-0945 \
 -Declipse-p2-repo.url=NOT_FOR_PRODUCTION_USE -DforceContextQualifier=N20151005-0945 \
 -Declipse.javadoc=/shared/common/jdk1.8.0_x64-latest/bin/javadoc

