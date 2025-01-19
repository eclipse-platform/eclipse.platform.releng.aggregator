pipelineJob('Releng/updateIndex'){
	displayName('Update Download Index')
	description('This is a Job for recreating the download index.')
	definition {
		cps {
			sandbox()
			script('''
pipeline {
	options {
		timestamps()
		buildDiscarder(logRotator(numToKeepStr:'10'))
	}
	agent {
		label 'basic'
	}
	stages {
		stage('Update index'){
			steps { // workspace is not always cleaned by default. Clean before custom tools are installed into workspace.
				sshagent(['projects-storage.eclipse.org-bot-ssh']) {
					sh \'''#!/bin/bash -xe
						PHP_PAGE="createIndex4x.php"
						HTML_PAGE="index.html"
						TEMP_INDEX="tempIndex.html"
						
						wget --no-verbose -O ${TEMP_INDEX} https://download.eclipse.org/eclipse/downloads/${PHP_PAGE} 2>&1
						
						scp ${TEMP_INDEX} genie.releng@projects-storage.eclipse.org:/home/data/httpd/download.eclipse.org/eclipse/downloads/${HTML_PAGE}
					\'''
				}
			}
		}
	}
}
''')
		}
	}
}
