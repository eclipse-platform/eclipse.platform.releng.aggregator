<?php 

// This is purely an example, to use as reference while in workspace. 
// The actual file, named 'buildproperties.php,
// is produced during the build, and so will not only be different values for each build,
// but will likely have variables
// some and go as the build changes over the months and years. So, for 
// accurate reference, be sure to get
// a recent copy. This was copied circa June 25, 2014.

// properties written for 4.4 
$PATH = "/shared/common/jdk1.7.0-latest/bin:/shared/common/apache-maven-3.1.1/bin:/shared/common/apache-ant-1.9.2/bin:/usr/local/bin:/usr/bin:/bin:/opt/buildhomes/e4Build/bin";
$INITIAL_ENV_FILE = "/shared/eclipse/builds/4I/production/build_eclipse_org.shsource";
$BUILD_ROOT = "/shared/eclipse/builds/4I";
$BRANCH = "master";
$STREAM = "4.4.0";
$BUILD_TYPE = "R";
$TIMESTAMP = "20140606-1215";
$TMP_DIR = "/shared/eclipse/builds/4I/tmp";
$JAVA_HOME = "/shared/common/jdk1.7.0-latest";
$MAVEN_OPTS = "-Xmx2560m -XX:MaxPermSize=256M -Djava.io.tmpdir=/shared/eclipse/builds/4I/tmp -Dtycho.localArtifacts=ignore -Declipse.p2.mirrors=false";
$MAVEN_PATH = "/shared/common/apache-maven-3.1.1/bin";
$AGGREGATOR_REPO = "file:///gitroot/platform/eclipse.platform.releng.aggregator.git";
$BASEBUILDER_TAG = "R38M6PlusRC3G";
$B_GIT_EMAIL = "e4Build@eclipse.org";
$B_GIT_NAME = "E4 Build";
$COMMITTER_ID = "e4Build";
$MVN_DEBUG = "true";
$MVN_QUIET = "false";
$SIGNING = "true";
$REPO_AND_ACCESS = "file:///gitroot";
$MAVEN_BREE = "-Pbree-libs";
$GIT_PUSH = "git push";
$LOCAL_REPO = "/shared/eclipse/builds/4I/localMavenRepo";
$SCRIPT_PATH = "/shared/eclipse/builds/4I/production";
$STREAMS_PATH = "/shared/eclipse/builds/4I/gitCache/eclipse.platform.releng.aggregator/streams";
$BUILD_KIND = "CBI";
$CBI_JDT_REPO_URL = "";
$CBI_JDT_REPO_URL_ARG = "";
$CBI_JDT_VERSION = "";
$CBI_JDT_VERSION_ARG = "";
$PATCH_BUILD = "";
$ALT_POM_FILE = "";
$JAVA_DOC_TOOL = "-Declipse.javadoc=/shared/common/jdk1.8.0_x64-latest/bin/javadoc";
$BUILD_ENV_FILE = "/shared/eclipse/builds/4I/siteDir/eclipse/downloads/drops4/R-4.4-201406061215/buildproperties.shsource";
$BUILD_ENV_FILE_PHP = "/shared/eclipse/builds/4I/siteDir/eclipse/downloads/drops4/R-4.4-201406061215/buildproperties.php";
$BUILD_ENV_FILE_PROP = "/shared/eclipse/builds/4I/siteDir/eclipse/downloads/drops4/R-4.4-201406061215/buildproperties.properties";
$BUILD_ID = "4.4";
$BUILD_DIR_SEG = "R-4.4-201406061215";
$EQ_BUILD_DIR_SEG = "R-Luna-201406061215";
$BUILD_PRETTY_DATE = "Fri Jun  6 12:15:11 EDT 2014";
$BUILD_TYPE_NAME = "Release";
$TRACE_OUTPUT = "/shared/eclipse/builds/4I/siteDir/eclipse/downloads/drops4/R-4.4-201406061215/buildlogs/trace_output.txt";
$comparatorRepository = "NOT_CURRENTLY_USED";
$logsDirectory = "/shared/eclipse/builds/4I/siteDir/eclipse/downloads/drops4/R-4.4-201406061215/buildlogs";
$BUILD_TIME_PATCHES = "false";
$BUILD_HOME = "/shared/eclipse/builds";
$EBUILDER_HASH = "163582accf19622102b335521e12474a74499450";
$API_PREV_REF_LABEL = "4.3.2";
$API_FREEZE_REF_LABEL = "4.4M6";
// finished properties for 4.4 
?>
