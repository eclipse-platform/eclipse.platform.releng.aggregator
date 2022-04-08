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
                        build job: 'ep-smoke-test-ubuntu18', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java11x64}"), string(name: 'secManager', value: '')]
                  }
              }
              stage('Ubuntu 20.04 Java11'){
                  steps {
                        build job: 'ep-smoke-test-ubuntu20', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java11x64}"), string(name: 'secManager', value: '')]
                  }
              }
              stage('Ubuntu Latest Java11'){
                  steps {
                        build job: 'ep-smoke-test-ubuntuLatest', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java11x64}"), string(name: 'secManager', value: '')]
                  }
              }
              stage('Opensuse Leap Java11'){
                  steps {
                        build job: 'ep-smoke-test-opensuse-leap', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java11x64}"), string(name: 'secManager', value: '')]
                  }
              }
              stage('Centos 7.x Java11'){
                  steps {
                        build job: 'ep-smoke-test-centos7', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java11x64}"), string(name: 'secManager', value: '')]
                  }
              }
              stage('Centos 8.x Java11'){
                  steps {
                        build job: 'ep-smoke-test-centos8', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java11x64}"), string(name: 'secManager', value: '')]
                  }
              }
              stage('Centos 8 arm64 Java11'){
                  steps {
                        build job: 'ep-smoke-test-arm64', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java11arm64}"), string(name: 'secManager', value: '')]
                  }
              }
              stage('Centos 8.x ppc64le Java11'){
                  steps {
                        build job: 'ep-smoke-test-ppcle', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java11ppcle}"), string(name: 'secManager', value: '')]
                  }
              }
              stage('Ubuntu 18.04 Java17'){
                  steps {
                        build job: 'ep-smoke-test-ubuntu18', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java17x64}")]
                  }
              }
              stage('Ubuntu 20.04 Java17'){
                  steps {
                        build job: 'ep-smoke-test-ubuntu20', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java17x64}")]
                  }
              }
              stage('Ubuntu Latest Java17'){
                  steps {
                        build job: 'ep-smoke-test-ubuntuLatest', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java17x64}")]
                  }
              }
              stage('Opensuse Leap Java17'){
                  steps {
                        build job: 'ep-smoke-test-opensuse-leap', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java17x64}")]
                  }
              }
              stage('Centos 7.x Java17'){
                  steps {
                        build job: 'ep-smoke-test-centos7', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java17x64}")]
                  }
              }
              stage('Centos 8.x Java17'){
                  steps {
                        build job: 'ep-smoke-test-centos8', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java17x64}")]
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
              stage('Ubuntu 18.04 Java18'){
                  steps {
                        build job: 'ep-smoke-test-ubuntu18', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java18x64}")]
                  }
              }
              stage('Ubuntu 20.04 Java18'){
                  steps {
                        build job: 'ep-smoke-test-ubuntu20', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java18x64}")]
                  }
              }
              stage('Ubuntu Latest Java18'){
                  steps {
                        build job: 'ep-smoke-test-ubuntuLatest', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java18x64}")]
                  }
              }
              stage('Opensuse Leap Java18'){
                  steps {
                        build job: 'ep-smoke-test-opensuse-leap', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java18x64}")]
                  }
              }
              stage('Centos 7.x Java18'){
                  steps {
                        build job: 'ep-smoke-test-centos7', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java18x64}")]
                  }
              }
              stage('Centos 8.x Java18'){
                  steps {
                        build job: 'ep-smoke-test-centos8', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java18x64}")]
                  }
              }
              stage('Centos 8 arm64 Java18'){
                  steps {
                        build job: 'ep-smoke-test-arm64', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java18arm64}")]
                  }
              }
              stage('Centos 8.x ppc64le Java18'){
                  steps {
                        build job: 'ep-smoke-test-ppcle', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java18ppcle}")]
                  }
              }
              stage('Ubuntu 18.04 Java19'){
                  steps {
                        build job: 'ep-smoke-test-ubuntu18', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java19x64}")]
                  }
              }
              stage('Ubuntu 20.04 Java19'){
                  steps {
                        build job: 'ep-smoke-test-ubuntu20', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java19x64}")]
                  }
              }
              stage('Ubuntu Latest Java19'){
                  steps {
                        build job: 'ep-smoke-test-ubuntuLatest', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java19x64}")]
                  }
              }
              stage('Opensuse Leap Java19'){
                  steps {
                        build job: 'ep-smoke-test-opensuse-leap', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java19x64}")]
                  }
              }
              stage('Centos 7.x Java19'){
                  steps {
                        build job: 'ep-smoke-test-centos7', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java19x64}")]
                  }
              }
              stage('Centos 8.x Java19'){
                  steps {
                        build job: 'ep-smoke-test-centos8', parameters: [string(name: 'buildId', value: "${params.buildId}"), string(name: 'javaDownload', value: "${params.java19x64}")]
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
