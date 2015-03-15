#!/usr/bin/env bash

# swt fix, instead of Tycho revert
pushd $aggDir/eclipse.platform.swt
patch -p0 < ${SCRIPT_PATH}/patches/Bug-461427-Tons-of-compile-errors-in-test-build-in-SWT.patch
rc=$?
popd
exit $rc

