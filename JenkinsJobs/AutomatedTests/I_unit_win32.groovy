def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('JenkinsJobs/JobDSL.json'))
def STREAMS = config.Streams

for (STREAM in STREAMS){
  def MAJOR = STREAM.split('\\.')[0]
  def MINOR = STREAM.split('\\.')[1]

  pipelineJob('AutomatedTests/ep' + MAJOR + MINOR + 'I-unit-win32-x86_64-java17'){
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
    timeout(time: 901, unit: 'MINUTES')
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
              ANT_OPTS = "-Djava.io.tmpdir=${WORKSPACE}\\\\tmp -Djava.security.manager=allow"
          }
          steps {
              cleanWs() // workspace not cleaned by default
              bat \'\'\'
@REM May want to try and restrict path, as we do on cron jobs, so we
@REM have more consistent conditions.
@REM export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:~/bin

@REM tmp must already exist, for Java to make use of it, in subsequent steps
@REM no -p (or /p) needed on Windows. It creates 
mkdir tmp

@REM Note: currently this file always comes from master, no matter what branch is being built/tested.
curl -o getEBuilder.xml https://download.eclipse.org/eclipse/relengScripts/production/testScripts/hudsonBootstrap/getEBuilder.xml
curl -o buildProperties.properties https://download.eclipse.org/eclipse/downloads/drops4/%buildId%/buildproperties.properties
echo off
For /F "tokens=1* delims==" %%A IN (buildProperties.properties) DO (
 IF "%%A"=="STREAM " set STREAM=%%B
 IF "%%A"=="EBUILDER_HASH " set EBUILDER_HASH=%%B
) 
echo on

set JAVA_HOME
set ANT_HOME
set PATH

env 1>envVars.txt 2>&1
ant -diagnostics 1>antDiagnostics.txt 2>&1
java -XshowSettings -version 1>javaSettings.txt 2>&1

ant -f getEBuilder.xml -DbuildId=%buildId%  -DeclipseStream=%STREAM% -DEBUILDER_HASH=%EBUILDER_HASH% ^
  -DdownloadURL="https://download.eclipse.org/eclipse/downloads/drops4/%buildId%" ^
  -Dargs=all -Dosgi.os=win32 -Dosgi.ws=win32 -Dosgi.arch=x86_64 ^
  -DtestSuite=all
@REM For smaller test-suites see: https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/blob/be721e33c916b03c342e7b6f334220c6124946f8/production/testScripts/configuration/sdk.tests/testScripts/test.xml#L1893-L1903
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
