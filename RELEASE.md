# Releng-Tasks 2.0

[Eclipse Major Release Schedule](https://github.com/eclipse-simrel/.github/blob/main/wiki/Simultaneous_Release.md)

## Milestone and RC Releases

### Friday before release week:
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
         - `DROP_ID`: ID of the build to promote, e.g.: `I20250817-1800`
         - `CHECKPOINT`: M1 etc. (blank for final releases)
         - `SIGNOFF_BUG`: Needs to be updated to sign-off issue (numeric part only)
         - For Milestones/RC promotions, this should automatically run the [Publish Promoted Build](https://ci.eclipse.org/releng/job/Releng/job/publishPromotedBuild/) job to make the promoted build immediatly visible on the download page.
       * Contribute to SimRel
         - If you have not already set up SimRel you can do so using Auto Launch [here](https://www.eclipse.org/setups/installer/?url=https://git.eclipse.org/c/oomph/org.eclipse.oomph.git/plain/setups/interim/SimultaneousReleaseTrainConfiguration.setup&show=true)
         - Clone [simrel.build](https://github.com/eclipse-simrel/simrel.build.git) (Should have been done by the installer during set up, but make sure you have latest).
           1. Open simrel.aggr in Eclipse
           2. Change to the properties view
           3. Select `Contribution:Eclipse` > `Mapped Repository`
           4. Update the Location property to the "Specific repository for building against" in the `mailtemplate.txt` from promotion.
           5. Commit Simrel updates to GitHub
              - Message should use year-month format, i.e "Simrel updates for Eclipse and Equinox for 2022-06 M1"
     * **After RC1**
       * Comment on EMF, ECF and Orbit issues to ask for final release builds.

## GA Releases 
Tasks to be completed after RC2

### **Release Preparation**: 
Tasks that need to be completed before Friday

  * Create an issue to track the current release tasks (see [Release 4.24](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/273)).
    - Tag @SarikaSinha (Readme), @ktatavarthi (JDT and Platform Migration Guides), @niraj-modi (SWT Javadoc bash).
    - Update the Acknowledgements.
    - A script to create this issue exists [here](scripts/GAReleasePrep.sh) for those who have the hub cli tool installed.
  * **Readme**
    Currently handled by @SarikaSinha
    - Create a tracking issue in [www.eclipse.org-eclipse](https://github.com/eclipse-platform/www.eclipse.org-eclipse) (see [Readme file for 4.26](https://github.com/eclipse-platform/www.eclipse.org-eclipse/issues/24) as an example).
    - Add Readme files and update generatation scripts.
  * **Acknowledgements**
    - The RelEng pipeline automatically runs the [Generate Acknowledgements](https://github.com/eclipse-platform/www.eclipse.org-eclipse/actions/workflows/generateAcknowledgements.yml) GitHub workflow
      which creates a pull request in the [www.eclipse.org-eclipse repository](https://github.com/eclipse-platform/www.eclipse.org-eclipse/pulls)
    - This pull request should be reviewed to ensure all bot accounts are excluded and that conflicting author names are properly resolved.
  * **Migration Guide**
    - Create a tracking issue in [eclipse.platform.releng.aggregator](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues) and link it to the main release issue.
    - Every release a new porting guide and folder need to be added to [eclipse.platform.common/bundles/org.eclipse.jdt.doc.isv/porting](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/tree/master/eclipse.platform.common/bundles/org.eclipse.jdt.doc.isv/porting), named with the version being migrated *to*.
      - i.e `eclipse_4_27_porting_guide.html` is for migrating from 4.26 tp 4.27.
    - Update `topics_Porting.xml` in [eclipse.platform.common/bundles/org.eclipse.jdt.doc.isv](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/tree/master/eclipse.platform.common/bundles/org.eclipse.jdt.doc.isv) and [eclipse.platform.common/bundles/org.eclipse.platform.doc.isv](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/tree/master/eclipse.platform.common/bundles/org.eclipse.platform.doc.isv)
    - Update the name of the proting html document in [eclipse.platform/platform/org.eclipse.platform/intro/migrateExtensionContent.xml](https://github.com/eclipse-platform/eclipse.platform/blob/master/platform/org.eclipse.platform/intro/migrateExtensionContent.xml) 
  * **SWT Javadoc bash**
    Currently handled by @niraj-modi 
    - Create a tracking issue in [eclipse.platform.swt](https://github.com/eclipse-platform/eclipse.platform.swt).
    - The javadoc bash tool needs to be run on SWT sources to make it consistent.

### **Release**: 
The actual steps to release

**Friday**
  * #### **Promote to GA**
    - After Simrel declares RC2 (usually the Friday before release) run the [Promote Build](https://ci.eclipse.org/releng/job/Releng/job/promoteBuild/) job to promote RC2 (or RC2a).
      - `DROP_ID`: Final delease candidate's ID, e.g.: `S-4.36RC2-202505281830/`
      - This will create pull requests to update the build configuration on the master and corresponding maintenance branch to the promoted release.
        - Only submit them AFTER the release was finally published.
    - You can subscribe to [cross-project-issues](https://accounts.eclipse.org/mailing-list/cross-project-issues-dev) to get the notifications on Simrel releases.
  * **Contribute to SimRel**
    - If SimRel is not updated before the I-builds are cleaned up (specifically the build for RC2/GA) it will break. 

**Wednesday**  
The release is scheduled for 10AM EST. Typically the jobs are scheduled beforehand and run automatically.

  * **Make the Release Visible**
    - Run the [Publish Promoted Build](https://ci.eclipse.org/releng/job/Releng/job/publishPromotedBuild/) job in Releng jenkins to make the promoted build visible on the download page.
      - `releaseBuildID`: the full id of the release build to publish, e.g. `R-4.36-202505281830`
  * **Complete Publication to Maven Central**
    - The final release to Maven-Central should happen latest by Tuesday before the release since there is up to a 24 hour delay for the Maven mirrors.
    - The artifacts have been deployed into a Maven-Central _staging_ repository by the `Promote Build` job when the RC was promoted to GA release.
    - Login to https://central.sonatype.com/ and "Release" the three staging repositories for `Platform`, `JDT` and `PDE` by closing them.
      - Make sure the name of the deployment you are about to release matches the tag and timestamp of the final release repository.
        E.g for a P2 repo with id `R-4.36-202505281830` the deploymenets should be named `PLATFORM_R-4.36-202505281830`, `PDE_R-4.36-202505281830` or `JDT_R-4.36-202505281830` respectivly.
      - If you do not have an account on `central.sonatype.com` for performing the rest of the release request one by creating an [EF Help Desk](https://gitlab.eclipse.org/eclipsefdn/helpdesk/-/issues) issue to get permissions for platform, JDT and PDE projects and tag an existing release engineer to give approval.

### **Preparation for the next Release**
  After RC2 create an issue to track preparation work for the next stream (see [Preparation work for 4.25 (2022-09)](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/284)).
  - A script to create this issue exists [here](scripts/newReleasePrep.sh) for those who have the hub cli tool installed. The process has been in flux recently so please update the script if necessary, but it provides a helpful template since most tasks in the previous release's issue become links.

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
  - Run the [Create Jobs](https://ci.eclipse.org/releng/job/CreateJobs/) job in Jenkins.
    But until the preparation work has completed, the new I-builds should not be triggered in order to avoid undesired interference. To ensure this, either
    - Run the `Create Jobs` job only after all preparation work is reviewed and submitted and the first I-build can run.
    - Disable the new `I-build` job until it's ready to run it the first time.
      In order to investigate the state of the I-build it's probably also good to disable the job again after the first I-build has completed, until the master is open for regular development.
  - Move the previous (still existing) I-Build job to run on the maintenance branch (and remove it's cron trigger). That job can be deleted (together with its test jobs), when we are sure RC respins won't happen anymore.

#### **Create Git Milestones for the next Release:**

Milestones are already created by running [`Prepare Next Development Cycle`](https://ci.eclipse.org/releng/job/Releng/job/prepareNextDevCycle/) job.
Previously they were created in ther own job:
  - Milestones in git are created by running the create-milestones job in jenkins, usually after RC1 or RC2. Only specific users can access this job for security reasons. If milestones need to be created and have not please contact @sdawley @sravanlakkimsetti or @laeubi to run it.

#### **Version Updates:**
  - Running the [`Prepare Next Development Cycle`](https://ci.eclipse.org/releng/job/Releng/job/prepareNextDevCycle/) job will update pom and product versions for the Eclipse repositories and submit pull requests for the changes.  
  This is still a work in progress so if there are any issues or a repo gets missed you can fall back to the old process below:   
  Once that's done it's easiest to just grep for the previous release version or stream number to find the remaining instances that need to be updated, then commit the changes in a new branch for each repo.   
  - **Update comparator repo and eclipse run repo**
    - Update the ECLIPSE_RUN_REPO in the [cje-production](cje-production) buildproperties.txt files
  - **Set Previous Version to RC2** 
    - RC2 becomes the new baseline for the week before the GA release.
    - Update previous-release.baseline in [eclipse-platform-parent/pom.xml](eclipse-platform-parent/pom.xml)

**RC2a Release**
  * Sometimes there is a critical issue that requires a fix, if it's decided that one is needed then an RC2a (followed by RC2b, RC2c etc if necessary) is built from the maintenance branch and promoted using the RC2 process.
  * Create an issue to set the previous release version to RC2a and add it to the Preparation issue for the next version, then update all references to RC2 to the RC2a release.


   

