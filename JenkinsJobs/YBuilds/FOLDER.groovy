def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('JenkinsJobs/JobDSL.json'))

folder('YPBuilds') {
  displayName('Y and P Builds')
  description('Builds and tests for the beta java builds.')
}

config.Y.streams.each{ STREAM, configuration ->

	pipelineJob('YPBuilds/Y-build-' + STREAM){
		description('Daily Maintenance Builds.')
		disabled(configuration.disabled?.toBoolean() ?: false)
		properties {
			pipelineTriggers {
				triggers {
					cron {
						spec(configuration.schedule ? """TZ=America/Toronto
# Format: Minute Hour Day Month Day-of-week (1-7)
# - - - Beta Java Eclipse SDK builds - - - 
# Schedule: 10 AM every second day (and every day in Java RC phase)
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
