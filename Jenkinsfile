pipeline {
	options {
		timeout(time: 240, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds(abortPrevious: true)
		timestamps()
	}
	agent {
		label "centos-latest-8gb"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'temurin-jdk21-latest'
	}
	stages {
		stage('Use master') {
			steps {
				sh 'git submodule foreach "git fetch origin master; git checkout FETCH_HEAD"'
			}
		}
		stage('Deploy eclipse-platform-parent pom and eclipse-sdk target') {
			when {
				anyOf {
					branch 'master'
					branch 'R*_maintenance'
					branch 'prepare_R*'
				}
			}
			steps {
				sh 'mvn clean deploy -f eclipse-platform-parent/pom.xml'
				sh 'mvn clean deploy -f eclipse.platform.releng.prereqs.sdk/pom.xml'
			}
		}
		stage('Build') {
		    when { not { branch pattern: "prepare_R.*", comparator: "REGEXP" } }
			steps {
				sh '''
					mvn clean install -pl :eclipse-sdk-prereqs,:org.eclipse.jdt.core.compiler.batch -DlocalEcjVersion=99.99 -Dmaven.repo.local=$WORKSPACE/.m2/repository -U
					mvn clean verify -e -Dmaven.repo.local=$WORKSPACE/.m2/repository \
						-Pbree-libs \
						-DskipTests=true \
						-Dcompare-version-with-baselines.skip=false \
						-DapiBaselineTargetDirectory=${WORKSPACE} \
						-Dgpg.passphrase="${KEYRING_PASSPHRASE}" \
						-Dcbi-ecj-version=99.99 \
						-U
				'''
			}
			post {
				always {
					archiveArtifacts allowEmptyArchive: true, artifacts: '\
						.*log,*/target/work/data/.metadata/.*log,\
						*/tests/target/work/data/.metadata/.*log,\
						apiAnalyzer-workspace/.metadata/.*log,\
						eclipse.platform.releng.tychoeclipsebuilder/eclipse.platform.repository/target/repository/*'
				}
			}
		}
	}
}
