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
# format: Minute Hour Day Month Day of the week (0-7)

#Daily Y-build
0 10 * * *
#milestone week
#0 6 * * 2
#0 6 * * 4
#
#0 2 21 7 4
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
