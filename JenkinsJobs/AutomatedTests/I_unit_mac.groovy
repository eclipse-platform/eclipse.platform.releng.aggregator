def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('JenkinsJobs/JobDSL.json'))
def STREAMS = config.Streams

def ARCHS = ['aarch64', 'x86_64']
def ARCHS_AGENT_LABEL = ['aarch64': 'nc1ht-macos11-arm64', 'x86_64': 'nc1ht-macos11-arm64']
def ARCHS_JAVA_HOME = ['aarch64': '/Library/Java/JavaVirtualMachines/jdk-21.0.5+11-arm64/Contents/Home', 'x86_64': '/Library/Java/JavaVirtualMachines/jdk-21.0.5+11/Contents/Home']

for (STREAM in STREAMS){
for (ARCH in ARCHS){
  def MAJOR = STREAM.split('\\.')[0]
  def MINOR = STREAM.split('\\.')[1]

  pipelineJob('AutomatedTests/ep' + MAJOR + MINOR + 'I-unit-macosx-' + ARCH + '-java21'){
    description('Run Eclipse SDK Tests for the platform implied by this job\'s name')
    parameters { // Define parameters in job configuration to make them available from the very first build onwards
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
    buildDiscarder(logRotator(numToKeepStr:'15', artifactNumToKeepStr:'5'))
  }
  agent {
    label \'''' + ARCHS_AGENT_LABEL[ARCH] + ''''
  }

  stages {
      stage('Run tests'){
          environment {
              // Declaring a jdk and ant the usual way in the 'tools' section, because of unknown reasons, breaks the usage of system commands like xvnc, pkill and sh
              JAVA_HOME = \'''' + ARCHS_JAVA_HOME[ARCH] + ''''
              ANT_HOME = tool(type:'ant', name:'apache-ant-latest')
              PATH = "${JAVA_HOME}/bin:${ANT_HOME}/bin:${PATH}"
              ANT_OPTS = "-Djava.io.tmpdir=${WORKSPACE}/tmp"
          }
          steps {
              cleanWs() // workspace not cleaned by default
              sh \'\'\'#!/bin/bash -x
echo " whoami:  $(whoami)"
echo " uname -a: $(uname -a)"

# 0002 is often the default for shell users, but it is not when ran from
# a cron job, so we set it explicitly, to be sure of value, so releng group has write access to anything
# we create on shared area.
oldumask=$(umask)
umask 0002
echo "umask explicitly set to 0002, old value was $oldumask"

# we want java.io.tmpdir to be in $WORKSPACE, but must already exist, for Java to use it.
mkdir -p tmp

curl -o getEBuilder.xml https://download.eclipse.org/eclipse/relengScripts/production/testScripts/hudsonBootstrap/getEBuilder.xml
curl -o buildproperties.shsource https://download.eclipse.org/eclipse/downloads/drops4/${buildId}/buildproperties.shsource
source buildproperties.shsource

echo JAVA_HOME: $JAVA_HOME
echo ANT_HOME: $ANT_HOME
echo PATH: $PATH

env 1>envVars.txt 2>&1
ant -diagnostics 1>antDiagnostics.txt 2>&1
java -XshowSettings -version 1>javaSettings.txt 2>&1

ant -f getEBuilder.xml -DbuildId=${buildId} -DeclipseStream=${STREAM} -DEBUILDER_HASH=${EBUILDER_HASH} \\
  -DdownloadURL=https://download.eclipse.org/eclipse/downloads/drops4/${buildId} \\
  -Dosgi.os=macosx -Dosgi.ws=cocoa -Dosgi.arch=''' + ARCH + ''' \\
  -DtestSuite=all
# For smaller test-suites see: https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/blob/be721e33c916b03c342e7b6f334220c6124946f8/production/testScripts/configuration/sdk.tests/testScripts/test.xml#L1893-L1903
              \'\'\'
              archiveArtifacts '**/eclipse-testing/results/**, **/eclipse-testing/directorLogs/**, *.properties, *.txt'
              junit keepLongStdio: true, testResults: '**/eclipse-testing/results/xml/*.xml'
              build job: 'Releng/ep-collectResults', wait: false, parameters: [
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
