folder('Releng') {
	description('Jobs related to routine releng tasks. Some are periodic, some are "manual" jobs ran only when needed.')
}

pipelineJob('Releng/PublishToMaven'){
	displayName('Publish to Maven')
	description('''\
<p>
This job uses the <a href="https://github.com/eclipse-cbi/p2repo-aggregator">CBI aggregator</a> to produce a Maven-compatible repository 
with contents as specified by the <a href="https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/blob/master/eclipse.platform.releng/publish-to-maven-central/SDK4Mvn.aggr">SDK4Mvn.aggr</a> and
then publishes the artifacts for <code>Eclipse-Platform</code>, <code>JDT</code>, <code>Equinox</code> and <code>PDE</code> from the output:
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
''')
	parameters { // Define parameters in job configuration to make them available even for the very first build after this job was (re)created.
		stringParam('sourceRepository', null, '''\
The URL of the source P2 repository to be published.<br>
For a snapshot publication, the 4.x-I-Builds child repository of the specific build should be specified, e.g. 'https://download.eclipse.org/eclipse/updates/4.37-I-builds/I20250710-1800/'<br>
<b>To deploy a <em>Release</em>, the corresponding release repository should be specified</b>, e.g. 'https://download.eclipse.org/eclipse/updates/4.36/R-4.36-202505281830/'<br>
If left blank (not recommended), the latest I-build is published.
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

pipelineJob('Releng/modifyP2CompositeRepository'){
	displayName('Modify P2 Composite Repository')
	description('Add or remove children from an Eclipse-P2 composite repository.')
	parameters {
		stringParam('repositoryPath', null, "Relative repository path from https://download.eclipse.org/. E.g. 'eclipse/updates/4.37-I-builds'")
		stringParam('add', null, 'Comma separated list of children to add. May be empty')
		stringParam('remove', null, 'Comma separated list of children to remove. May be empty')
		stringParam('sizeLimit', null, '''
The maximum number of childrem the modified composite should contain.
If the total number of children exceeds this limit (after adding new ones), a corresponding number of children is removed from the beginning of the list.
''')
	}
	definition {
		cpsScm {
			lightweight(true)
			scm {
				github('eclipse-platform/eclipse.platform.releng.aggregator', 'master')
			}
			scriptPath('JenkinsJobs/Releng/modifyP2CompositeRepository.jenkinsfile')
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

pipelineJob('Releng/renameAndPromote'){
	displayName('Rename and Promote')
	description('''\
This job does the "stage 1" or first part of a promotion.
It renames the files for Equinox and Eclipse, creates an appropriate repo on 'downloads', rsync's everything to 'downloads', but leave everything "invisible" -- unless someone knows the exact URL.
This allows two things. First, allows artifacts some time to "mirror" when that is needed.
But also, allows the sites and repositories to be examined for correctness before making them visible to the world.
The second (deferred) step that makes things visible works, in part, based on some output of this first step. Hence, they must "share a workspace".
''')
	parameters {
		stringParam('DROP_ID', null, '''\
The name (or, build id) of the build to rename and promote. Typically would be a value such as I20160530-2000 or M20160912-1000.
It must match the name of the build on the build machine.
		''')
		stringParam('CHECKPOINT', null, 'M1, M3, RC1, RC2, RC3 etc (blank for final releases).')
		stringParam('SIGNOFF_BUG', null, 'The issue that was used to "signoff" the checkpoint. If there are no unit test failures, this can be left blank. Otherwise a link is added to test page explaining that "failing unit tests have been investigated".')
		stringParam('TRAIN_NAME', null, 'The name of the release stream, typically yyyy-mm. For example: 2022-09')
		stringParam('STREAM', null, 'Needs to be all three files of primary version for the release, such as 4.7.1 or 4.8.0.')
		stringParam('DL_TYPE', null, "This is the build type we are promoting TO. I-builds promote to 'S' until 'R'.")
		stringParam('TAG', null, ''' For passing to the tagEclipseRelease job.
R is used for release builds. For example: R4_25
S is used for milestones and includes the milestone version. For example: S4_25_0_RC2
''')
	}
	definition {
		cpsScm {
			lightweight(true)
			scm {
				github('eclipse-platform/eclipse.platform.releng.aggregator', 'master')
			}
			scriptPath('JenkinsJobs/Releng/renameAndPromote.jenkinsfile')
		}
	}
}

pipelineJob('Releng/tagEclipseRelease'){
	displayName('Tag Eclipse Release')
	description('Tag promoted builds.')
	parameters {
		stringParam('tag', null, '''\
R is used for release builds. For example: R4_25
S is used for milestones and includes the milestone version. For example: S4_25_0_RC2
''')
		stringParam('buildID', null, 'I-build ID of the build that was promoted, for example: I20220831-1800')
		stringParam('annotation', null, '''\
GitHub issue to track tagging the release, for example:
'https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/3058' - Tag Eclipse 4.36 release
''')
	}
	definition {
		cpsScm {
			lightweight(true)
			scm {
				github('eclipse-platform/eclipse.platform.releng.aggregator', 'master')
			}
			scriptPath('JenkinsJobs/Releng/tagEclipseRelease.jenkinsfile')
		}
	}
}

pipelineJob('Releng/makeVisible'){
	displayName('Make Visible')
	description('''\
Make a 'release build', which was previously declared by running the 'Rename And Promote' job, visible.
The first part of a promotion -- the 'Rename And Promote' job -- puts the build at its final location, but keeps it hidden.
Therefore, both jobs have to share a 'workspace', and the output of the first job must remain in place until its time to "make visible".
''')
	parameters {
		stringParam('releaseBuildID', null, '''\
The id of the 'milestone', 'release-candidate' or 'GA-release' build to make visible.
Typically would be a value such as 'S-4.26M1-202209281800' or 'R-4.36-202505281830'.
It must match the name of the build on the download server.
''')
	}
	definition {
		cpsScm {
			lightweight(true)
			scm {
				github('eclipse-platform/eclipse.platform.releng.aggregator', 'master')
			}
			scriptPath('JenkinsJobs/Releng/makeVisible.jenkinsfile')
		}
	}
}

