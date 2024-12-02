folder('SmokeTests') {
  displayName('Smoke Tests')
  description('Folder for Smoke Tests')
}

pipelineJob('SmokeTests/Start-smoke-tests'){
	description('Start all smoke tests for the Eclipse SDK')
	definition {
		cpsScm {
			lightweight(true)
			scm {
				github('eclipse-platform/eclipse.platform.releng.aggregator', 'master')
			}
			scriptPath('JenkinsJobs/SmokeTests/StartSmokeTests.jenkinsfile')
		}
	}
}
