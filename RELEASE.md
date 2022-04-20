# Releng-Tasks 2.0

[Eclipse Major Release Schedule](https://wiki.eclipse.org/Simultaneous_Release)

[Previous Documentation](https://wiki.eclipse.org/Releng-Tasks)

## Milestone Releases

**Friday before release week**:
 * Update [I-builds](https://ci.eclipse.org/releng/view/Builds/) to build on the milestone schedule (Twice daily at 06:00 EST and 18:00 EST except Thursday).
 * Create prerequisite issues for tracking ECF, EMF and Orbit
   * See previous Bugzilla issues for [ECF](https://bugs.eclipse.org/bugs/show_bug.cgi?id=578002), [EMF](https://bugs.eclipse.org/bugs/show_bug.cgi?id=578003) and [Orbit](https://bugs.eclipse.org/bugs/show_bug.cgi?id=578004)
 * Send reminder email for upcoming milestone week to platform-releng-dev@eclipse.org, platform-dev@eclipse.org, eclipse-dev@eclipse.org and equinox-dev@eclipse.org
   * [Example from 4.23 M1](https://www.eclipse.org/lists/platform-releng-dev/msg38067.html) but the usual schedule:
     * Monday: Last day of development.
     * Tuesday: Tests
     * Wednesday: 
       - Fixes from Tuesday. 
       - "New and Noteworthy entries due. 
       - Release Candidate is built Wednesday evening at 6PM EST. 
     * Thursday: Sign-Off
     * Friday: 
       - Build delcared and released.
       - Make sure to mention that the Master branch will stay closed until the milestone is officially released.

 **Milestone Week**
   - **Wednesday**:
     * Verify that EMF, ECF and Orbit contributions have been included (if applicable).
     * Final release candidate [build](https://ci.eclipse.org/releng/view/Builds/) runs at 6PM EST.
     * Because of time zones, PST/EST might want to do Thursday's tasks EOD Wednesday after the release candidate has built. 
   - **Thursday**:
     * Create a Sign-Off issue for the Release Candidate in [eclipse.platform.releng.aggregator](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues). 
       * [Example issue #198](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/198)
       * If RC build is successful there should be an email sent to platform-releng-dev with the download and repository links needed for the issue. 
       * Deadlines are Friday at whatever time you intend to promote the RC.
     * Send email asking for component go/no go to platform-releng-dev@eclipse.org, platform-dev@eclipse.org, eclipse-dev@eclipse.org and equinox-dev@eclipse.org
       * Just [1 line](https://www.eclipse.org/lists/platform-releng-dev/msg38086.html) asking for sign off on the GitHub issue created in the previous step.
   - **Friday**:
     * **Promote** the release candidate (if go).
       * Run the [rename and promote](https://ci.eclipse.org/releng/job/eclipse.releng.renameAndPromote/) job in Jenkins
         - DROP_ID: Release candidate build ID (make sure there is no space before or after the ID).
         - CHECKPOINT: M1 etc (blank for final releases)
         - SIGNOFF_BUG: Needs to be updated to sign-off issue (numeric part only)
         - TRAIN_NAME: Whenever the current GA release is planned for (formatted 4 digit year - 2 digit month, i.e `2022-06`)
         - STREAM: 4.24.0 etc
         - DL_TYPE: S is used to promote I-builds.
         - After the build  find and open the mail template [artifact](https://ci.eclipse.org/releng/job/eclipse.releng.renameAndPromote/lastSuccessfulBuild/artifact/) and have it ready.
       * Contribute to SimRel
         - If you have not already set up SimRel you can do so using Auto Launch [here](https://www.eclipse.org/setups/installer/?url=https://git.eclipse.org/c/oomph/org.eclipse.oomph.git/plain/setups/interim/SimultaneousReleaseTrainConfiguration.setup&show=true)
         - Clone [org.eclipse.simrel.build](https://git.eclipse.org/c/simrel/org.eclipse.simrel.build.git) (Should have been done by the installer during set up, but make sure you have latest).
           1. Open simrel.aggr in Eclipse
           2. Change to the properties view
           3. Select Contribution:Eclipse > Mapped Repository
           4. Update the Location property to the "Specific repository for building against" in the mailtemplate.txt from promotion.
           5. Repeat steps 3 and 4 for Equinox
           6. Commit Simrel updates to Gerrit
              - Message should use year-month format, i.e "Simrel updates for Eclipse and Equinox for 2022-06 M1"
       * Make the build visible
         - Run [this](https://ci.eclipse.org/releng/job/eclipse.releng.stage2DeferredMakeVisible/) job in Releng jenkins
         - Parameters should match Rename and Promote job
       * Run [Tag Eclipse Release](https://ci.eclipse.org/releng/job/TagEclipseRelease) to tag the source code.
         - Tag Parameter should match stream version, i.e `S4_24_0M1` etc
       * Send email that the M1 build is available
         - Use the mail template from the promotion build [artifacts](https://ci.eclipse.org/releng/job/eclipse.releng.renameAndPromote/lastSuccessfulBuild/artifact/) in Jenkins to get the download urls.
     * **Update ECJ compiler** in the platform build.
       * To find the new compiler version:
         - Go to the update site for the release candidate
         - Click `plugins`
         - Find `org.eclipse.jdt.core.complier.batch_${ecjversion}.jar`
       * Edit the [copyAndDeployJDTCompiler](https://ci.eclipse.org/jdt/job/copyAndDeployJDTCompiler) job in Jenkins
         - Only JDT committers can run the job, but Releng/Platform committers can configure it
         - Update the default values of the `versionfolder`, `buildid` and `ecjversion` parameters.
         - Update the build triggers to schedule a build for the current date.
       * Finally update the `cbi-ecj-version` in [eclipse.platform.releng.aggregator/eclipse-platform-parent/pom.xml](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/blob/master/eclipse-platform-parent/pom.xml)

## GA Releases

TBD