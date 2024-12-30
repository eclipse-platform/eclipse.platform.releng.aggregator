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