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
              stage('Ubuntu 18.04 Java15'){
                  steps {
                        build job: 'ep-pipeline-ubuntu18-java15', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Ubuntu 20.04 Java11'){
                  steps {
                        build job: 'ep-pipeline-ubuntu20-java11', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Ubuntu 20.04 Java15'){
                  steps {
                        build job: 'ep-pipeline-ubuntu20-java15', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Centos 7.x Java11'){
                  steps {
                        build job: 'ep-pipeline-cen7x-java11', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Centos 7.x Java15'){
                  steps {
                        build job: 'ep-pipeline-cen7x-java15', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Centos 8.x Java11'){
                  steps {
                        build job: 'ep-pipeline-cen8x-java11', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Centos 8.x Java15'){
                  steps {
                        build job: 'ep-pipeline-cen8x-java15', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Centos 8.x ppc64le Java11'){
                  steps {
                        build job: 'ep-smoke-test-ppcle-java11', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Centos 8.x ppc64le Java15'){
                  steps {
                        build job: 'ep-smoke-test-ppcle-java15', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Raspberry Pi OS arm64 Java11'){
                  steps {
                        build job: 'ep-smoke-test-arm64-java11', parameters: [string(name: 'buildId', value: "${params.buildId}")]
                  }
              }
              stage('Raspberry Pi Java15'){
                  steps {
                        build job: 'ep-smoke-test-arm64-java15', parameters: [string(name: 'buildId', value: "${params.buildId}")]
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
