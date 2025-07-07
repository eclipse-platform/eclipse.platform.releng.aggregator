# Releng-Tasks 2.0

[Eclipse Major Release Schedule](https://github.com/eclipse-simrel/.github/blob/main/wiki/Simultaneous_Release.md)

## Milestone and RC Releases

### Friday before release week:
 * Update [I-builds job definition](JenkinsJobs/Builds/FOLDER.groovy) to build on the milestone schedule (Twice daily at 06:00 EST and 18:00 EST except Thursday).
 * Create or update prerequisite issues for tracking ECF, EMF and Orbit
 * Send reminder email for upcoming RC week to platform-releng-dev@eclipse.org, platform-dev@eclipse.org, eclipse-dev@eclipse.org and equinox-dev@eclipse.org
   * [Example from 4.30 RC1](https://www.eclipse.org/lists/platform-dev/msg03924.html) but the usual schedule:
     * Wednesday: 
       - Release Candidate is built Wednesday evening at 6PM EST. 
     * Thursday: Sign-Off
     * Friday: 
       - Build delcared and released.
       - Make sure to mention that the Master branch will stay closed until RC is officially released.

### Milestone/RC Week
   - **M 1/2/3 Release**
     * All milestone releases are 'lightweight', meaning there is no announcement or signoff.
     No additional builds need to be run, just the daily I-build at 6PM EST.
     Thursdays build is promoted to simrel on friday (unless there are problems with Thursdays build, in which case promote Wednesdays) and the compiler is updated if necessary,
     but the `Promote Build` and `Publish Promoted Build` jobs don't need to be run.
   - **Wednesday**:
     * Verify that EMF, ECF and Orbit contributions have been included (if applicable).
     * Final release candidate build runs at 6PM EST.
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
       * Run the [Promote Build](https://ci.eclipse.org/releng/job/Releng/job/promoteBuild/) job in Jenkins
         - DROP_ID: Release candidate build ID (make sure there is no space before or after the ID).
         - CHECKPOINT: M1 etc (blank for final releases)
         - SIGNOFF_BUG: Needs to be updated to sign-off issue (numeric part only)
         - TRAIN_NAME: Whenever the current GA release is planned for (formatted 4 digit year - 2 digit month, i.e `2022-06`)
         - STREAM: 4.24.0 etc
         - DL_TYPE: S is used to promote I-builds.
         - TAG: Parameter should match stream version, i.e `S4_30_0_RC1` etc
         - After the build  find and open the mail template [artifact](https://ci.eclipse.org/releng/job/Releng/job/promoteBuild/lastSuccessfulBuild/artifact/) and have it ready.
         - This should automatically run [tag Eclipse release](https://ci.eclipse.org/releng/job/Releng/job/tagEclipseRelease/) to tag the source code.
       * Contribute to SimRel
         - If you have not already set up SimRel you can do so using Auto Launch [here](https://www.eclipse.org/setups/installer/?url=https://git.eclipse.org/c/oomph/org.eclipse.oomph.git/plain/setups/interim/SimultaneousReleaseTrainConfiguration.setup&show=true)
         - Clone [org.eclipse.simrel.build](https://git.eclipse.org/c/simrel/org.eclipse.simrel.build.git) (Should have been done by the installer during set up, but make sure you have latest).
           1. Open simrel.aggr in Eclipse
           2. Change to the properties view
           3. Select Contribution:Eclipse > Mapped Repository
           4. Update the Location property to the "Specific repository for building against" in the mailtemplate.txt from promotion.
           5. Commit Simrel updates to Gerrit
              - Message should use year-month format, i.e "Simrel updates for Eclipse and Equinox for 2022-06 M1"
       * Run the [Publish Promoted Build](https://ci.eclipse.org/releng/job/Releng/job/publishPromotedBuild/) job in Releng jenkins to make the promoted build visible on the download page.
         - `releaseBuildID`: the full id of the milestone, release-candidate or release build to publish, e.g. `S-4.26M1-202209281800` or `R-4.36-202505281830`
       * Send email that the M1 build is available
         - Use the mail template from the promotion build [artifacts](https://ci.eclipse.org/releng/job/Releng/job/promoteBuild/lastSuccessfulBuild/artifact/) in Jenkins to get the download urls.
         - Make sure to mention that the Master branch is now again open for development.
       * For **Milestone builds** return the I-builds to the normal schedule.
     * **After RC1**
       * Leave the I-builds running on the milestone schedule for RC2. 
       * Comment on EMF, ECF and Orbit issues to ask for final release builds.
     * **After RC2**
       * (optional) Disable the automatic [nightly cleanup](https://ci.eclipse.org/releng/job/Cleanup/job/dailyCleanOldBuilds/) of I-builds

## GA Releases 
Tasks to be completed after RC2

### **Release Preparation**: 
Tasks that need to be completed before Friday

  * Create an issue to track the current release tasks (see [Release 4.24](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/273)).
    - Tag @lshanmug (New & Noteworthy), @SarikaSinha (Readme), @ktatavarthi (JDT and Platform Migration Guides), @niraj-modi (SWT Javadoc bash).
    - Update the Acknowledgements.
    - A script to create this issue exists [here](scripts/GAReleasePrep.sh) for those who have the hub cli tool installed.
  * **New & Noteworthy**
    Currently this is handled by @lshanmug who will
    - Create a tracking issue in the [eclipse.platform.common](https://github.com/eclipse-platform/eclipse.platform.common) repo (see [N&N for 4.26](https://github.com/eclipse-platform/eclipse.platform.common/pull/93) as an example).
    - Update the WhatsNew files and folders for the doc bundles.
  * **Readme**
    Currently handled by @SarikaSinha
    - Create a tracking issue in [www.eclipse.org-eclipse](https://github.com/eclipse-platform/www.eclipse.org-eclipse) (see [Readme file for 4.26](https://github.com/eclipse-platform/www.eclipse.org-eclipse/issues/24) as an example).
    - Add Readme files and update generatation scripts.
  * **Acknowledgements**
    - Create a tracking issue in [www.eclipse.org-eclipse](https://github.com/eclipse-platform/www.eclipse.org-eclipse) and link it to the main release issue in eclipse.platform.releng.aggregator.
    - Create a new acknowledgements file for the current release and add it to [www.eclipse.org-eclipse/development](https://github.com/eclipse-platform/www.eclipse.org-eclipse/tree/master/development).
    - The previous acknowledgement files are there for reference.
  * **Migration Guide**
    - Create a tracking issue in [eclipse.platform.common](https://github.com/eclipse-platform/eclipse.platform.common) and link it to the main release issue in eclipse.platform.releng.aggregator.
    - Every release a new porting guide and folder need to be added to [eclipse.platform.common/bundles/org.eclipse.jdt.doc.isv/porting](https://github.com/eclipse-platform/eclipse.platform.common/tree/master/bundles/org.eclipse.jdt.doc.isv/porting), named with the version being migrated *to*.
      - i.e `eclipse_4_27_porting_guide.html` is for migrating from 4.26 tp 4.27.
    - Update topics_Porting.xml in [eclipse.platform.common/bundles/org.eclipse.jdt.doc.isv](https://github.com/eclipse-platform/eclipse.platform.common/tree/master/bundles/org.eclipse.jdt.doc.isv) and [eclipse.platform.common/bundles/org.eclipse.platform.doc.isv](https://github.com/eclipse-platform/eclipse.platform.common/tree/master/bundles/org.eclipse.platform.doc.isv)
    - Update the name of the proting html document in [eclipse.platform/platform/org.eclipse.platform/intro/migrateExtensionContent.xml](https://github.com/eclipse-platform/eclipse.platform/blob/master/platform/org.eclipse.platform/intro/migrateExtensionContent.xml) 
  * **SWT Javadoc bash**
    Currently handled by @niraj-modi 
    - Create a tracking issue in [eclipse.platform.swt](https://github.com/eclipse-platform/eclipse.platform.swt).
    - The javadoc bash tool needs to be run on SWT sources to make it consistent.

### **Release**: 
The actual steps to release

**Friday**
  * #### **Promote to GA**
    - After Simrel declares RC2 (usually the Friday before release) run the [Promote Build](https://ci.eclipse.org/releng/job/Releng/job/promoteBuild/) job to promote RC2 (or RC2a). If the [daily cleanup for old builds](https://ci.eclipse.org/releng/job/Cleanup/job/dailyCleanOldBuilds/) job was not disabled and the original I-build is no longer available you can use the promoted RC2 build.
      - Change the DL_TYPE from S to R.  
      - TAG will be set to R as well, for example `R4_27` 
    - You can subscribe to [cross-project-issues](https://accounts.eclipse.org/mailing-list/cross-project-issues-dev) to get the notifications on Simrel releases.
  * #### **Deploy to Maven central**
    - Deploying to maven should happen by at least Tuesday before the release since there is up to a 24 hour delay for the maven mirrors.
    - Run the [Deploy to Maven](https://ci.eclipse.org/releng/job/Releng/job/deployToMaven/) job in Jenkins with the release build as `sourceRepository`.
      - About a minute after triggering the job, Jenkins will ask for confirmation on the console, if the specified build should really be deployed to Maven-Central staging.
    - Once that deploy-job has completed successfully, log into https://central.sonatype.com/ and close the `Platform`, `JDT` and `PDE` repositories.
      - If you do not have an account on `central.sonatype.com` for performing the rest of the release request one by creating an [EF Help Desk](https://gitlab.eclipse.org/eclipsefdn/helpdesk/-/issues) issue to get permissions for platform, JDT and PDE projects and tag an existing release engineer to give approval.
  * **Contribute to SimRel**
    - If SimRel is not updated before the I-builds are cleaned up (specifically the build for RC2/GA) it will break. 

**Wednesday**  
The release is scheduled for 10AM EST. Typically the jobs are scheduled beforehand and run automatically.

  * **Make the Release Visible**
    - Same process as for a milestone but with release versions.
  * **Complete Publication to Maven Central**
    - Go to https://oss.sonatype.org/#stagingRepositories and "Release" the three staging repositories.
  * **Send the Announcement Email**

### **Post Release Tasks:**
  * #### **Clean up intermediate artifacts** 
    - To clean up specific artifacts from the old stream (milestones, I-builds and old releases) run the [Cleanup Release Artifacts](https://ci.eclipse.org/releng/job/Releng/job/cleanupReleaseArtifacts/) job. 
    - `release_to_clean` is the release that was just published.
    - `release_build` is the I-build that was promoted, this is used as a landmark to the build will clear out all previous I-builds.
    - `release_to_remove` only the last 3 major releases are kept on the download page, so if 4.25 was promoted then remove 4.22.
    - For the Y and P build parameters it's important to know whether or not Y and P builds were run during the release. Since they correspond to java releases on a 6 month cycle, typically they are built in odd-numbered releases.  
    The existing builds are kept for one release, then cleaned up before the next stream that will have Y and P builds. it's convoluted and I dont want to type it out. Remove Y builds on even releases. 
    - If something doesn't get cleaned up properly you can use  Use the [list artifacts](https://ci.eclipse.org/releng/view/Cleanup/job/list_artifacts_from_download_server/) job to generate ta list of what's on the download server and either create a new job to clean it up or update and rerun the cleanup job as appropriate.
  * **Set Previous Release to GA** 
    - Everything that was updated to RC2 (see below) should now use the released build.

### **Preparation for the next Release**
  After RC2 create an issue to track preparation work for the next stream (see [Preparation work for 4.25 (2022-09)](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/284)).
  - A script to create this issue exists [here](scripts/newReleasePrep.sh) for those who have the hub cli tool installed. The process has been in flux recently so please update the script if necessary, but it provides a helpful template since most tasks in the previous release's issue become links.

#### **Maintenance Branches:**
  * **Maintenance Branch Creation:**
    - Create the branch from RC2 using the [create maintenance branch](https://ci.eclipse.org/releng/job/Releng/job/createMaintenanceBranch/) job in the Eclipse Platform Releng Jenkins.
  * **Update maintenance branch with release version**
    - Once the I-build repo is removed for the previous release the maintenance branch will have to use the release location, i.e. any references to `https://download.eclipse.org/eclipse/updates/4.25-I-builds/` will need to be updated to `https://download.eclipse.org/eclipse/updates/4.26/R-4.26-202211231800/`
    - Functionally this means:
      - Update the ECLIPSE_RUN_REPO in the [cje-production](cje-production) buildproperties.txt files
      - Update `eclipse-sdk-repo` in [eclipse-platform-parent/pom.xml](eclipse-platform-parent/pom.xml)
    - This step can be prepared ahead of time but can't be merged until the release build has been promoted and the update site exists.
     * **Update ECJ compiler** in the platform build (if it needs to be updated).
       * To find the new *unqualified* compiler version:
         - Go to the update site for the release candidate
         - Click `plugins`
         - Find the *unqualified* version of the artifact `org.eclipse.jdt.core.complier.batch_${ecjversion}.jar`, i.e. the version consisting only of the _major_._minor_._service_ part, but without qualifier.
         It's the version of the `org.eclipse.jdt:ecj` artifact at Maven-Central, about to be relased.
       * Update the `cbi-ecj-version` in [eclipse.platform.releng.aggregator/eclipse-platform-parent/pom.xml](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/blob/master/eclipse-platform-parent/pom.xml)
       to the exact version of the to be released`org.eclipse.jdt.core.complier.batch` bundle, e.g.: `3.40.0`

#### **Update the Build Calendar:**
  - Create an [issue](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/289) and update the [build calendar](https://calendar.google.com/calendar/u/0?cid=cHJmazI2ZmRtcHJ1MW1wdGxiMDZwMGpoNHNAZ3JvdXAuY2FsZW5kYXIuZ29vZ2xlLmNvbQ) for the next GA release based on the [Simultaneous Release schedule](https://wiki.eclipse.org/Simultaneous_Release).
  - Each stream has its own [wiki](https://wiki.eclipse.org/Category:SimRel-2022-06) page with a more detailed schedule.
  - List of people who can edit the calendar
    - Alexander Kurtakov(@akurtakov)
    - David Williams
    - Gireesh Punathil
    - Rahul Mohanan
    - Samantha Dawley
    - Sravan Kumar Lakkimsetti

#### **Update Splash Screen:**
  - Create a tracking issue in [eclipse.platform](https://github.com/eclipse-platform/eclipse.platform) and link it to the main issue in eclipse.platform.releng.aggregator.
  - Future spash screens are kept in a subfolder of [eclipse.platform/platform/org.eclipse.platform](https://github.com/eclipse-platform/eclipse.platform/tree/master/platform/org.eclipse.platform), usually named something like 'splashscreens2022' (or the current year).
  - Find the appropriate splash screen, copy it one level up and rename it splash.png, replacing the existing png.
  - NOTE: Splash screens are created 4 at a time, for 4 consequtive quarterly releases, so they need to be requested once a year before the 20XX-06 release (the cycle is 2022-06 -> 2023-03, etc). Create an issue in the [Eclipse Help Desk](https://gitlab.eclipse.org/eclipsefdn/helpdesk/-/issues) similar to [Bug 575781](https://bugs.eclipse.org/bugs/show_bug.cgi?id=575781). It is customary to do this by the previous -09 (September) release so that there's plenty of time for discussion before the -06 (June) release is opened. 
  - Issue for the 2023 releases is [https://gitlab.eclipse.org/eclipsefdn/helpdesk/-/issues/2336](https://gitlab.eclipse.org/eclipsefdn/helpdesk/-/issues/2336)
  
#### **Update Jenkins for the next Release:**
  - Edit the [JobDSL.json](JenkinsJobs/JobDSL.json)
      * Add the next release version to the `Streams` key item.
      * In the `branches` item update the current release to map to the maintenance branch and add a new key:value pair mapping the next release to master.
  - Run the [Create Jobs](https://ci.eclipse.org/releng/job/Create%20Jobs/) job in Jenkins.  
    This should move the current I-builds to run on the maintenance branch and create new I-builds for the next release.
    Performance and Unit tests should also be generated for the new release automatically.

#### **Create Git Milestones for the next Release:**

Milestones are already created by running [`Prepare Next Development Cycle`](https://ci.eclipse.org/releng/job/Releng/job/prepareNextDevCycle/) job.
Previously they were created in ther own job:
  - Milestones in git are created by running the create-milestones job in jenkins, usually after RC1 or RC2. Only specific users can access this job for security reasons. If milestones need to be created and have not please contact @sdawley @sravanlakkimsetti or @laeubi to run it.

#### **Version Updates:**
  - Running the [`Prepare Next Development Cycle`](https://ci.eclipse.org/releng/job/Releng/job/prepareNextDevCycle/) job will update pom and product versions for the Eclipse repositories and submit pull requests for the changes.  
  This is still a work in progress so if there are any issues or a repo gets missed you can fall back to the old process below:   
  If you cloned eclipse.platform.releng.aggregator's submodules you can fix the set version and run [updateProductVersion.sh](scripts/updateProductVersion.sh) to update most of the versions.  
  Once that's done it's easiest to just grep for the previous release version or stream number to find the remaining instances that need to be updated, then commit the changes in a new branch for each repo.   
  - **Update version number in mac's Eclipse.app**
    - In [eclipse-equinox/equinox](https://github.com/eclipse-equinox/equinox) update the versions in the Info.plist for both architectures under `eclipse-equinox/equinox/features/org.eclipse.equinox.executable.feature/bin/cocoa/macosx`
  - **Update comparator repo and eclipse run repo**
    - Update the ECLIPSE_RUN_REPO in the [cje-production](cje-production) buildproperties.txt files
  - **Set Previous Version to RC2** 
    - RC2 becomes the new baseline for the week before the GA release.
    - Update previous-release.baseline in [eclipse-platform-parent/pom.xml](eclipse-platform-parent/pom.xml)
    - Update the last release build versions in [eclipse.platform.releng.tychoeclipsebuilder/eclipse-junit-tests/src/main/resources/equinoxp2tests.properties](eclipse.platform.releng.tychoeclipsebuilder/eclipse-junit-tests/src/main/resources/equinoxp2tests.properties)
    - Update the previousReleaseVersion in [eclipse.platform.releng.tychoeclipsebuilder/eclipse-junit-tests/src/main/resources/label.properties](eclipse.platform.releng.tychoeclipsebuilder/eclipse-junit-tests/src/main/resources/label.properties)
    - Update the name of the copied files in [eclipse.platform.releng.tychoeclipsebuilder/eclipse-junit-tests/src/main/scripts/getPreviousRelease.sh](eclipse.platform.releng.tychoeclipsebuilder/eclipse-junit-tests/src/main/scripts/getPreviousRelease.sh)
**General Cleanup**
  - In [eclipse.platform.common] search for and clear out all of the forceQualifierUpdate.txt files.  
    The context here is that the doc builds only check for changes in this repo and so these files need to be changed to trigger a full rebuild.
* #### **Create Generic Composites**
   - After First Stable Ibuild move Generic repos to next stream.
   - Run the [Create Generic Composites](https://ci.eclipse.org/releng/job/Releng/job/createGenericComposites/) job to recreate the generic build repos for the next release. 
      - `currentStream`: To clarify this is the next stream, not the one currently being released. If you are releasing 4.32, the 'current' stream is 4.33 so that repos are created for it.
      - `previousStream`: The stream being released, which needs to be removed.  
      - For reference, the generic repositories created are for the [latest GA release](https://download.eclipse.org/eclipse/updates/latest/) and the current (ongoing) [I-builds](https://download.eclipse.org/eclipse/updates/I-builds/), [Y-builds](https://download.eclipse.org/eclipse/updates/Y-builds/) and [P-builds](https://download.eclipse.org/eclipse/updates/P-builds/). 

**RC2a Release**
  * Sometimes there is a critical issue that requires a fix, if it's decided that one is needed then an RC2a (followed by RC2b, RC2c etc if necessary) is built from the maintenance branch and promoted using the RC2 process.
  * Create an issue to set the previous release version to RC2a and add it to the Preparation issue for the next version, then update all references to RC2 to the RC2a release.


   

