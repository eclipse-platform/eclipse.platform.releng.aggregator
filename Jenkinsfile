pipeline {
	options {
		timeout(time: 240, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
	}
	agent {
		label "centos-7-6gb"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'openjdk-jdk11-latest'
	}
	stages {
		stage('initialize Gerrit review') {
			steps {
				gerritReview labels: [Verified: 0], message: "Build started $BUILD_URL"
			}
		}
		stage('Build') {
			steps {
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh """
					mvn clean verify -Dmaven.repo.local=$WORKSPACE/.m2/repository \
						-Pbree-libs \
						-Dmaven.test.skip=true -DskipTests=true -DaggregatorBuild=true \
						-DapiBaselineTargetDirectory=${WORKSPACE} \
						-Dproject.build.sourceEncoding=UTF-8 
					"""
				}
			}
			post {
				always {
					archiveArtifacts artifacts: '.*log,*/target/work/data/.metadata/.*log,*/tests/target/work/data/.metadata/.*log,apiAnalyzer-workspace/.metadata/.*log', allowEmptyArchive: true
					publishIssues issues:[scanForIssues(tool: java()), scanForIssues(tool: mavenConsole())]
				}
				unstable {
					gerritReview labels: [Verified: -1], message: "Build UNSTABLE (test failures) $BUILD_URL"
				}
				failure {
					gerritReview labels: [Verified: -1], message: "Build FAILED $BUILD_URL"
				}
			}
		}
		stage('Check freeze period') {
			steps {
				sh "wget https://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/plain/scripts/verifyFreezePeriod.sh"
				sh "chmod +x verifyFreezePeriod.sh"
				withCredentials([string(credentialsId: 'google-api-key', variable: 'GOOGLE_API_KEY')]) {
					sh './verifyFreezePeriod.sh'
				}
			}
			post {
				failure {
					gerritReview labels: [Verified: -1], message: "Build and test are OK, but Eclipse project is currently in a code freeze period.\nPlease wait for end of code freeze period before merging.\n $BUILD_URL"
				}
			}
		}
	}
	post {
		success {
			gerritReview labels: [Verified: 1], message: "Build Succcess $BUILD_URL"
		}
	}
}
