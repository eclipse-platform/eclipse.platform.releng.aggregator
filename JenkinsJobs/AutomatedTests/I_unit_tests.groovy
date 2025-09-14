def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('JenkinsJobs/JobDSL.json'))

def TEST_CONFIGURATIONS = [
	[os: 'linux' , ws:'gtk'  , arch: 'x86_64' , javaVersion: 21, agentLabel: 'ubuntu-2404'        , javaHome: "tool(type:'jdk', name:'temurin-jdk21-latest')" ],
	[os: 'linux' , ws:'gtk'  , arch: 'x86_64' , javaVersion: 24, agentLabel: 'ubuntu-2404'        , javaHome: "tool(type:'jdk', name:'temurin-jdk24-latest')" ],
	[os: 'linux' , ws:'gtk'  , arch: 'x86_64' , javaVersion: 25, agentLabel: 'ubuntu-2404'        , javaHome: "tool(type:'jdk', name:'openjdk-jdk25-latest')" ],
	[os: 'macosx', ws:'cocoa', arch: 'aarch64', javaVersion: 21, agentLabel: 'nc1ht-macos11-arm64', javaHome: "'/Library/Java/JavaVirtualMachines/jdk-21.0.5+11-arm64/Contents/Home'" ],
	[os: 'macosx', ws:'cocoa', arch: 'x86_64' , javaVersion: 21, agentLabel: 'nc1ht-macos11-arm64', javaHome: "'/Library/Java/JavaVirtualMachines/jdk-21.0.5+11/Contents/Home'" ],
	[os: 'win32' , ws:'win32', arch: 'x86_64' , javaVersion: 21, agentLabel: 'qa6xd-win11'        , javaHome: "'C:\\\\Program Files\\\\Eclipse Adoptium\\\\jdk-21.0.5.11-hotspot'" ],
]

for (STREAM in config.Branches.keySet()){
	def MAJOR = STREAM.split('\\.')[0]
	def MINOR = STREAM.split('\\.')[1]
	for (TEST_CONFIG in TEST_CONFIGURATIONS){

		pipelineJob('AutomatedTests/ep' + MAJOR + MINOR + 'I-unit-' + TEST_CONFIG.os + '-' + TEST_CONFIG.arch + '-java' + TEST_CONFIG.javaVersion){
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
    label \'''' + TEST_CONFIG.agentLabel + ''''
  }
  stages {
    stage('Clean Workspace'){
      steps { // workspace is not always cleaned by default. Clean before custom tools are installed into workspace.
        cleanWs()
      }
    }
    stage('Run tests'){
      environment {
        // Declaring a jdk and ant the usual way in the 'tools' section, because of unknown reasons, breaks the usage of system commands like xvnc, pkill and sh
        JAVA_HOME = ''' + TEST_CONFIG.javaHome + '''
        ANT_HOME = tool(type:'ant', name:'apache-ant-latest')
        PATH = [pathOf("${JAVA_HOME}/bin"), pathOf("${ANT_HOME}/bin"), env.PATH].join(isUnix() ? ':' : ';')
        ANT_OPTS = "-Djava.io.tmpdir=${pathOf(env.WORKSPACE+'/tmp')}"
      }
      steps {''' + {
	if(TEST_CONFIG.os == 'linux' || TEST_CONFIG.os == 'macosx') {
		def runTestsScript = '''
        sh \'\'\'#!/bin/bash -x
          export LANG=en_US.UTF-8
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
          
          curl -o getEBuilder.xml https://download.eclipse.org/eclipse/relengScripts/testScripts/bootstrap/getEBuilder.xml
          
          echo JAVA_HOME: $JAVA_HOME
          echo ANT_HOME: $ANT_HOME
          echo PATH: $PATH
          
          env 1>envVars.txt 2>&1
          ant -diagnostics 1>antDiagnostics.txt 2>&1
          java -XshowSettings -version 1>javaSettings.txt 2>&1
          
          ant -f getEBuilder.xml -DbuildId=${buildId} \\
            -Dosgi.os=''' + TEST_CONFIG.os + ' -Dosgi.ws=' + TEST_CONFIG.ws + ' -Dosgi.arch=' + TEST_CONFIG.arch + ''' \\
            -DtestSuite=all
        \'\'\''''
		return TEST_CONFIG.os == 'linux' ? """
        xvnc(useXauthority: true) {${runTestsScript}
        }""" : runTestsScript
	} else if(TEST_CONFIG.os == 'win32') {
		return '''
        bat \'\'\'
          @REM tmp must already exist, for Java to make use of it, in subsequent steps
          mkdir tmp
          
          curl -o getEBuilder.xml https://download.eclipse.org/eclipse/relengScripts/testScripts/bootstrap/getEBuilder.xml
          
          echo JAVA_HOME: %JAVA_HOME%
          echo ANT_HOME: %ANT_HOME%
          echo PATH: %PATH%
          
          env 1>envVars.txt 2>&1
          cmd /c ant -diagnostics 1>antDiagnostics.txt 2>&1
          java -XshowSettings -version 1>javaSettings.txt 2>&1
          
          ant -f getEBuilder.xml -DbuildId=%buildId% ^
            -Dosgi.os=''' + TEST_CONFIG.os + ' -Dosgi.ws=' + TEST_CONFIG.ws + ' -Dosgi.arch=' + TEST_CONFIG.arch + ''' ^
            -DtestSuite=all
        \'\'\''''
	}else {
		throw new IllegalArgumentException('Unsupported OS: ' + TEST_CONFIG.os)
	}
}() + '''
        // For smaller test-suites see: https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/blob/be721e33c916b03c342e7b6f334220c6124946f8/production/testScripts/configuration/sdk.tests/testScripts/test.xml#L1893-L1903
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
  post {
    always {
      cleanWs()
    }
  }
}

def pathOf(String path){
	return path.replace('/', isUnix() ? '/' : '\\\\')
}

def installTemurinJDK(String version, String os, String arch, String releaseType='ga') {
	// Translate os/arch names that are different in the Adoptium API
	if (arch == 'x86_64') {
		arch == 'x64'
	}
	dir("${WORKSPACE}/java") {
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
