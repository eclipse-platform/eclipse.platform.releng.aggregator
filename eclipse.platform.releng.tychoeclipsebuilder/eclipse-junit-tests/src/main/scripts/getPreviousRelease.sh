#!/usr/bin/env bash

# Convenience script.
# Utility to copy current and previous versions of SDK and platform binary to the expected location.
# some parts expected to be temporary, until 'installDir' and 'testDir' 
# correctly or redefined.


mkdir -p workarea/${buildId}/eclipse-testing
#cp /home/files/buildzips/oxygen/R/R-4.10-201812060815/eclipse-SDK-4.10-linux-gtk-x86_64.tar.gz ./workarea/${buildId}/eclipse-testing/
cp /home/files/buildzips/oxygen/R/R-4.10-201812060815/eclipse-platform-4.10-linux-gtk-x86_64.tar.gz ./workarea/${buildId}/eclipse-testing/platformLocation/

cp ../../eclipse-SDK-${buildId}-linux-gtk-x86_64.tar.gz workarea/${buildId}/eclipse-testing/

cp eclipse-junit-tests-${buildId}.zip workarea/${buildId}/eclipse-testing/

cp library.xml workarea/${buildId}/eclipse-testing/
cp JUNIT.XSL   workarea/${buildId}/eclipse-testing/
