pipeline {
	options {
		timeout(time: 240, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds(abortPrevious: true)
		timestamps()
	}
	agent {
		kubernetes {
			inheritFrom 'ubuntu-2404'
			yaml '''
			apiVersion: v1
			kind: Pod
			spec:
			  containers:
			  - name: "jnlp"
			    resources:
			      limits:
			        memory: "8Gi"
			        cpu: "4000m"
			      requests:
			        memory: "4Gi"
			        cpu: "2000m"
			'''.stripIndent()
		}
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'temurin-jdk25-latest'
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
		}
	}
	post {
		always {
			archiveArtifacts allowEmptyArchive: true, artifacts: '\
				.*log,*/target/work/data/.metadata/.*log,\
				*/tests/target/work/data/.metadata/.*log,\
				apiAnalyzer-workspace/.metadata/.*log,\
				sites/eclipse-platform-repository/target/repository/*,\
				'
			// To archive the built products, add to above's list: products/*/target/products/*
		}
	}
}
