folder('Cleanup') {
  description('Cleanup Scripts.')
}

pipelineJob('Cleanup/cleanupBuilds'){
	displayName('Daily Cleanup of old Builds')
	description('Remove old builds from the downloads servers.')
	properties {
		pipelineTriggers {
			triggers {
				cron {
					spec('''TZ=America/Toronto
0 4 * * *
0 16 * * *
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
			scriptPath('JenkinsJobs/Cleanup/cleanupBuilds.jenkinsfile')
		}
	}
}

pipelineJob('Cleanup/cleanupReleaseArtifacts'){
	displayName('Cleanup Release Artifacts')
	description('Cleanup major artifacts from previous releases at the beginning of a new release.')
	definition {
		cpsScm {
			lightweight(true)
			scm {
				github('eclipse-platform/eclipse.platform.releng.aggregator', 'master')
			}
			scriptPath('JenkinsJobs/Cleanup/cleanupReleaseArtifacts.jenkinsfile')
		}
	}
}
