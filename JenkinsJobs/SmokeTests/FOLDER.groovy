folder('SmokeTests') {
  displayName('Smoke Tests')
  description('Folder for Smoke Tests')
}

pipelineJob('SmokeTests/Start-smoke-tests'){
	description('Start all smoke tests for the Eclipse SDK')
	parameters {  // Define parameters in job configuration to make them already available in the very first build
		stringParam {
			name('buildId')
			description('Build Id to test (such as I20240611-1800, N20120716-0800).')
			trim(true)
		}
	}
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
