#!/usr/bin/env bash

# Utility to copy current and previous rversions of SDK and platform binary to the expected location.
# some parts expected to be temporary, until 'installDir' and 'testDir' 
# correctly or redefined.


mkdir -p workarea/${buildId}/eclipse-testing
#cp /home/files/buildzips/mars/SR0/R-4.5-201506032000/eclipse-SDK-4.5-linux-gtk-x86_64.tar.gz ./workarea/${buildId}/eclipse-testing/
cp /home/files/buildzips/mars/SR0/R-4.5.2-201602121500/eclipse-platform-4.5.1-linux-gtk-x86_64.tar.gz ./workarea/${buildId}/eclipse-testing/platformLocation/

cp ../../eclipse-SDK-${buildId}-linux-gtk-x86_64.tar.gz workarea/${buildId}/eclipse-testing/

cp eclipse-junit-tests-${buildId}.zip workarea/${buildId}/eclipse-testing/

cp library.xml workarea/${buildId}/eclipse-testing/
cp JUNIT.XSL   workarea/${buildId}/eclipse-testing/
