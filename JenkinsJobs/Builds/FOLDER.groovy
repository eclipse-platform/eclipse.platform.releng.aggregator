folder('Builds') {
  description('Eclipse periodic build jobs.')
}

pipelineJob('Builds/Build-Docker-images'){
	description('Build and publish custom Docker images')
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
