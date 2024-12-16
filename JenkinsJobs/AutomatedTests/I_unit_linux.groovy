def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('JenkinsJobs/JobDSL.json'))
def STREAMS = config.Streams
def JAVA_VERSIONS = ['21', '23']

def BUILD_CONFIGURATIONS = [
  [javaVersion: 21, javaHome: "tool(type:'jdk', name:'openjdk-jdk21-latest')" ],
  [javaVersion: 23, javaHome: "installJDK('23', 'linux', 'x86_64', 'ea')" ]
]

for (STREAM in STREAMS){
  def MAJOR = STREAM.split('\\.')[0]
  def MINOR = STREAM.split('\\.')[1]
  for (BUILD_CONFIG in BUILD_CONFIGURATIONS){

  pipelineJob('AutomatedTests/ep' + MAJOR + MINOR + 'I-unit-linux-x86_64-java' + BUILD_CONFIG.javaVersion){
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
    label 'ubuntu-2404'
  }

  stages {
      stage('Run tests'){
          environment {
              // Declaring a jdk and ant the usual way in the 'tools' section, because of unknown reasons, breaks the usage of system commands like xvnc, pkill and sh
              JAVA_HOME = ''' + BUILD_CONFIG.javaHome + '''
              ANT_HOME = tool(type:'ant', name:'apache-ant-latest')
              PATH = "${JAVA_HOME}/bin:${ANT_HOME}/bin:${PATH}"
              ANT_OPTS = "-Djava.io.tmpdir=${WORKSPACE}/tmp"
          }
          steps {
              xvnc(useXauthority: true) {
                      sh \'\'\'#!/bin/bash -x
                        
                        buildId=$(echo $buildId|tr -d ' ')
                        export LANG=en_US.UTF-8
                        cat /etc/*release
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
                          -Dosgi.os=linux -Dosgi.ws=gtk -Dosgi.arch=x86_64 \\
                          -DtestSuite=all
                        # For smaller test-suites see: https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/blob/be721e33c916b03c342e7b6f334220c6124946f8/production/testScripts/configuration/sdk.tests/testScripts/test.xml#L1893-L1903
                      \'\'\'
              }
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

def installJDK(String version, String os, String arch, String releaseType='ga') {
	// Translate os/arch names that are different in the Adoptium API
	if (arch == 'x86_64') {
		arch == 'x64'
	}
	dir ("${WORKSPACE}/java") {
		sh "curl -L https://api.adoptium.net/v3/binary/latest/${version}/${releaseType}/${os}/${arch}/jdk/hotspot/normal/eclipse | tar -xzf -"
		return "${pwd()}/" + sh(script: 'ls', returnStdout: true).strip()
	}
}
''')
      }
    }
  }
}
}
