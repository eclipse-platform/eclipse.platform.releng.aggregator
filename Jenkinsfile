pipeline {
	options {
		timeout(time: 240, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds(abortPrevious: true)
	}
	agent {
		label "centos-latest"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'openjdk-jdk17-latest'
	}
	stages {
		stage('initialize PGP') {
			steps {
				withCredentials([file(credentialsId: 'secret-subkeys.asc', variable: 'KEYRING')]) {
					sh 'gpg --batch --import "${KEYRING}"'
					sh 'for fpr in $(gpg --list-keys --with-colons  | awk -F: \'/fpr:/ {print $10}\' | sort -u); do echo -e "5\ny\n" |  gpg --batch --command-fd 0 --expert --edit-key ${fpr} trust; done'
				}
			}
		}
		stage('Use master') {
			steps {
				sh 'git submodule foreach "git fetch origin master; git checkout FETCH_HEAD"'
			}
		}
		stage('Deploy eclipse-platform-parent pom and eclipse-sdk target') {
			when {
				branch 'master'
			}
			steps {
				sh 'mvn clean deploy -f eclipse-platform-parent/pom.xml'
				sh 'mvn clean deploy -f eclipse.platform.releng.prereqs.sdk/pom.xml'
			}
		}
		stage('Build') {
			steps {
				withCredentials([string(credentialsId: 'gpg-passphrase', variable: 'KEYRING_PASSPHRASE')]) {
					sh '''
					mvn clean verify -Dmaven.repo.local=$WORKSPACE/.m2/repository \
						-Pbree-libs -Peclipse-sign \
						-Dmaven.test.skip=true -DskipTests=true -DaggregatorBuild=true \
						-DapiBaselineTargetDirectory=${WORKSPACE} \
						-Dgpg.passphrase="${KEYRING_PASSPHRASE}" \
						-Dproject.build.sourceEncoding=UTF-8 
					'''
				}

			}
			post {
				always {
					archiveArtifacts artifacts: '.*log,*/target/work/data/.metadata/.*log,*/tests/target/work/data/.metadata/.*log,apiAnalyzer-workspace/.metadata/.*log,eclipse.platform.releng.tychoeclipsebuilder/eclipse.platform.repository/target/repository/*', allowEmptyArchive: true
				}
			}
		}
	}
}
