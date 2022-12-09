pipeline {
	options {
		timeout(time: 300, unit: 'MINUTES')
		timestamps()
		buildDiscarder(logRotator(numToKeepStr:'5'))
	}
  agent any

  stages {
	  stage('Trigger tests'){
          parallel {
              stage('Ubuntu 22.04 Java17'){
                  steps {
                        build job: 'ep-smoke-test-ubuntu22', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java17x64}")]
                  }
              }
              stage('Opensuse Leap Java17'){
                  steps {
                        build job: 'ep-smoke-test-opensuse-leap', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java17x64}")]
                  }
              }
              stage('Centos 9.x Java17'){
                  steps {
                        build job: 'ep-smoke-test-centos9', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java17x64}")]
                  }
              }
              stage('Centos 8 arm64 Java17'){
                  steps {
                        build job: 'ep-smoke-test-arm64', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java17arm64}")]
                  }
              }
              stage('Centos 8.x ppc64le Java17'){
                  steps {
                        build job: 'ep-smoke-test-ppcle', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java17ppcle}")]
                  }
              }
              stage('Ubuntu 22.04 Java19'){
                  steps {
                        build job: 'ep-smoke-test-ubuntu22', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java19x64}")]
                  }
              }
              stage('Opensuse Leap Java19'){
                  steps {
                        build job: 'ep-smoke-test-opensuse-leap', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java19x64}")]
                  }
              }
              stage('Centos 9.x Java19'){
                  steps {
                        build job: 'ep-smoke-test-centos9', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java19x64}")]
                  }
              }
              stage('Centos 8 arm64 Java19'){
                  steps {
                        build job: 'ep-smoke-test-arm64', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java19arm64}")]
                  }
              }
          }
		}
	}
	post {
        aborted {
            emailext body: "Smoke Tests failed. Please go to ${BUILD_URL} and check the build failure",
            subject: "Smoke test for ${buildId} - ABORTED", 
            to: "sravankumarl@in.ibm.com sravan.lakkimsetti@gmail.com",
            from:"genie.releng@eclipse.org"
        }
        failure {
            emailext body: "Smoke Tests failed. Please go to ${BUILD_URL} and check the build failure",
            subject: "Smoke test for ${buildId} - FAILED", 
            to: "sravankumarl@in.ibm.com sravan.lakkimsetti@gmail.com",
            from:"genie.releng@eclipse.org"
        }
        unstable {
            emailext body: "Smoke Tests failed. Please go to ${BUILD_URL} and check the test failures",
            subject: "Smoke test for ${buildId} - UNSTABLE", 
            to: "sravankumarl@in.ibm.com sravan.lakkimsetti@gmail.com",
            from:"genie.releng@eclipse.org"
        }
        success {
            emailext body: "Smoke Tests successful",
            subject: "Smoke test for ${buildId} - SUCCESS", 
            to: "sravankumarl@in.ibm.com sravan.lakkimsetti@gmail.com",
            from:"genie.releng@eclipse.org"
        }
	}
}
