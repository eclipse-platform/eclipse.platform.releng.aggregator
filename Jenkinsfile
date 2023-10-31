pipeline {
	options {
		timeout(time: 240, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds(abortPrevious: true)
		timestamps()
	}
	agent {
		label "centos-latest"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'temurin-jdk17-latest'
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
				withCredentials([string(credentialsId: 'gpg-passphrase', variable: 'KEYRING_PASSPHRASE')]) {
					sh '''
					if [[ ${BRANCH_NAME} == master ]] || [[ ${BRANCH_NAME} =~ ^R[0-9]_[0-9]+_maintenance ]]; then
						MVN_ARGS="-Peclipse-sign"
					else
						MVN_ARGS=
						export KEYRING="deadbeef"
						export KEYRING_PASSPHRASE="none"
					fi
					mvn clean install -pl :eclipse-sdk-prereqs,:org.eclipse.jdt.core.compiler.batch -DlocalEcjVersion=99.99 -Dmaven.repo.local=$WORKSPACE/.m2/repository
					mvn clean verify -e -Dmaven.repo.local=$WORKSPACE/.m2/repository \
						-Pbree-libs \
						${MVN_ARGS} \
						-DskipTests=true \
						-Dcompare-version-with-baselines.skip=false \
						-DapiBaselineTargetDirectory=${WORKSPACE} \
						-Dgpg.passphrase="${KEYRING_PASSPHRASE}" \
						-Dcbi-ecj-version=99.99
					'''
				}

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
