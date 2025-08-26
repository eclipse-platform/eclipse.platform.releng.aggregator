def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('JenkinsJobs/JobDSL.json'))

folder('YPBuilds') {
  displayName('Y and P Builds')
  description('Builds and tests for the beta java builds.')
}

for (STREAM in config.Streams){
	def BRANCH = config.Branches[STREAM]

	pipelineJob('YPBuilds/Y-build-' + STREAM){
		description('Daily Maintenance Builds.')
		properties {
			pipelineTriggers {
				triggers {
					cron {
						spec('''TZ=America/Toronto
# Format: Minute Hour Day Month Day-of-week (1-7)
# - - - Beta Java Eclipse SDK builds - - - 
# Schedule: 10 AM every second day
0 10 * * 2,4,6
''')
					}
				}
			}
		}
		definition {
			cpsScm {
				lightweight(true)
				scm {
					github('eclipse-platform/eclipse.platform.releng.aggregator', BRANCH)
				}
				scriptPath('JenkinsJobs/Builds/build.jenkinsfile')
			}
		}
	}

}
