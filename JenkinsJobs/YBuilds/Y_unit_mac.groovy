def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('JenkinsJobs/JobDSL.json'))
def STREAMS = config.Streams

def BUILD_CONFIGURATIONS = [
  [arch: 'aarch64', agentLabel: 'nc1ht-macos11-arm64', javaHome: '/usr/local/openjdk-17/Contents/Home' ],
  [arch: 'x86_64',  agentLabel: 'nc1ht-macos11-arm64', javaHome: '/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home' ]
]

for (STREAM in STREAMS){
  def MAJOR = STREAM.split('\\.')[0]
  def MINOR = STREAM.split('\\.')[1]
  for (BUILD_CONFIG in BUILD_CONFIGURATIONS){

    pipelineJob('YPBuilds/ep' + MAJOR + MINOR + 'Y-unit-macosx-' + BUILD_CONFIG.arch + '-java17'){
	  description('Run Eclipse SDK Tests for ' + BUILD_CONFIG.arch + ' Mac (and ' + BUILD_CONFIG.arch + ' VM and Eclipse)')
	  parameters {
	    stringParam('buildId', null, 'Build Id to test (such as I20240611-1800, N20120716-0800).')
	  }
      definition {
        cps {
          sandbox()
          script('''
pipeline {
  options {
    timeout(time: 600, unit: 'MINUTES')
    timestamps()
    buildDiscarder(logRotator(numToKeepStr:'5'))
  }
  agent {
    label \'''' + BUILD_CONFIG.agentLabel + ''''
  }
  stages {
      stage('Run tests'){
          environment {
              // Declaring a jdk and ant the usual way in the 'tools' section, because of unknown reasons, breaks the usage of system commands like xvnc, pkill and sh
              JAVA_HOME = \'''' + BUILD_CONFIG.javaHome + ''''
              ANT_HOME = tool(type:'ant', name:'apache-ant-latest')
              PATH = "${JAVA_HOME}/bin:${ANT_HOME}/bin:${PATH}"
              ANT_OPTS = "-Djava.io.tmpdir=${WORKSPACE}/tmp -Djava.security.manager=allow"
          }
          steps {
              cleanWs() // workspace not cleaned by default
              sh \'\'\'#!/bin/bash -x
echo "whoami:  $(whoami)"
echo "uname -a: $(uname -a)"

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

echo JAVA_HOME: $JAVA_HOME
echo ANT_HOME: $ANT_HOME
echo PATH: $PATH

env  1>envVars.txt 2>&1
ant -diagnostics  1>antDiagnostics.txt 2>&1
java -XshowSettings -version  1>javaSettings.txt 2>&1

ant -f getEBuilder.xml -DbuildId=${buildId} \\
  -DeclipseStream=$STREAM -DEBUILDER_HASH=${EBUILDER_HASH} \\
  -DdownloadURL=https://download.eclipse.org/eclipse/downloads/drops4/${buildId} \\
  -Dosgi.os=macosx -Dosgi.ws=cocoa -Dosgi.arch=''' + BUILD_CONFIG.arch + ''' \\
  -DtestSuite=all
\'\'\'
              archiveArtifacts '**/eclipse-testing/results/**, **/eclipse-testing/directorLogs/**, *.properties, *.txt'
              junit keepLongStdio: true, testResults: '**/eclipse-testing/results/xml/*.xml'
              build job: 'YPBuilds/ep-collectYbuildResults', wait: false, parameters: [
                string(name: 'triggeringJob', value: "${JOB_BASE_NAME}"),
                string(name: 'buildURL', value: "${BUILD_URL}"),
                string(name: 'buildID', value: "${params.buildId}")
              ]
          }
      }
  }
}
''')
        }
      }
    }
  }
}
