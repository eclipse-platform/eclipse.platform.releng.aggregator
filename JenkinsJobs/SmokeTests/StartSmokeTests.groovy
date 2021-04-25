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
              stage('Ubuntu 18.04 Java11'){
                  steps {
                        build job: 'ep-pipeline-ubuntu18-java11', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Ubuntu 20.04 Java11'){
                  steps {
                        build job: 'ep-pipeline-ubuntu20-java11', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Ubuntu Latest Java11'){
                  steps {
                        build job: 'ep-smoke-test-ubuntuLatest-x86_64-java11', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Opensuse Leap Java11'){
                  steps {
                        build job: 'ep-smoke-test-opensuse-leap-x64-java11', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Centos 7.x Java11'){
                  steps {
                        build job: 'ep-pipeline-cen7x-java11', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Centos 8.x Java11'){
                  steps {
                        build job: 'ep-pipeline-cen8x-java11', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Centos 8.x ppc64le Java11'){
                  steps {
                        build job: 'ep-smoke-test-ppcle-java11', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Raspberry Pi Java11'){
                  steps {
                        build job: 'ep-smoke-test-arm64-java11', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Ubuntu 18.04 Java16'){
                  steps {
                        build job: 'ep-smoke-test-ubuntu18-x86_64-java16', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java16x64}")]
                  }
              }
              stage('Ubuntu 20.04 Java16'){
                  steps {
                        build job: 'ep-smoke-test-ubuntu20-x86_64-java16', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java16x64}")]
                  }
              }
              stage('Ubuntu Latest Java16'){
                  steps {
                        build job: 'ep-smoke-test-ubuntuLatest-x86_64', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java16x64}")]
                  }
              }
              stage('Opensuse Leap Java16'){
                  steps {
                        build job: 'ep-smoke-test-opensuse-leap-x64-java16', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java16x64}")]
                  }
              }
              stage('Centos 7.x Java16'){
                  steps {
                        build job: 'ep-smoke-test-centos7-x86_64-java16', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java16x64}")]
                  }
              }
              stage('Centos 8.x Java16'){
                  steps {
                        build job: 'ep-smoke-test-centos8-x86_64-java16', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java16x64}")]
                  }
              }
              stage('Raspberry Pi Java16'){
                  steps {
                        build job: 'ep-smoke-test-arm64-java16', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java16arm64}")]
                  }
              }
              stage('Centos 8.x ppc64le Java16'){
                  steps {
                        build job: 'ep-smoke-test-ppcle-java16', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Ubuntu 18.04 Java17'){
                  steps {
                        build job: 'ep-smoke-test-ubuntu18-x86_64-java16', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java17x64}")]
                  }
              }
              stage('Ubuntu 20.04 Java17'){
                  steps {
                        build job: 'ep-smoke-test-ubuntu20-x86_64-java16', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java17x64}")]
                  }
              }
              stage('Ubuntu Latest Java17'){
                  steps {
                        build job: 'ep-smoke-test-ubuntuLatest-x86_64', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java17x64}")]
                  }
              }
              stage('Opensuse Leap Java17'){
                  steps {
                        build job: 'ep-smoke-test-opensuse-leap-x64-java16', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java17x64}")]
                  }
              }
              stage('Centos 7.x Java17'){
                  steps {
                        build job: 'ep-smoke-test-centos7-x86_64-java16', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java17x64}")]
                  }
              }
              stage('Centos 8.x Java17'){
                  steps {
                        build job: 'ep-smoke-test-centos8-x86_64-java16', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java17x64}")]
                  }
              }
              stage('Raspberry Pi Java17'){
                  steps {
                        build job: 'ep-smoke-test-arm64-java17', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java17arm64}")]
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
