def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('JenkinsJobs/JobDSL.json'))
def STREAMS = config.Streams

def BUILD_CONFIGURATIONS = [ 
  [name: 'macM1', arch: 'aarch64', agentLabel: 'nc1ht-macos11-arm64', javaHome: '/usr/local/openjdk-17/Contents/Home' ],
  [name: 'mac64', arch: 'x86_64',  agentLabel: 'nc1ht-macos11-arm64', javaHome: '/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home' ]
]

for (STREAM in STREAMS){
  def MAJOR = STREAM.split('\\.')[0]
  def MINOR = STREAM.split('\\.')[1]
  for (BUILD_CONFIG in BUILD_CONFIGURATIONS){

	job('YPBuilds/ep' + MAJOR + MINOR + 'Y-unit-' + BUILD_CONFIG.name + '-java17'){
	  description('Run Eclipse SDK Tests for ' + BUILD_CONFIG.arch + ' Mac (and ' + BUILD_CONFIG.arch + ' VM and Eclipse)')
	
	  logRotator {
	    numToKeep(5)
	  }
	
	  parameters {
	    stringParam('buildId', null, 'Build Id to test (such as I20120717-0800, N20120716-0800). ')
	    stringParam('testSuite', 'all', null)
	  }
	
	
	  label('nc1ht-macos11-arm64')
	
	  jdk('openjdk-jdk11-latest')
	
	  wrappers { //adds pre/post actions
	    timestamps()
	    timeout {
	      absolute(600)
	    }
	  }
	  
	  steps {
	    shell('''
#!/usr/bin/env bash

if [[ -z "${WORKSPACE}" ]]
then
  echo -e "\\n\\tERROR: WORKSPACE variable was not defined"
  exit 1
else
  if [[ ! -d "${WORKSPACE}" ]]
  then
    echo -e "\\n\\tERROR: WORKSPACE was defined, but did not exist?"
    echo -e "\\t\\tIt was defined as ${WORKSPACE}"
    exit 1
  else
    echo -e "\\n\\tINFO: WORKSPACE was defined as ${WORKSPACE}"
    echo -e "\\t\\tWill delete contents, for clean run"
    MaxLoops=15
    SleepTime=60
    currentLoop=0
    nFilesOrDirs=$( find "${WORKSPACE}" -mindepth 1 -maxdepth 1 | wc -l )
    while [[ ${nFilesOrDirs} -gt 0 ]]
    do
      currentLoop=$(( ${currentLoop} + 1 ))
      if [[ ${currentLoop} -gt ${MaxLoops} ]]
      then
        echo -e "\\n\\tERROR: Number of re-try loops, ${currentLoop}, exceeded maximum, ${MaxLoops}. "
        echo -e " \\t\\tPossibly due to files still being used by another process?"
        exit 0
        break
      fi
      echo -e "\\tcurrentLoop: ${currentLoop}   nFilesOrDirs:  ${nFilesOrDirs}"
      find "${WORKSPACE}" -mindepth 1 -maxdepth 1 -execdir rm -fr '{}' \\;
      nFilesOrDirs=$( find "${WORKSPACE}" -mindepth 1 -maxdepth 1 | wc -l )
      if [[ ${nFilesOrDirs} -gt 0 ]]
      then
        sleep ${SleepTime}
      fi
    done
  fi
fi
echo -e "\\t... ending cleaning"

exit 0
    ''')
    shell('''
#!/bin/bash -x

RAW_DATE_START="$(date +%s )"

echo -e "\\n\\tRAW Date Start: ${RAW_DATE_START} \\n"

echo -e "\\n\\t whoami:  $( whoami )\\n"
echo -e "\\n\\t uname -a: $(uname -a)\\n"

# unset commonly defined system variables, which we either do not need, or want to set ourselves.
# (this is to improve consistency running on one machine versus another)
echo -e "Unsetting variables: JAVA_BINDIR JAVA_HOME JAVA_ROOT JDK_HOME JRE_HOME CLASSPATH ANT_HOME\\n"
unset -v JAVA_BINDIR JAVA_HOME JAVA_ROOT JDK_HOME JRE_HOME CLASSPATH ANT_HOME

# 0002 is often the default for shell users, but it is not when ran from
# a cron job, so we set it explicitly, to be sure of value, so releng group has write access to anything
# we create on shared area.
oldumask=$(umask)
umask 0002
echo "umask explicitly set to 0002, old value was $oldumask"

# we want java.io.tmpdir to be in $WORKSPACE, but must already exist, for Java to use it.
mkdir -p tmp

curl -o getEBuilder.xml https://download.eclipse.org/eclipse/relengScripts/production/testScripts/hudsonBootstrap/getEBuilder.xml 2>&1
cat getEBuilder.xml
curl -o buildProperties.sh https://download.eclipse.org/eclipse/downloads/drops4/$buildId/buildproperties.shsource
cat getEBuilder.xml
source buildProperties.sh

export JAVA_HOME=''' + BUILD_CONFIG.javaHome + '''
export ANT_HOME=/opt/homebrew/Cellar/ant/1.10.11/libexec
export PATH=${JAVA_HOME}/bin:${ANT_HOME}/bin:${PATH}

echo JAVA_HOME: $JAVA_HOME
echo ANT_HOME: $ANT_HOME
echo PATH: $PATH

env  1>envVars.txt 2>&1
ant -diagnostics  1>antDiagnostics.txt 2>&1
java -XshowSettings -version  1>javaSettings.txt 2>&1

ant -f getEBuilder.xml -Djava.io.tmpdir=${WORKSPACE}/tmp -DbuildId=$buildId \\
  -DeclipseStream=$STREAM -DEBUILDER_HASH=${EBUILDER_HASH} \\
  -DdownloadURL=https://download.eclipse.org/eclipse/downloads/drops4/${buildId} \\
  -Dosgi.os=macosx -Dosgi.ws=cocoa -Dosgi.arch=''' + BUILD_CONFIG.arch + ''' \\
  -DtestSuite=${testSuite}

RAW_DATE_END="$(date +%s )"

echo -e "\\n\\tRAW Date End: ${RAW_DATE_END} \\n"

TOTAL_TIME=$((${RAW_DATE_END} - ${RAW_DATE_START}))

echo -e "\\n\\tTotal elapsed time: ${TOTAL_TIME} \\n"
    ''')
  }

  publishers {
    archiveJunit('**/eclipse-testing/results/xml/*.xml') {
      retainLongStdout()
      healthScaleFactor((1.0).doubleValue())
    }
    archiveArtifacts {
      pattern('**/eclipse-testing/results/**, **/eclipse-testing/directorLogs/**, *.properties, *.txt')
    }
    extendedEmail {
      recipientList("sravankumarl@in.ibm.com")
    }
    downstreamParameterized {
      trigger('YPBuilds/ep-collectYbuildResults') {
        condition('UNSTABLE_OR_BETTER')
        parameters {
          predefinedProp('triggeringJob', '$JOB_BASE_NAME')
          predefinedProp('buildURL', '$BUILD_URL')
          predefinedProp('buildID', '$buildId')
        }
      }
    }
  }
 }
}
}
