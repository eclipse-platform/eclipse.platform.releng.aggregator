#!/usr/bin/env bash

source localBuildProperties.shsource  2>/dev/null

# No longer works as expected.
# TODO: fix.

#   Utility to test the test.xml file. It's intended to allow rapid confirmation
#   that the test.xml file ends up "computing" correct values, for various versions
#   of java and ant.

#   In particular, ant cant be invoked with one version of java, but another version
#   of java used to actually run the tests.

#   Currently the file 'testing.properties' must be copied as peer of test.xml ... simply for
#   its absence to not cause an error.

#   Typically, to simulate production builds (tests) on Hudson, a 'vm.properties' tests will
#   also be present, where alternative, specific VMs could be specified. (This allows, for example,
#   the same Hudson setup to test Java 6, 7, or 8 (no matter what VM the ant task was
#   started with.

#   In addition to the console output. which should be captured, with something like
#      ./testTextXMLScript.sh | tee testoutput.txt
#   some data or set-up files will be created as they normally would if running the real tests.
#   The can be checked to be sure they are correct, and in correct (relative) locations.

# unset some common variables which we don't want (or, set ourselves)
unset JAVA_HOME
unset JAVA_ROOT
unset JAVA_JRE
unset CLASSPATH
unset JAVA_BINDIR
unset JRE_HOME


#   Different versions of Ant are specified here in test script, just to confirm
#   nothing is specific to any recent version of ant. (Though, some of the machines
#   have ant 1.6 set as 'default'!)
#export ANT_HOME=/shared/common/apache-ant-1.7.1
#export ANT_HOME=/shared/common/apache-ant-1.8.4/
export ANT_HOME=/shared/common/apache-ant-1.9.6

#   JAVA_HOME is, at least, what runs the ant instance. If no 'jvm' option is specified,
#   it also becomes the instance that runs the tests.
JAVA_11_HOME=/opt/public/common/java/openjdk/jdk-11_x64-latest
export JAVA_HOME=${JAVA_HOME:-${JAVA_11_HOME}}
export PATH=${JAVA_HOME}/bin:${ANT_HOME}/bin:/usr/local/bin:/usr/bin:/bin:${HOME}/bin

export TESTING_TEST_XML=true

#    There are a number of test-<something> methods in test xml which, by convention, mean
#    to simply test the test script itself. The test-all target runs all of those tests.
#ant -f test.xml test-all

ant -f test.xml test-all  -propertyfile vm.properties -DbuildId=I20160430-0237 -DeclipseStream=4.7.0 -Dosgi.os=linux

