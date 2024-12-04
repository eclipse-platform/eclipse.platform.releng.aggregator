def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('JenkinsJobs/JobDSL.json'))
def STREAMS = config.Streams

for (STREAM in STREAMS){
  def MAJOR = STREAM.split('\\.')[0]
  def MINOR = STREAM.split('\\.')[1]

    pipelineJob('YPBuilds/ep' + MAJOR + MINOR + 'Y-unit-win32-x86_64-java17'){
	  description('Run Eclipse SDK Windows Tests ')
	  parameters {
	    stringParam('buildId', null, 'Build Id to test (such as I20120717-0800, N20120716-0800). ')
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
          steps {
              cleanWs() // workspace not cleaned by default
              bat \'\'\'
rem May want to try and restrict path, as we do on cron jobs, so we
rem have more consistent conditions.
rem export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:~/bin

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
set JAVA_HOME="C:\\PROGRA~1\\ECLIPS~1\\jdk-17.0.5.8-hotspot\\"
set JAVA_HOME
set Path="C:\\PROGRA~1\\ECLIPS~1\\jdk-17.0.5.8-hotspot\\bin";C:\\ProgramData\\Boxstarter;C:\\Windows\\system32;C:\\Windows;C:\\Windows\\System32\\Wbem;C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\;C:\\Windows\\System32\\OpenSSH\\;C:\\ProgramData\\chocolatey\\bin;C:\\tools\\cygwin\\bin;C:\\Program Files\\IcedTeaWeb\\WebStart\\bin;C:\\WINDOWS\\system32;C:\\WINDOWS;C:\\WINDOWS\\System32\\Wbem;C:\\WINDOWS\\System32\\WindowsPowerShell\\v1.0\\;C:\\WINDOWS\\System32\\OpenSSH\\;C:\\Users\\jenkins_vnc\\AppData\\Local\\Microsoft\\WindowsApps;%PATH%

ant -f getEBuilder.xml -Djava.io.tmpdir=%WORKSPACE%\\tmp -Djvm="C:\\PROGRA~1\\ECLIPS~1\\jdk-17.0.5.8-hotspot\\bin\\java.exe" -DbuildId=%buildId%  -DeclipseStream=%STREAM% -DEBUILDER_HASH=%EBUILDER_HASH%  -DdownloadURL="https://download.eclipse.org/eclipse/downloads/drops4/%buildId%" -Dargs=all -Dosgi.os=win32 -Dosgi.ws=win32 -Dosgi.arch=x86_64 -DtestSuite=all
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
