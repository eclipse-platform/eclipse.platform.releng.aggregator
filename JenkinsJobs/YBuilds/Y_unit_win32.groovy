def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('JenkinsJobs/JobDSL.json'))
def STREAMS = config.Streams

for (STREAM in STREAMS){
  def MAJOR = STREAM.split('\\.')[0]
  def MINOR = STREAM.split('\\.')[1]

    pipelineJob('YPBuilds/ep' + MAJOR + MINOR + 'Y-unit-win32-x86_64-java17'){
	  description('Run Eclipse SDK Windows Tests ')
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
    label 'qa6xd-win11'
  }
  stages {
      stage('Run tests'){
          environment {
              // Declaring a jdk and ant the usual way in the 'tools' section, because of unknown reasons, breaks the usage of system commands like xvnc, pkill and sh
              JAVA_HOME = 'C:\\\\Program Files\\\\Eclipse Adoptium\\\\jdk-17.0.11+9'
              ANT_HOME = tool(type:'ant', name:'apache-ant-latest')
              PATH = "${JAVA_HOME}\\\\bin;${ANT_HOME}\\\\bin;${PATH}"
              ANT_OPTS = "-Djava.io.tmpdir=${WORKSPACE}\\\\tmp"
          }
          steps {
              cleanWs() // workspace not cleaned by default
              bat \'\'\'
rem tmp must already exist, for Java to make use of it, in subsequent steps
rem no -p (or /p) needed on Windows. It creates 
mkdir tmp

rem Note: currently this file always comes from master, no matter what branch is being built/tested.
wget -O getEBuilder.xml --no-verbose https://download.eclipse.org/eclipse/relengScripts/production/testScripts/hudsonBootstrap/getEBuilder.xml 2>&1
set buildId
wget -O buildProperties.properties https://download.eclipse.org/eclipse/downloads/drops4/%buildId%/buildproperties.properties
echo off
For /F "tokens=1* delims==" %%A IN (buildProperties.properties) DO (
 IF "%%A"=="STREAM " set STREAM=%%B
 IF "%%A"=="EBUILDER_HASH " set EBUILDER_HASH=%%B
) 
echo on

set STREAM
set EBUILDER_HASH
set JAVA_HOME
set ANT_HOME
set PATH

env 1>envVars.txt 2>&1
cmd /c ant -diagnostics 1>antDiagnostics.txt 2>&1
java -XshowSettings -version 1>javaSettings.txt 2>&1

ant -f getEBuilder.xml -DbuildId=%buildId% ^
  -DeclipseStream=%STREAM% -DEBUILDER_HASH=%EBUILDER_HASH% ^
  -DdownloadURL="https://download.eclipse.org/eclipse/downloads/drops4/%buildId%" ^
  -Dosgi.os=win32 -Dosgi.ws=win32 -Dosgi.arch=x86_64 ^
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
