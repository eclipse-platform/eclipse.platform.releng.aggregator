def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('JenkinsJobs/JobDSL.json'))
def STREAMS = config.Streams

def BUILD_CONFIGURATIONS = [
  [javaVersion: 17, javaHome: '''installJDK('https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_linux-x64_bin.tar.gz')''' ],
  [javaVersion: 21, javaHome: '''installJDK('https://download.java.net/java/GA/jdk21/fd2272bbf8e04c3dbaee13770090416c/35/GPL/openjdk-21_linux-x64_bin.tar.gz')''' ],
  [javaVersion: 24, javaHome: '''installJDK('https://download.java.net/java/early_access/jdk24/18/GPL/openjdk-24-ea+18_linux-x64_bin.tar.gz')''' ]
]

for (STREAM in STREAMS){
  def MAJOR = STREAM.split('\\.')[0]
  def MINOR = STREAM.split('\\.')[1]
  for (BUILD_CONFIG in BUILD_CONFIGURATIONS){

    pipelineJob('YPBuilds/ep' + MAJOR + MINOR + 'Y-unit-linux-x86_64-java' + BUILD_CONFIG.javaVersion){
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
                                mkdir -p ${WORKSPACE}/tmp
                                
                                wget -O ${WORKSPACE}/getEBuilder.xml --no-verbose --no-check-certificate https://download.eclipse.org/eclipse/relengScripts/production/testScripts/hudsonBootstrap/getEBuilder.xml 2>&1
                                wget -O ${WORKSPACE}/buildproperties.shsource --no-check-certificate https://download.eclipse.org/eclipse/downloads/drops4/${buildId}/buildproperties.shsource
                                cat ${WORKSPACE}/buildproperties.shsource
                                source ${WORKSPACE}/buildproperties.shsource
                                
                                echo JAVA_HOME: $JAVA_HOME
                                echo ANT_HOME: $ANT_HOME
                                echo PATH: $PATH
                                
                                env 1>envVars.txt 2>&1
                                ant -diagnostics 1>antDiagnostics.txt 2>&1
                                java -XshowSettings -version 1>javaSettings.txt 2>&1
                                
                                ant -f getEBuilder.xml -DbuildId=${buildId} \\
                                  -DeclipseStream=$STREAM -DEBUILDER_HASH=${EBUILDER_HASH} \\
                                  -DdownloadURL=https://download.eclipse.org/eclipse/downloads/drops4/${buildId} \\
                                  -Dosgi.os=linux -Dosgi.ws=gtk -Dosgi.arch=x86_64 \\
                                  -DtestSuite=all
                              \'\'\'
              }
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

def installJDK(String downloadURL) {
	dir ("${WORKSPACE}/java") {
		sh "curl -L ${downloadURL} | tar -xzf -"
		return "${pwd()}/" + sh(script: 'ls', returnStdout: true).strip()
	}
}
''')
    }
  }
 }
}
}
