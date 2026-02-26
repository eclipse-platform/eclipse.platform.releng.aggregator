# Jenkins

## Job Creation with Job DSL

### Create Jobs

The [Create Jobs](https://ci.eclipse.org/releng/job/CreateJobs/) job is used to populate the Jenkins subfolders with the jobs defined in [JenkinsJobs](JenkinsJobs).
It is defined in [`JenkinsJobs/seedJob.jenkinsfile`](JenkinsJobs/seedJob.jenkinsfile).

Create Jobs *must be run manually*.
Unfortunately JobDSL needs to be run by a specific user, so the build cannot be automatically started by a timer or when it detects jenkins changes without installing an additional Plug-in (like [Authorize Project](https://plugins.jenkins.io/authorize-project/)) which supposedly still works but is abandoned.
This means that while any committer can make changes to the Jenkins Jobs in git, someone with Jenkins permissions will have to start the build to implement those changes.

Obsolete jobs have to be deleted manually.
They are not deleted automatically as some may be are still in use for a short time (e.g. Y-builds if a java-release is imminent) or to serve as reference in case the new jobs have problems.

### The JenkinsJobs Folder

As a general rule, the [JenkinsJobs](./JenkinsJobs) folder should match the layout of Jenkins itself.
Every subfolder contains a `.jenkinsfile` file for each individual job.
The folders containing the individual job definitions are replicated in the Jenkins instance. 
Labels and descriptions of the folders implied by that structure can be specified in the [Seed Job](JenkinsJobs/seedJob.jenkinsfile):
https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/blob/9a0ef15799eb3420d748a54d8719ca08c3c7b5af/JenkinsJobs/seedJob.jenkinsfile#L19-L26

Multiple build and test jobs have the release version in the job name because it is required for result parsing, e.g. the [automated tests](https://ci.eclipse.org/releng/job/AutomatedTests/).
In order to minimize constant adjustments of the job definitions the active streams are configured through the [buildConfigurations.json](JenkinsJobs/buildConfigurations.json) file.
This file is used to create the versioned build and test jobs and is also read by these jobs during their execution for detailed configuration.

### New Jobs

Adding a job to Jenkins should be as easy as adding a new `.jenkinsfile` file to git, but here are some general points to consider.

- Use **only CamelCase in filenames**, no spaces or dashes (it breaks JobDSL).
- The job Names/IDs (used in the job URL) are the same as the names of the `.jenkinsfile` files, without `.jenkinsfile` extension.
- The display names are derived from that filenames by inserting spaces before each uppercase letter.
  - The generated display name can be overwritten by a static `_JOB_DISPLAY_NAME` field.
- Similarly the job's description can be defined using a static `_JOB_DESCRIPTION` field.
  
* **Script Formatting** You can use `'''`triple apostrophes`'''` to define multiline scripts. It's best to append a back-slash to prevent a blank line at the top of the script step in Jenkins.
* To see what plugins are installed and what methods are available in the Releng Jenkins you can consult the [api viewer](https://ci.eclipse.org/releng/plugin/job-dsl/api-viewer/index.html).

### Testing new Jobs or trying out changes before production

Just like regular code, new changes or changes on jobs should be tested in a 'testing' environment before being used in production.
As we don't have a dedicated testing environment, the regular [RelEng Jeninks](https://ci.eclipse.org/releng/) has to be used,
but one can use a 'testing' copy of the final job that doesn't have any effect on the production environment.
A testing job has to be created manually in the Jenkins UI and will only exist temporarily for the time of testing.
It's recommended to put your name in the job's name to help others identifying the responsible person, in case you forgot to clean it up later.
In the testing job you can use another source for the Jenkins file, for example your fork of this repo and another branch.

**Make sure your testing job does not have an effect on the production environment**

'Disable' all steps that have any effect on the production environment or redirect them to a testing environment too
- Disable `git push`. Use `--dry-run` or comment out
- Disable modification at the download server via `ssh` or `scp` or redirect changes to a 'try-out' area, which is hidden from the public
  - Some jobs already have a `DRY_RUN` parameter that deploy to `https://download.eclipse.org/eclipse/try-outs/`.
- Don't send mails to mailing lists via the `emailext` step, instead just send a mail to your private mail directly.
- Disable triggers for other jobs that may modify the production environment

# Updating Java Versions

Every 6 months there is a new Java release which requires additional builds and testing.
The Java release usually corresponds to odd numbered streams so a new Java version would be tested in separated builds run during the 4.41 stream and will then be included in the main line of the 4.42 release.

## Y and P Builds
The **Y-build** is a full Eclipse-SDK build with the support of the new Java version included for testing.

The **P-build** is a patch build that contains modified plugins designed to be installed on top of the current I-build to use the new java version support as early as possible.
They are now managed by the JDT-project itself in [org.eclipse.jdt.releng](https://github.com/eclipse-jdt/eclipse.jdt/tree/master/org.eclipse.jdt.releng).

The Y-build themselves and their unit tests are, just like the regular I-builds, defined in the [build.jenkinsfile](./JenkinsJobs/Builds/build.jenkinsfile) respectively the [integrationTests.jenkinsfile](./JenkinsJobs/AutomatedTests/integrationTests.jenkinsfile) in git
and used for the [Y Builds](https://ci.eclipse.org/releng/job/YBuilds/) folder in Jenkins.

## Setting Up New Builds

When the JDT team is ready they will raise an issue to create new Y builds and supply the name of the new branch, usually `BETA_JAVA##`.

**Things to Do:**
  * Update the Y-build configuration in the (buildConfigurations.json)[JenkinsJobs/buildConfigurations.json]
    - Update `typeName` to the name of the new java version.
    - Remove the disablement of the current stream in the Y-build configuration (should be the only Y-build stream).
    - Update `branches` to point to the new BETA branch.
    - Add unit tests for the new java version and remove old ones.
