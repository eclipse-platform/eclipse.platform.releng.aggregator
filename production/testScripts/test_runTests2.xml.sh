#!/usr/bin/env bash


#   Utility to test the runTests2.xml file. It's intended to allow rapid confirmation
#   that the runTests2.xml file computes correct values, for various versions platforms
#   and various versions of java and ant, etc.

#   In particular, ant can be invoked with one version of java, but another version
#   of java used to actually run the tests.

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

function clean()
{

  echo -e "\n Cleaning previous results, as requested."
  # remember, do not want to rm "out.txt" since we are currently piping to it via 'tee'.
  rm -fr eclipse.platform.releng.aggregator/ eclipse.platform.releng.aggregator-master.zip eclipse.platform.releng.basebuilder-R38M6PlusRC3G/ getEBuilder.xml org.eclipse.releng.basebuilder/ runTests2.xml tempEBuilder/

  rm -fr production.properties propertiesAllFromRunTest2.properties sdk.tests streamSpecific-build.properties workarea

}

source localBuildProperties.shsource 2>/dev/null
#   Different versions of Ant are specified here in test script, just to confirm
#   nothing is specific to any recent version of ant. (Though, some of the machines
#   have ant 1.6 set as 'default'!)
#export ANT_HOME=/shared/common/apache-ant-1.7.1
export ANT_HOME=/shared/common/apache-ant-1.8.4/
#export ANT_HOME=/shared/common/apache-ant-1.9.2

#   JAVA_HOME is, at least, what runs the ant instance. If no 'jvm' option is specified,
#   it also becomes the instance that runs the tests.
#export JAVA_HOME=/shared/common/jdk1.5.0-latest
#export JAVA_HOME=/shared/common/jdk1.6.0-latest
#export JAVA_HOME=/shared/common/jdk1.7.0-latest
#export JAVA_HOME=/shared/common/jdk1.8.0_x64-latest

export JAVA_HOME=/shared/common/jdk1.7.0-latest
export WORKSPACE=${HOME}/tempworkarea/
export PATH=${JAVA_HOME}/bin:${ANT_HOME}/bin:/usr/local/bin:/usr/bin:/bin:${HOME}/bin

# This variable signals parts of the script that we are testing the test scripts,
# and should not actually start the tests.
export TESTING_TEST_XML=true
#export ANT_OPTS=-Xms1024m -Xmx1024m -Djava.io.tmpdir=${WORKSPACE}/tmp
export ANT_OPTS=-Djava.io.tmpdir=${WORKSPACE}/tmp

if [[ "$1" == "-c" ]]
then
  clean
fi

if [[ -z "${GIT_HOST}" ]]
then
  GIT_HOST=git.eclipse.org
fi

# print basic diagnostics and properties
ant -diagnostics
java -XshowSettings -version


#    There are a number of test-<something> methods in test xml which, by convention, mean
#    to simply test the test script itself. The test-all target runs all of those tests.

# Note: currently this file always comes from master, no matter what branch is being built/tested.
wget -O getEBuilder.xml --no-verbose   http://${GIT_HOST}/c/platform/eclipse.platform.releng.aggregator.git/plain/production/testScripts/hudsonBootstrap/getEBuilder.xml 2>&1

# Can only test the "downloadURL form" if there is a current, accurate build. During development, should use git/master version.


ANTFILE=getEBuilder.xml
buildId=N20140823-1500
eclipseStream=4.5.0
EBUILDER_HASH=d4ca36a125742e490be6b22caca84d7769030758

ant -f "${ANTFILE}" -DbuildId=$buildId -DeclipseStream=$eclipseStream -Dosgi.os=linux -Dosgi.ws=gtk -Dosgi.arch=x86_64 -DEBUILDER_HASH=${EBUILDER_HASH} -DdownloadURL=http://download.eclipse.org/eclipse/downloads/drops4/${buildId} -Dtest.target=performance -DskipDerby=true


