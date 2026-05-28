# Releng-Tasks 2.1

[Eclipse Simultaneous Release Schedule](https://github.com/eclipse-simrel/.github/blob/main/wiki/Simultaneous_Release.md)

## Milestone and RC Releases

### Friday before release week:
 * Create or update prerequisite issues for tracking ECF, EMF and Orbit
- Send reminder email for upcoming RC week
  - Run the [`Send Announcement`](https://ci.eclipse.org/releng/job/Releng/job/sendAnnouncement/) job
    - `type`: `REMINDER_RC`
    - `rcNumber`: number of current RC, `1` or `2`
### Milestone/RC Week
   - **M 1/2/3 Release**
     * All milestone releases are 'lightweight', meaning there is no announcement or signoff.
     No additional builds need to be run, just the daily I-build at 6PM EST.
     Thursdays build is promoted to SimRel on Friday (unless there are problems with Thursdays build, in which case promote Wednesdays or a later build).
   - **Wednesday**:
     * Verify that EMF, ECF and Orbit contributions have been included (if applicable).
     - Ensure the build is cleared of comparator errors in doc bundles.
       - If `buildlogs/comparatorlogs/buildtimeComparatorDocBundle.log.txt` of the latest build is not empty, enforce a qualifier update on the affected doc-bundles
     * Final release candidate build runs at 6PM EST.
     * Because of time zones, PST/EST might want to do Thursday's tasks EOD Wednesday after the release candidate has built. 
   - **Thursday**:
     - Send request to sign-off the current release candidate build
       - Run the [`Send Announcement`](https://ci.eclipse.org/releng/job/Releng/job/sendAnnouncement/) job
         - `type`: `SIGNOFF_RC`
         - `rcNumber`: number of current RC, e.g. `1`, `2`, `2a`, ...
         - `buildId`: ID of the current release candidate I-build, e.g. `I20260527-1800`
   - **Friday**:
     * **Promote** the release candidate (if go).
       - Run the [`Promote Build`](https://ci.eclipse.org/releng/job/Releng/job/promoteBuild/) job
         - `DROP_ID`: ID of the build to promote, e.g.: `I20250817-1800`
         - `CHECKPOINT`: M1 etc. (blank for final releases)
         - `SIGNOFF_ISSUE`: Only relevant for RCs, the issue on which sign-off by component leads was requested (numeric part only)
       - For Milestone/RC promotions, this automatically runs the [Publish Promoted Build](https://ci.eclipse.org/releng/job/Releng/job/publishPromotedBuild/) job to make the promoted build immediatly visible on the download page.
         - For release promotions that run has to be started manually at the time of the release
       - It also triggers the [Generate Acknowledgements](https://github.com/eclipse-platform/www.eclipse.org-eclipse/actions/workflows/generateAcknowledgements.yml) workflow.
         - Review and submit the PR created by it and pay special attention about name conflicts mentioned in the PR message and make sure they are resolved:
           https://github.com/eclipse-platform/www.eclipse.org-eclipse/pulls
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

### Release Preparation
Tasks that need to be completed before Friday

The job sending the request to sign-off of RC2, also creates the issue to track the final release steps in this repository.

  * #### Readme
    - Create a tracking issue in [www.eclipse.org-eclipse](https://github.com/eclipse-platform/www.eclipse.org-eclipse) (see [Readme file for 4.26](https://github.com/eclipse-platform/www.eclipse.org-eclipse/issues/24) as an example).
    - Add Readme files and update generatation scripts.
  * #### Migration Guide
    - Create a tracking issue in [eclipse.platform.releng.aggregator](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues) and link it to the main release issue.
    - Every release a new porting guide and folder need to be added to [eclipse.platform.common/bundles/org.eclipse.jdt.doc.isv/porting](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/tree/master/eclipse.platform.common/bundles/org.eclipse.jdt.doc.isv/porting), named with the version being migrated *to*.
      - i.e `eclipse_4_27_porting_guide.html` is for migrating from 4.26 tp 4.27.
    - Update `topics_Porting.xml` in [eclipse.platform.common/bundles/org.eclipse.jdt.doc.isv](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/tree/master/eclipse.platform.common/bundles/org.eclipse.jdt.doc.isv) and [eclipse.platform.common/bundles/org.eclipse.platform.doc.isv](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/tree/master/eclipse.platform.common/bundles/org.eclipse.platform.doc.isv)
    - Update the name of the proting html document in [eclipse.platform/platform/org.eclipse.platform/intro/migrateExtensionContent.xml](https://github.com/eclipse-platform/eclipse.platform/blob/master/platform/org.eclipse.platform/intro/migrateExtensionContent.xml) 

### RC2 Respin
Sometimes there is a critical issue that requires a fix, if it's decided that one is needed then an `RC2a` (followed by `RC2b`, `RC2c`, etc. if necessary) is built from the maintenance branch and promoted using the RC2 process.
  1. Backport the fix to the corresponding maintenance branch
  2. Start a new I-build for that release.
    The still existing I-build jobs are now running on the maintenance branch and will include the backported fix.
  3. Promote the newly built I-build as `RC2a` or `RC2b`, ... and use it for the final release promotion
  - In case the a fix is necessary after the final release promotion, but before its publication, additionally run the final release promotion with the new RC2x.
    Also make sure to discard the previously promoted artifacts, by deleting the previous release download and update sites and to **DROP** the artifacts previously staged for Maven Central.

### Release
The actual steps to release

**Friday**
  * #### Promote to GA
    - After **SimRel** declares RC2 (usually on Friday before the final release) run the [Promote Build](https://ci.eclipse.org/releng/job/Releng/job/promoteBuild/) job to promote RC2 (or RC2a, ...) to be the final release.
      - `DROP_ID`: Final release candidate's ID, e.g.: `S-4.36RC2-202505281830`
      - `CHECKPOINT`: blank for final releases
    - It creates the download and update sites of the final release at their final location, but does not publish them and keeps them hidden from the public.
    - It also creates pull requests to update the build configuration on the master and corresponding maintenance branch to the promoted release.
      - Only submit them **AFTER** the release was finally published.
    - It will again update the Acknowledgements and creates a PR if there are any last changes.
      Review and submit it at https://github.com/eclipse-platform/www.eclipse.org-eclipse/pulls
    - You can subscribe to [cross-project-issues](https://accounts.eclipse.org/mailing-list/cross-project-issues-dev) to get the notifications on Simrel releases.

**Tuesday/Monday**
  - #### Complete Publication to Maven Central
    - The final release to Maven-Central should happen latest by Tuesday before the release since there is up to a 24 hour delay for the Maven mirrors.
    - The artifacts have been deployed into a Maven-Central _staging_ repository by the `Promote Build` job when the RC was promoted to GA release.
    - Login to https://central.sonatype.com/ and `Publish` the three staging repositories for `Platform`, `JDT` and `PDE` by closing them.
      - Make sure the name of the deployment you are about to release matches the tag and timestamp of the final release repository.
        E.g for a P2 repo with id `R-4.36-202505281830` the deployments should be named `PLATFORM_R-4.36-202505281830`, `PDE_R-4.36-202505281830` or `JDT_R-4.36-202505281830` respectivly.
      - If you do not have an account on `central.sonatype.com` for performing the rest of the release request one by creating an [EF Help Desk](https://gitlab.eclipse.org/eclipsefdn/helpdesk/-/issues) issue to get permissions for platform, JDT and PDE projects and tag an existing release engineer to give approval.
  - #### Contribute to SimRel
    - The final release repo has to be contributed to SimRel before the currently active I-build repo is deleted (is done automatically after the release is published)
  - #### Update build versions to GA
    - Upon promotion of the final GA build, PRs for the the master and corresponding maintenance branch were created to update the build to the GA versions.
    - Submit these PRs now.

**Wednesday**
The release is scheduled for 15:00 UTC.

  - #### Make the Release Visible
    - Run the [Publish Promoted Build](https://ci.eclipse.org/releng/job/Releng/job/publishPromotedBuild/) job in Releng jenkins to make the promoted build visible on the download page.
      - `releaseBuildID`: the full id of the release build to publish, e.g. `R-4.36-202505281830`
  - The (still existing) I-Build jobs of that release can now be deleted (together with the associdated test jobs), because we are now sure a RC respins won't happen anymore.

### Preparation for the next Release
  - After RC2 is promoted/published run the [`Prepare Next Development Cycle`](https://ci.eclipse.org/releng/job/Releng/job/prepareNextDevCycle/) job
    - Review the Pull-Requests created by it in this `eclipse.platform.releng.aggregator` repository and all its submodules and complete the listed tasks.

#### **Update the Build Calendar:**
  - Create an [issue](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/issues/289) and update the [build calendar](https://calendar.google.com/calendar/u/0?cid=cHJmazI2ZmRtcHJ1MW1wdGxiMDZwMGpoNHNAZ3JvdXAuY2FsZW5kYXIuZ29vZ2xlLmNvbQ) for the next GA release based on the [Simultaneous Release schedule](https://wiki.eclipse.org/Simultaneous_Release).
  - Each stream has its own [page](https://github.com/eclipse-simrel/.github/blob/main/wiki/SimRel/2026-06.md) with a more detailed schedule.
  - List of people who can edit the calendar
    - Alexander Kurtakov(@akurtakov)
    - Gireesh Punathil
    - Rahul Mohanan

#### **Update Splash Screen:**
  - Future spash screens are kept in a subfolder of [eclipse.platform/platform/org.eclipse.platform](https://github.com/eclipse-platform/eclipse.platform/tree/master/platform/org.eclipse.platform/futureSplashScreens).
  - NOTE: Splash screens are created 4 at a time, for 4 consequtive quarterly releases, so they need to be requested once a year before the 20XX-06 release (the cycle is 2022-06 -> 2023-03, etc). Create an issue in the [Eclipse Help Desk](https://gitlab.eclipse.org/eclipsefdn/helpdesk/-/issues) similar to [Bug 575781](https://bugs.eclipse.org/bugs/show_bug.cgi?id=575781). It is customary to do this by the previous -09 (September) release so that there's plenty of time for discussion before the -06 (June) release is opened. 
  - Issue for the 2023 releases is [https://gitlab.eclipse.org/eclipsefdn/helpdesk/-/issues/2336](https://gitlab.eclipse.org/eclipsefdn/helpdesk/-/issues/2336)
