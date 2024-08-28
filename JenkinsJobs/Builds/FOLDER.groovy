folder('Builds') {
  description('Eclipse periodic build jobs.')
}

pipelineJob('Builds/Build-Docker-images'){
	description('Build and publish custom Docker images')
	properties {
		pipelineTriggers {
			triggers {
				cron {
					spec('''
H	0 * * 2
H	8 * * 4
H 16 * * 6
					''')
				}
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
