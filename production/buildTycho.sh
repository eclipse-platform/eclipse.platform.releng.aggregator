#!/usr/bin/env bash

# Small, temp utility to patch and build Tycho

if [[ -z "${LOCAL_REPO}" ]]
then
  echo "LOCAL_REPO not defined as required"
  exit 1
fi
# Similar for SCRIPT_PATH, relevant here only if local, isolated 
# build, else, it is defined elsewhere. 
SCRIPT_PATH=${SCRIPT_PATH:-${PWD}}

TYCHO_MVN_ARGS="-Dmaven.repo.local=$LOCAL_REPO -Dtycho.localArtifacts=ignore"
echo -e "\n\tTYCHO_MVN_ARGS: ${TYCHO_MVN_ARGS}\n"

if [[ -d org.eclipse.tycho ]]
then
  echo "Removing existing directory: org.eclipse.tycho"
  rm -fr org.eclipse.tycho
fi
git clone git://git.eclipse.org/gitroot/tycho/org.eclipse.tycho.git --quiet
echo "Tycho Patch"
pushd org.eclipse.tycho
git am --ignore-space-change <${SCRIPT_PATH}/patches/0927-Pascal-s-commit-not-mine-for-bug-461872-.-created-th.patch
rc=$?
if [ $rc != 0 ]
then
  echo "Tycho Patch did not apply? git am return code: $rc"
  popd
  exit $rc
fi

mvn -X -e clean install ${TYCHO_MVN_ARGS}
rc=$?
popd
if [ $rc == 0 ]
then
  if [[ -d org.eclipse.tycho.extras ]]
  then
    echo "Removing existing directory: org.eclipse.tycho.extras"
    rm -fr org.eclipse.tycho.extras
  fi
  git clone git://git.eclipse.org/gitroot/tycho/org.eclipse.tycho.extras.git --quiet
  pushd org.eclipse.tycho.extras
  mvn -X -e clean install ${TYCHO_MVN_ARGS}
  rc=$?
  if [ $rc != 0 ]
  then
    echo -e "\n\t[ERROR] Tycho Extras build failed. mvn returned $rc\n"
    exit $rc
  fi
else
  echo -e "\n\t[ERROR] Tycho Build failed. mvn returned $rc\n"
  exit $rc
fi
popd
