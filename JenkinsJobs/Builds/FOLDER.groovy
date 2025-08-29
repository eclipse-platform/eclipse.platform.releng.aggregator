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
						spec('')
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
				github('eclipse-platform/eclipse.platform.releng.aggregator', 'R4_37_maintenance')
			}
			scriptPath('JenkinsJobs/Builds/DockerImagesBuild.jenkinsfile')
		}
	}
}
