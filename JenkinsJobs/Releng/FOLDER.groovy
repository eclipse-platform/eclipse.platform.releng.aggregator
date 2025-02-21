folder('Releng') {
  description('Jobs related to routine releng tasks. Some are periodic, some are "manual" jobs ran only when needed.')
}

pipelineJob('Releng/PublishToMaven'){
	displayName('Publish to Maven')
	description('''\
<p>
This job uses the <a href="https://github.com/eclipse-cbi/p2repo-aggregator">CBI aggregator</a> to produce a Maven-compatible repository 
with contents as specified by the <a href="https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/blob/master/eclipse.platform.releng/publish-to-maven-central/SDK4Mvn.aggr">SDK4Mvn.aggr</a> and
then publishes the artifacts for <code>Eclipse-Platform</code>, <code>JDT</code> and <code>PDE</code> from the output:
</p>
<ul>
<li>
Snapshots are published to <a href="https://repo.eclipse.org/content/repositories/eclipse-snapshots/">https://repo.eclipse.org/content/repositories/eclipse-snapshots/</a>.
</li>
<li>
Releases are published to <a href="https://repo1.maven.org/maven2/org/eclipse/">Maven central</a> by publishing to a <a href="https://oss.sonatype.org/#stagingRepositories">staging repository</a>.
</li>
</ul>
<p>
The source repository to be published is specified via the
<a href="https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/blob/b6c45b1b38b74ad1fa7955e996976da4c259f926/eclipse.platform.releng/publish-to-maven-central/SDK4Mvn.aggr#L5">local</a> and 
<a href="https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/blob/b6c45b1b38b74ad1fa7955e996976da4c259f926/eclipse.platform.releng/publish-to-maven-central/SDK4Mvn.aggr#L8">remote</a> repository locations.
<b>
For a release build, these should specify the release repository location.
After the release, these should specify the current 4.x-I-Builds.
</b>
</p>
''')
	parameters { // Define parameters in job configuration to make them available even for the very first build after this job was (re)created.
		choiceParam('snapshotOrRelease', ['-snapshot' /*default*/, '-release'], '''\
<p>
The source repository to be published is specified via the
<a href="https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/blob/b6c45b1b38b74ad1fa7955e996976da4c259f926/eclipse.platform.releng/publish-to-maven-central/SDK4Mvn.aggr#L5">local</a> and 
<a href="https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/blob/b6c45b1b38b74ad1fa7955e996976da4c259f926/eclipse.platform.releng/publish-to-maven-central/SDK4Mvn.aggr#L8">remote</a> repository locations 
the <a href="https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/blob/master/eclipse.platform.releng/publish-to-maven-central/SDK4Mvn.aggr">SDK4Mvn.aggr</a>.
<b>
For a release build, these should specify the release repository location.
After the release, these should specify the current 4.x-I-Builds.
</b>
</p>
<ul>
<li>
Snapshots are published to <a href="https://repo.eclipse.org/content/repositories/eclipse-snapshots/">https://repo.eclipse.org/content/repositories/eclipse-snapshots/</a>.
</li>
<li>
Releases are published to <a href="https://repo1.maven.org/maven2/org/eclipse/">Maven central</a> by publishing to a <a href="https://oss.sonatype.org/#stagingRepositories">staging repository</a>.
</li>
</ul>
	''')
	}
	definition {
		cpsScm {
			lightweight(true)
			scm {
				github('eclipse-platform/eclipse.platform.releng.aggregator', 'master')
			}
			scriptPath('JenkinsJobs/Releng/publishToMaven.jenkinsfile')
		}
	}
}

pipelineJob('Releng/prepareNextDevCycle'){
	displayName('Prepare Next Development Cycle')
	description('Perform all steps to prepare the next development cycle of Eclipse.')
	parameters {
		booleanParam('DRY_RUN', true, 'If enabled, the final publication of all changes is skipped. Useful for debugging and to very that the pipeline behaves as intended.')
		stringParam('NEXT_RELEASE_VERSION', null, 'Version of the release to prepare, for example: 4.37')
		stringParam('PREVIOUS_RELEASE_CANDIDATE_ID', null, 'Id of the current release-candiate for the previous release, for example: S-4.36RC2-202505281830')
		stringParam('M1_DATE', null, 'Milestone 1 end date in the format yyyy-mm-dd, for example: 2025-07-04')
		stringParam('M2_DATE', null, 'Milestone 2 end date in the format yyyy-mm-dd, for example: 2025-07-25')
		stringParam('M3_DATE', null, 'Milestone 3 end date in the format yyyy-mm-dd, for example: 2025-08-15')
		stringParam('RC1_DATE', null, 'Release-Candidate 1 end date in the format yyyy-mm-dd, for example: 2025-08-22')
		stringParam('RC2_DATE', null, 'Release-Candidate 2 end date in the format yyyy-mm-dd, for example: 2025-08-29')
		stringParam('GA_DATE', null, 'Final general availability release date in the format yyyy-mm-dd, for example: 2025-09-10')
	}
	definition {
		cpsScm {
			lightweight(true)
			scm {
				github('eclipse-platform/eclipse.platform.releng.aggregator', 'master')
			}
			scriptPath('JenkinsJobs/Releng/prepareNextDevCycle.jenkinsfile')
		}
	}
}

