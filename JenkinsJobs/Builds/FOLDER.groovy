def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('JenkinsJobs/JobDSL.json'))

folder('Builds') {
  description('Eclipse periodic build jobs.')
}

for (STREAM in config.Streams){
	def BRANCH = config.Branches[STREAM]

	pipelineJob('Builds/I-build-' + STREAM){
		description('Daily Eclipse Integration builds.')
		properties {
			pipelineTriggers {
				triggers {
					cron {
						spec('''TZ=America/Toronto
# Format: Minute Hour Day Month Day-of-week (1-7)
# - - - Integration Eclipse SDK builds - - - 
# Schedule: 6 PM every day until end of RC2
0 18 * 8-10 *
0 18 1-26 11 *
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
