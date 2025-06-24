for (marker in [ 'stable', 'unstable' ]) {
	pipelineJob("Builds/mark${marker.capitalize()}"){
		displayName("Mark ${marker.capitalize()}")
		description("Mark a build as ${marker}.")
		parameters {
			stringParam('buildId', null, "ID of the build to be marked ${marker}.")
			if(marker == 'unstable') {
				stringParam('issueUrl', null, 'URL of the causing Github issue or PR.')
			}
		}
		definition {
			cps {
				sandbox()
				script('''\
pipeline {
	options {
		timestamps()
		buildDiscarder(logRotator(numToKeepStr:'5'))
	}
	agent {
		label 'basic'
	}
	stages {
		stage('Mark ''' + marker + ''''){
			environment {
				RELEASE_VER = readBuildProperty('RELEASE_VER')
			}
			steps {
				sshagent(['projects-storage.eclipse.org-bot-ssh']) {
					sh \'''#!/bin/bash -xe
						# this function executes command passed as command line parameter and 
						# if that command fails it exit with the same error code as the failed command
						
						# Strip spaces from the buildId and eclipseStream
						buildId=$(echo $buildId|tr -d ' ')
						issueUrl=$(echo $issueUrl|tr -d ' ')
						
						if [ -z "$buildId" ]; then
							echo "BuildId is empty! Exiting."
							exit 1
						fi''' + (marker == 'unstable' ? '''
						if [ -z $issueUrl ]; then
							echo "IssueUrl is empty! Exiting."
							exit 1
						fi''' : '') + '''
						epDownloadDir=/home/data/httpd/download.eclipse.org/eclipse
						buildDir="${epDownloadDir}/downloads/drops4/${buildId}"
						''' + { switch (marker) {
							case 'stable': return '''
						#Remove hidden attribute and unstable tags
						ssh genie.releng@projects-storage.eclipse.org rm -f ${buildDir}/buildHidden
						ssh genie.releng@projects-storage.eclipse.org rm -f ${buildDir}/buildUnstable
						'''
							case 'unstable': return '''
						# Convert URL of GH issue or PR into: 'organization/repository#number'
						label=$(echo "${issueUrl##'https://github.com/'}" | sed 's/\\\\/issues\\\\//#/g' | sed 's/\\\\/pull\\\\//#/g')
						#add unstable tags
						ssh genie.releng@projects-storage.eclipse.org rm -f ${buildDir}/buildUnstable
						echo "<p>This build is marked unstable due to <a href='${issueUrl}'>${label}</a>.</p>" > buildUnstable
						scp buildUnstable genie.releng@projects-storage.eclipse.org:${buildDir}/buildUnstable
						'''
							default : throw new IllegalArgumentException("Unknown marker: ${marker}")
						} }() + '''
					\'''
				}
				build job: 'Releng/updateIndex', wait: false
				build job: 'Releng/modifyP2CompositeRepository', wait: true, propagate: true, parameters: [
					string(name: 'repositoryPath', value: "eclipse/updates/${RELEASE_VER}-I-builds"),
					string(name: \'''' + { switch (marker) {
						case 'stable': return 'add'
						case 'unstable': return 'remove'
						default : throw new IllegalArgumentException("Unknown marker: ${marker}")
					} }() + '''\', value: "${buildId}")
				]
			}
		}
	}
}

def readBuildProperty(String name) {
	def value = sh(script: """curl https://download.eclipse.org/eclipse/downloads/drops4/${buildId}/buildproperties.properties | grep "^${name}" | cut -d'=' -f2""", returnStdout: true).trim()
	return value.startsWith('"') && value.endsWith('"') ? value.substring(1, value.length() - 1) : value
}
	''')
			}
		}
	}
}