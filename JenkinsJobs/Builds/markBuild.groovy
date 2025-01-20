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
			steps { // workspace is not always cleaned by default. Clean before custom tools are installed into workspace.
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
						dropsPath=${epDownloadDir}/downloads/drops4
						p2RepoPath=${epDownloadDir}/updates
						buildDir=${dropsPath}/${buildId}
						
						workingDir=${epDownloadDir}/workingDir
						
						workspace=${workingDir}/${JOB_NAME}-${BUILD_NUMBER}
						
						ssh genie.releng@projects-storage.eclipse.org rm -rf ${workingDir}/${JOB_NAME}*
						
						ssh genie.releng@projects-storage.eclipse.org mkdir -p ${workspace}
						ssh genie.releng@projects-storage.eclipse.org cd ${workspace}
						
						#get latest Eclipse platform product
						epRelDir=$(ssh genie.releng@projects-storage.eclipse.org ls -d --format=single-column ${dropsPath}/R-*|sort|tail -1)
						ssh genie.releng@projects-storage.eclipse.org tar -C ${workspace} -xzf ${epRelDir}/eclipse-platform-*-linux-gtk-x86_64.tar.gz
						
						#get requisite tools
						markingScriptName=\'''' + { switch (marker) {
							case 'stable': return 'addToComposite'
							case 'unstable': return 'removeFromComposite'
							default : throw new IllegalArgumentException("Unknown marker: ${marker}")
						} }() + ''''
						ssh genie.releng@projects-storage.eclipse.org wget -O ${workspace}/${markingScriptName}.xml https://download.eclipse.org/eclipse/relengScripts/cje-production/scripts/${markingScriptName}.xml
						
						#triggering ant runner
						baseBuilderDir=${workspace}/eclipse
						javaCMD=/opt/public/common/java/openjdk/jdk-17_x64-latest/bin/java
						
						launcherJar=$(ssh genie.releng@projects-storage.eclipse.org find ${baseBuilderDir}/. -name "org.eclipse.equinox.launcher_*.jar" | sort | head -1 )
						
						scp genie.releng@projects-storage.eclipse.org:${buildDir}/buildproperties.shsource .
						source ./buildproperties.shsource
						repoDir=/home/data/httpd/download.eclipse.org/eclipse/updates/${STREAMMajor}.${STREAMMinor}-${BUILD_TYPE}-builds
						
						devworkspace=${workspace}/workspace-antRunner
						devArgs=-Xmx512m
						extraArgs="${markingScriptName} -Drepodir=${repoDir} -Dcomplocation=${buildId}"
						ssh genie.releng@projects-storage.eclipse.org  ${javaCMD} -jar ${launcherJar} -nosplash -consolelog -debug -data $devworkspace \\
							-application org.eclipse.ant.core.antRunner -file ${workspace}/${markingScriptName}.xml ${extraArgs} -vmargs $devArgs
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
						ssh genie.releng@projects-storage.eclipse.org rm -rf ${workingDir}/${JOB_NAME}*
					\'''
					build job: 'Releng/updateIndex', wait: false
				}
			}
		}
	}
}
	''')
			}
		}
	}
}