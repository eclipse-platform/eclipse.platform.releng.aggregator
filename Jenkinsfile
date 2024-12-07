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
		stage('Deploy parent-pom and SDK-target') {
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
		stage('Deploy RelEng scripts') {
			when {
				branch 'master'
			}
			steps {
				sshagent(['projects-storage.eclipse.org-bot-ssh']) {
					sh '''
						serverBase='/home/data/httpd/download.eclipse.org/eclipse/'
						serverTarget="${serverBase}/relengScripts"
						serverCradle="${serverBase}/relengScripts-cradle"
						serverGrave="${serverBase}/relengScripts-grave"
						#To minimize the 'downtime' transfer all files to a 'cradle' folder on the server first 
						# then move the existing folder to a 'grave' and move the 'cradle' to the desired target.
						# Eventually delete the previously existing content from the grave.
						
						ssh genie.platform@projects-storage.eclipse.org rm -rf ${serverCradle}
						ssh genie.platform@projects-storage.eclipse.org mkdir -p ${serverCradle}
						
						scp -r production genie.platform@projects-storage.eclipse.org:${serverCradle}
						scp -r scripts genie.platform@projects-storage.eclipse.org:${serverCradle}
						scp -r cje-production genie.platform@projects-storage.eclipse.org:${serverCradle}
						
						pushd eclipse.platform.releng.tychoeclipsebuilder/eclipse
						scp -r buildScripts genie.platform@projects-storage.eclipse.org:${serverCradle}
						popd
						
						pushd eclipse.platform.releng.tychoeclipsebuilder
						scp -r entitlement genie.platform@projects-storage.eclipse.org:${serverCradle}
						popd
						
						ssh genie.platform@projects-storage.eclipse.org "\
							mv -f ${serverTarget} ${serverGrave} ;\
							mv -f ${serverCradle} ${serverTarget} &&\
							rm -rf ${serverGrave}"
					'''
				}
			}
		}
		stage('Build') {
		    when { not { branch pattern: "prepare_R.*", comparator: "REGEXP" } }
			steps {
				sh '''
					mvn clean install -pl :eclipse-sdk-prereqs,:org.eclipse.jdt.core.compiler.batch -DlocalEcjVersion=99.99 -Dmaven.repo.local=$WORKSPACE/.m2/repository -U
					mvn clean verify -e -Dmaven.repo.local=$WORKSPACE/.m2/repository \
						-T 1C \
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
