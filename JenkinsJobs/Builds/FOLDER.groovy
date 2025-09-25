def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('JenkinsJobs/JobDSL.json'))

folder('Builds') {
  description('Eclipse periodic build jobs.')
}

config.I.streams.each{ STREAM, configuration ->

	pipelineJob('Builds/I-build-' + STREAM){
		description('Daily Eclipse Integration builds.')
		properties {
			pipelineTriggers {
				triggers {
					cron {
						spec(configuration.schedule ? """TZ=America/Toronto
# Format: Minute Hour Day Month Day-of-week (1-7)
# - - - Integration Eclipse SDK builds - - - 
# Schedule: 6 PM every day until end of RC2
${configuration.schedule}
""" : '')
					}
				}
			}
		}
		definition {
			cpsScm {
				lightweight(true)
				scm {
					github('eclipse-platform/eclipse.platform.releng.aggregator', configuration.branch)
				}
				scriptPath('JenkinsJobs/Builds/build.jenkinsfile')
			}
		}
	}

}

pipelineJob('Builds/Build-Docker-images'){
	description('Build and publish custom Docker images')
	properties {
		pipelineTriggers {
			triggers {
				cron { spec('@weekly') }
			}
		}
	}
	definition {
		cpsScm {
			lightweight(true)
			scm {
				github('eclipse-platform/eclipse.platform.releng.aggregator', 'master')
			}
			scriptPath('JenkinsJobs/Builds/DockerImagesBuild.jenkinsfile')
		}
	}
}

pipelineJob('Builds/mark-build'){
	displayName("Mark build")
	description("Mark a build as stable or unstable.")
	parameters {
		stringParam('buildId', null, "ID of the build to be marked.")
		choiceParam('markAs', [
			'STABLE',
			'UNSTABLE',
		], 'The kind of marker to apply to (respectively remove from) the specified build.')
		stringParam('issueURL', null, 'URL of the causing Github issue or PR (<em>only relevant if the build is marked as unstable<em>).')
	}

	definition {
		cpsScm {
			lightweight(true)
			scm {
				github('eclipse-platform/eclipse.platform.releng.aggregator', 'master')
			}
			scriptPath('JenkinsJobs/Builds/markBuild.jenkinsfile')
		}
	}
}
