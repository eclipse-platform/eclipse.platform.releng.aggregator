#!/usr/bin/env bash

# swt fix, instead of Tycho revert
pushd $aggDir/eclipse.platform.swt
git reset --hard
git revert --no-edit 2015cd98ef5b2c9ff44d19f27b2e4161df8682ce
RC=$?
popd
if [[ $RC != 0 ]]
then
  echo "Error revert 1 in SWT Patch"
  exit $RC
fi

pushd $aggDir/eclipse.platform.swt.binaries
git reset --hard
git revert --no-edit 9c21286548a1eeb87aa8e958c2df8e8747f0167b
RC=$?
popd
if [[ $RC != 0 ]]
then
  echo "Error revert 2 in SWT Patch"
  exit $RC
fi

pushd $aggDir/eclipse.platform.swt
patch -p0 < ${SCRIPT_PATH}/patches/Bug-461427-Tons-of-compile-errors-in-test-build-in-SWT.patch
RC=$?
popd
if [[ $RC != 0 ]]
then
  echo "Error patch in SWT Patch"
  exit $RC
fi


exit 0
