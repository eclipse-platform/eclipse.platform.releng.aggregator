<?php 

// This is purely an example, to use as reference while in workspace. The actual file, named 'buildproperties.php,
// is produced during the build, and so will not only be different values for each build, 
// but will likely have variables 
// some and go as the build changes over the months and years. So, for accurate reference, be sure to get 
// a recent copy. This was copied circa March 14, 2013.

// properties written for I20130314-1330 
$PATH = "/shared/common/jdk1.7.0_11/bin:/shared/common/apache-maven-3.0.4/bin:/shared/common/apache-ant-1.8.4/bin:/usr/local/bin:/usr/bin:/bin:/opt/buildhomes/e4Build/bin";
$INITIAL_ENV_FILE = "/shared/eclipse/builds/4I/production/build_eclipse_org.shsource";
$BUILD_ROOT = "/shared/eclipse/builds/4I";
$BRANCH = "master";
$STREAM = "4.3.0";
$BUILD_TYPE = "I";
$TIMESTAMP = "20130314-1330";
$TMP_DIR = "/shared/eclipse/builds/4I/tmp";
$JAVA_HOME = "/shared/common/jdk1.7.0_11";
$MAVEN_OPTS = "-Xmx2048m -XX:MaxPermSize=256M -Djava.io.tmpdir=/shared/eclipse/builds/4I/tmp -Dtycho.localArtifacts=ignore";
$MAVEN_PATH = "/shared/common/apache-maven-3.0.4/bin";
$AGGREGATOR_REPO = "git://git.eclipse.org/gitroot/platform/eclipse.platform.releng.aggregator.git";
$BASEBUILDER_TAG = "R38M6PlusRC3G";
$B_GIT_EMAIL = "e4Build@eclipse.org";
$B_GIT_NAME = "E4 Build";
$COMMITTER_ID = "e4Build";
$MVN_DEBUG = "false";
$MVN_QUIET = "false";
$SIGNING = "true";
$UPDATE_BRANDING = "true";
$FORCE_LOCAL_REPO = "false";
$MAVEN_BREE = "-Pbree-libs";
$GIT_PUSH = "git push";
$LOCAL_REPO = "/shared/eclipse/builds/4I/localMavenRepo";
$INITIAL_ENV_FILE = "/shared/eclipse/builds/4I/production/build_eclipse_org.shsource";
$SCRIPT_PATH = "/shared/eclipse/builds/4I/production";
$STREAMS_PATH = "/shared/eclipse/builds/4I/master/gitCache/eclipse.platform.releng.aggregator/streams";
$BUILD_ENV_FILE = "/shared/eclipse/builds/4I/siteDir/eclipse/downloads/drops4/I20130314-1330/buildproperties.shsource";
$BUILD_ENV_FILE_PHP = "/shared/eclipse/builds/4I/siteDir/eclipse/downloads/drops4/I20130314-1330/buildproperties.php";
$BUILD_ENV_FILE_PROP = "/shared/eclipse/builds/4I/siteDir/eclipse/downloads/drops4/I20130314-1330/buildproperties.properties";
$BUILD_ID = "I20130314-1330";
$BUILD_PRETTY_DATE = "Thu Mar 14 13:30:06 EDT 2013";
$BUILD_TYPE_NAME = "Integration";
$EBUILDER_HASH = "7d7fca9ab1745a2aec0f6be3b3c2ff95c9456195";
// finished properties for I20130314-1330 
?>
