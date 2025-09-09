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
	environment {
		MAVEN_OPTS = '-Xmx4000m'
	}
	stages {
		stage('Deploy parent-pom and SDK-target') {
			when {
				anyOf {
					branch 'master'
					branch 'R*_maintenance'
				}
			}
			steps {
				sh 'mvn clean deploy -f eclipse-platform-parent/pom.xml'
				sh 'mvn clean deploy -f eclipse.platform.releng.prereqs.sdk/pom.xml'
			}
		}
		stage('Deploy RelEng scripts') {
			when {
				allOf {
					branch 'master'
					expression { // Only deploy scripts on actual changes in the deployed folders
						return !sh(script:'''
							latestCommitID=$(curl https://download.eclipse.org/eclipse/relengScripts/state)
							git diff --name-only ${latestCommitID} HEAD \\
								production/ cje-production/ eclipse.platform.releng.tychoeclipsebuilder/
							''', returnStdout: true).trim().isEmpty()
					}
				}
			}
			steps {
				sshagent(['projects-storage.eclipse.org-bot-ssh']) {
					sh '''
						serverBase='/home/data/httpd/download.eclipse.org/eclipse'
						serverStaging="${serverBase}/relengScripts-staging"
						
						ssh genie.platform@projects-storage.eclipse.org rm -rf ${serverStaging}
						ssh genie.platform@projects-storage.eclipse.org mkdir -p ${serverStaging}
						
						ssh genie.platform@projects-storage.eclipse.org mkdir -p ${serverStaging}/testScripts
						scp -r production/testScripts/bootstrap genie.platform@projects-storage.eclipse.org:${serverStaging}/testScripts
						
						scp -r cje-production genie.platform@projects-storage.eclipse.org:${serverStaging}
						
						scp -r eclipse.platform.releng.tychoeclipsebuilder/entitlement genie.platform@projects-storage.eclipse.org:${serverStaging}
						
						# Create state file that contains the current commitID for later diffs in this stage's conditional
						commitID=$(git rev-parse HEAD)
						ssh genie.platform@projects-storage.eclipse.org "echo ${commitID}>${serverStaging}/state"
						
						# To minimize 'downtime', all files are first transfered to a staging folder on the server, 
						# then the existing folder is moved to 'disposal' and the 'staging' to the desired target.
						# Eventually the previously existing content is deleted with the 'disposal'-folder.
						serverTarget="${serverBase}/relengScripts"
						serverDisposal="${serverBase}/relengScripts-disposal"
						
						ssh genie.platform@projects-storage.eclipse.org "\
							mv -f ${serverTarget} ${serverDisposal} ;\
							mv -f ${serverStaging} ${serverTarget} &&\
							rm -rf ${serverDisposal}"
					'''
				}
			}
		}
		stage('Build') {
			steps {
				sh '''
					mvn clean install -pl :eclipse-sdk-prereqs,:org.eclipse.jdt.core.compiler.batch -DlocalEcjVersion=99.99 -Dmaven.repo.local=$WORKSPACE/.m2/repository -U
					mvn clean verify -e -Dmaven.repo.local=$WORKSPACE/.m2/repository \
						-T 1C \
						-Pbree-libs \
						-DskipTests=true \
						-Dcompare-version-with-baselines.skip=false \
						-DapiBaselineTargetDirectory=${WORKSPACE} \
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
					// To archive the built products, add to above's list: eclipse.platform.releng.tychoeclipsebuilder/eclipse.platform.repository/target/products/*
				}
			}
		}
	}
}
