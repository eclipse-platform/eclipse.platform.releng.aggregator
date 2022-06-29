# Releng-Tasks 2.0

[Eclipse Major Release Schedule](https://wiki.eclipse.org/Simultaneous_Release)

[Previous Documentation](https://wiki.eclipse.org/Releng-Tasks)

## Milestone and RC Releases

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
           5. Commit Simrel updates to Gerrit
              - Message should use year-month format, i.e "Simrel updates for Eclipse and Equinox for 2022-06 M1"
       * Make the build visible
         - Run [this](https://ci.eclipse.org/releng/job/eclipse.releng.stage2DeferredMakeVisible/) job in Releng jenkins
         - Parameters should match Rename and Promote job
       * Run [Tag Eclipse Release](https://ci.eclipse.org/releng/job/TagEclipseRelease) to tag the source code.
         - Tag Parameter should match stream version, i.e `S4_24_0M1` etc
       * Send email that the M1 build is available
         - Use the mail template from the promotion build [artifacts](https://ci.eclipse.org/releng/job/eclipse.releng.renameAndPromote/lastSuccessfulBuild/artifact/) in Jenkins to get the download urls.
       * For **Milestone builds** return the I-builds to the normal schedule.
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
     * **After RC1**
       * Open an issue in [eclipse.platform.releng.aggregator](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator) to enable the API Freeze Report for the current release. 
         - Open [buildproperties.txt](cje-production/buildproperties.txt) 
         - Comment out the empty `FREEZE_PARAMS` line and uncomment the line with the freeze arguments. Update `freezeBaseURL` with the RC1 build number.
         - Comment out the empty `API_FREEZE_REF_LABEL` line and update the version of the label to RC1.
       * Leave the I-builds running on the milestone schedule for RC2. 
       * Comment on EMF, ECF and Orbit issues to ask for final release builds.

## GA Releases

**After RC2**
  * Create an issue to track the current release tasks (see [Release 4.24](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/273)).
    - Tag @lshanmug (New & Noteworthy), @SarikaSinha (Readme), @vik-chand (Tips & Tricks), @ktatavarthi (JDT and Platform Migration Guides), @niraj-modi (SWT Javadoc bash).
    - Update the Acknowledgements.
  * Create an issue to track preparation work for the next stream (see [Preparation work for 4.25 (2022-09)](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/284)).
  * **Maintenance Branch Creation:**
    - Create an issue for [maintenance branch creation](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/301) from the item in the Preparation issue created above.  
      Create the branch from RC2 using the [ep-createMaintenanceBranch](https://ci.eclipse.org/releng/job/ep-createMaintenanceBranch/) job in the Eclipse Platform Releng jeknins.
    - Create an issue for [moving I-builds to the maintenance branch](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/317).
    - Create an issue to [Update parent pom and target sdk deployment jobs](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/286) in jenkins.  
      Update [deploy-eclipse-sdk-target-pom](https://ci.eclipse.org/releng/job/deploy-eclipse-sdk-target-pom/) and [deploy-eclipse-platform-parent-pom](https://ci.eclipse.org/releng/job/deploy-eclipse-platform-parent-pom/) jobs to include the new maintenance branch.
  * Create an [issue](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/289) and update the [build calendar](https://calendar.google.com/calendar/u/0?cid=cHJmazI2ZmRtcHJ1MW1wdGxiMDZwMGpoNHNAZ3JvdXAuY2FsZW5kYXIuZ29vZ2xlLmNvbQ) for the next GA release based on the [Simultaneous Release schedule](https://wiki.eclipse.org/Simultaneous_Release).  
    Each stream has its own [wiki](https://wiki.eclipse.org/Category:SimRel-2022-06) page with a more detailed schedule. 
  * Create an [issue](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/287) to track the creation of new [Performance Test](https://ci.eclipse.org/releng/view/Performance%20Tests/) and [Automated Test](https://ci.eclipse.org/releng/view/Automated%20tests/) jobs in the Releng jenkins. 
  * Create an [issue](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/290) and run the [newStreamRepos](https://ci.eclipse.org/releng/job/eclipse.releng.newStreamRepos/) job to make an I-builds repo for the next release. Once the repos are made create new I-build (and Y or P-builds as necessary) for the next release. 
  * [Update cleanup scripts](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/305) to include Y and P-builds if those were added, or take them out if not.
  * [Cleanup approved API list](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/294).
  * [Clean forceQualifierUpdate files for doc bundles](https://github.com/eclipse-platform/eclipse.platform.common/issues/32). The context here is that the doc builds only check for changes in this repo and so these files need to be changed to trigger a full rebuild.
  - Update splash screen.  
    Splash screens are created 4 at a time, for 4 consequtive quarterly releases, so they only need to be requested once a year before the 20XX-06 release (the cycle is 2022-06 -> 2023-03, etc). Create an issue in the [Eclipse Help Desk](https://gitlab.eclipse.org/eclipsefdn/helpdesk/-/issues) similar to [Bug 575781](https://bugs.eclipse.org/bugs/show_bug.cgi?id=575781). It is customary to do this by the previous -09 (September) release so that there's plenty of time for discussion before the -06 (June) release is opened. 
  * **Version Updates**  
    These updates are currently broken into multiple github issues, but the changes can be made at once and merged in a single commit. 
    - [Set the previous version to RC2](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/302).
    - Update versions to the next release across product files and build scripts:  
      [POM and product version changes](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/290)  
      [Update product version number across build scripts](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/291).  
      This is a large task so it's better to get started as soon as possible after the RC2 release, though it won't be merged until there's a final GA candidate. Most of the version updates can be done by updating [updateProductVersion.sh](scripts/updateProductVersion.sh) and running it in `eclipse.platform.releng.aggregator`. Clone recursively to get each submodule: 
      ```
      git clone -b master --recursive git clone -b master --recursive git@github.com:eclipse-platform/eclipse.platform.releng.aggregator.git
      ```
      then run `updateProductVersion.sh`, then commit the changes in a new branch for each repo. Once that's done it's easiest to just grep for the previous release version or stream number to find the remaining instances that need to be updated.   
      The repos that need to be updated (excluding the ones included in `eclipse.platform.releng.aggregator`) are:
        * [eclipse-platform/eclipse.platform.releng.aggregator](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator)
        * [eclipse-platform/eclipse.platform.runtime](https://github.com/eclipse-platform/eclipse.platform.runtime)
        * [eclipse-equinox/p2](https://github.com/eclipse-equinox/p2)
        * [eclipse-equinox/equinox.framework](https://github.com/eclipse-equinox/equinox.framework)
        * [eclipse-equinox/equinox.bundles](https://github.com/eclipse-equinox/equinox.bundles)
    - [Disable the freeze report](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/300) for the next stream.
    - [Update the checkComposities script](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/299).
    - [Update comparator repo and eclipse run repo](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/298).
    - [Update version number in mac's Eclipse.app](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/303).
      
**RC2a Release**
  * Sometimes there is a critical issue that requires a fix, if it's decided that one is needed then an RC2a (followed by RC2b, RC2c etc if necessary) is built from the maintenance branch and promoted using the RC2 process.
  * Create an issue to set the previous release version to RC2a and add it to the Preparation issue for the next version, then update all references to RC2 to the RC2a release.

**Friday before GA Release**
  * After Simrel declares RC2 run the [rename and promote](https://ci.eclipse.org/releng/job/eclipse.releng.renameAndPromote/) job to promote RC2 (or RC2a). Change the DL_TYPE from S to R.  
    You can subscribe to [cross-project-issues](https://accounts.eclipse.org/mailing-list/cross-project-issues-dev) to get the notifications on Simrel releases.
  * Once you have the release url for the GA release you can complete the [Publish to Maven Central](https://github.com/eclipse-platform/eclipse.platform.releng/issues/45) and [Update maintenance branch with release version](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/334) tasks.
  * [Set the previous release to the GA release across build scripts](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/284).
  * Update and contribute to Simrel.
   
**Publishing to Maven**

  * Publishing to maven should happen by at least Tuesday before the release since there is up to a 24 hour delay for the maven mirrors.
  * Update [SDK4Mvn.aggr](https://github.com/eclipse-platform/eclipse.platform.releng/blob/master/publish-to-maven-central/SDK4Mvn.aggr) and [properties.sh](https://github.com/eclipse-platform/eclipse.platform.releng/blob/master/publish-to-maven-central/properties.sh) to the release build, and update EMF and Orbit urls.
  * Run the [CBI aggregator](https://ci.eclipse.org/releng/view/Publish%20to%20Maven%20Central/job/CBIaggregator/) job in jenkins.
    - Sometimes there are new source bundles that need to be added/generated, add these to [sourceBundles.txt](https://github.com/eclipse-platform/eclipse.platform.releng/blob/master/publish-to-maven-central/sourceBundles.txt)
  * Run [Publish Platform to Maven](https://ci.eclipse.org/releng/view/Publish%20to%20Maven%20Central/job/PublishPlatformToMaven/), [Publish JDT to Maven](https://ci.eclipse.org/releng/view/Publish%20to%20Maven%20Central/job/PublishJDTtoMaven/) and [Publish PDE to Maven](https://ci.eclipse.org/releng/view/Publish%20to%20Maven%20Central/job/PublishPDEToMaven/) in parallel, using the CBI aggregator build number for the argument.
  * If you do not have an account on oss.sonatype.org for performing the rest of the release request one by creating an issue like https://issues.sonatype.org/browse/OSSRH-43870 to get permissions for platform, JDT and PDE projects and tag an existing release engineer to give approval.
  * Log into https://oss.sonatype.org/#stagingRepositories and close the Platform, JDT and PDE repositories, then select each and click release to release them.
  * Replace contents of [baseline.txt](https://github.com/eclipse-platform/eclipse.platform.releng/blob/master/publish-to-maven-central/baseline.txt) with the contents of baseline-next.txt created in CBI aggregator.

**Wednesday, GA Release**
  * The release is scheduled for 10AM EST 
  * The [Tag Eclipse Release](https://ci.eclipse.org/releng/job/TagEclipseRelease) needs to be run before then, so it's usually scheduled to run at about 8AM EST. This will complete the [Tag GA release](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/343) task.
  * For the [Clean up intermediate artifacts](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/348) task, update the [cleanup release artifacts](https://ci.eclipse.org/releng/view/Cleanup/job/cleanup_release_artifacts/) job and schedule it to run at 8AM on Wednesday. Use the [list artifacts](https://ci.eclipse.org/releng/view/Cleanup/job/list_artifacts_from_download_server/) job to generate the list used by the cleanup job.
  * At around 9:30 EST run (or have scheduled) [ep_createGenericComposites](https://ci.eclipse.org/releng/job/ep_createGenericComposites/) to [update the generic repos](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/319) for this release.  
    For reference, the generic repositories are for the [latest GA release](https://download.eclipse.org/eclipse/updates/latest/) and the current (ongoing) [I-builds](https://download.eclipse.org/eclipse/updates/I-builds/), [Y-builds](https://download.eclipse.org/eclipse/updates/Y-builds/) and [P-builds](https://download.eclipse.org/eclipse/updates/P-builds/). 
  * Schedule the [make visible](https://ci.eclipse.org/releng/job/eclipse.releng.stage2DeferredMakeVisible/) job for about 9:45AM EST.
  * Once Simrel announces the GA release send the announcement email.
