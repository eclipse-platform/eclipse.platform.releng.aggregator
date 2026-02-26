# Jenkins

## **Job Creation with Job DSL**

### **Create Jobs**

The (Create Jobs)[https://ci.eclipse.org/releng/job/CreateJobs/] job is used to populate the jenkins subfolders with the jobs defined in (JenkinsJobs)[JenkinsJobs] groovy files. There are 2 Process Job DSLs steps, the first looks for FOLDER.groovy files and creates the folders, the second creates the jobs themselves.

Create Jobs *must be run manually*. Unfortunately JobDSL needs to be run by a specific user, so the build cannot be automatically started by a timer or when it detects jenkins changes without installing an additional plugin like (Authorize Project)[https://plugins.jenkins.io/authorize-project/], which supposedly still works but is abandoned and I (Sam) have not had time to investigate further or find alternatives. This means that while any committer can make changes to the Jenkins Jobs in git, someone with Jenkins rights will have to start the build to implement those changes.

Obsolete jobs have to be deleted manually.
They are not deleted automatically as some may be are still in use for a short time (e.g. Y-builds if a java-release is imminent) or to serve as reference in case the new jobs have problems.

### **The JenkinsJobs Folder**

As a general rule, the (JenkinsJobs)[JenkinsJobs] folder should match the layout of Jenkins itself. Every subfolder should contain a (FOLDER.groovy)[JenkinsJobs/Builds/FOLDER.groovy] file as well as groovy files for all individual jobs.

Note: JobDSL does also support the creation of Views, so those can be added at some point but someone will either have to manually keep them up to date with the desired jobs or write a script to keep them up-to-date.

Many jobs have the release version in the job name because it is required for results parsing (i.e. the (automated tests)[https://ci.eclipse.org/releng/job/AutomatedTests/]).
In order to minimize manual labor the currently active streams are listed in the (buildConfigurations.json)[JenkinsJobs/buildConfigurations.json] file.
Version dependent jobs then parse this file during creation and make a job for each stream.

### **New Jobs**

Adding a job to Jenkins should be as easy as adding a new groovy file to git, but here are some general pointers.

* **No dashes in filenames**, it breaks JobDSL. If there was a `-` in the job name I changed it to `_` in the file name.
* **Job Names**
  - No spaces, this is just for the good of scripts etc. The job NAME is used in the url, but you can always add a `displayName` field if you want the job to appear with a more natural looking name on the page.
    See (Releng/deployToMaven)[JenkinsJobs/Releng/FOLDER.groovy] as an example.
  - Jenkins files should use CamelCase Naming but the corresponding job names should use dash-case for better readabliliy of the resulting URLs. In the display name composed names should be separated by spaces.
    #TODO: Consisistently apply this!
  - The folder needs to be part of the job name, otherwise Jenkins will just create the job at the dashboard/top level.
* **job vs pipelineJob**
  - Generally pipelineJobs defined an dedicated jenkinsfile are prefered as they are more powerful and simpler to modify and manage, defining Jenkins _Freestyle_ jobs is discouraged.
* **Script Formatting** You can use `'''`triple apostrophes`'''` to define multiline scripts. It's best to append a back-slash to prevent a blank line at the top of the script step in Jenkins.
* To see what plugins are installed and what methods are available in the Releng Jenkins you can consult the (api viewer)[https://ci.eclipse.org/releng/plugin/job-dsl/api-viewer/index.html#].

### **Testing new Jobs or trying out changes before production**

Just like regular code, new changes or changes on jobs should be tested in a 'testing' environment before put into production.
As we don't have a dedicated testing environment, the regular [RelEng Jeninks](https://ci.eclipse.org/releng/) has to be used,
but one can use a 'testing' copy of the final job that doesn't have any effect on the production environment.
A testing job has to be created manually in the Jenkins UI and will only exist temporarily for the time of the testing.
It's recommended to put your name in the job name in order to help others, to identify the person to ask if that job is still relevant, in case you forgot to clean it up.
In the testing job you can use another source for the Jenkins file, for example your fork of this repo and another branch.

** Make sure your testing job does not have an effect on the production environment**

'Disable' all steps that have any effect on the production environment or redirect them to a testing environment too
- Disable `git push`. Use `--dry-run` or comment out
- Disable modification at the download server via `ssh` or `scp` or redirect changes to a 'try-out' area hidden from the public
  - Some jobs already have a `DRY_RUN` parameter that deploy to `https://download.eclipse.org/eclipse/try-outs/`.
- Don't send mails to mailing lists via the `emailext` step, instead just send a mail to your private mail directly.

# Updating Java Versions

Every 6 months there is a new Java release which requires additional builds and testing. The java release usually corresponds to odd numbered streams so a new java version would be tested in additional builds run during the 4.25 stream and then included in the 4.26 release I-builds.

## **Y and P Builds**
The **Y-build** is a full sdk build with the new java version for testing.

The **P-build** is a patch build that contains modified plugins designed to be installed on top of the current I-build to test the new java version.
They are now managed by the JDT-project itself in (org.eclipse.jdt.releng)[https://github.com/eclipse-jdt/eclipse.jdt/tree/master/org.eclipse.jdt.releng].

The builds themselves and their unit tests are in the (Y Builds)[JenkinsJobs/YBuilds] folder in git and the (Y Builds)[https://ci.eclipse.org/releng/job/YBuilds/] folder in jenkins.

## Setting Up New Builds

When the JDT team is ready they will raise an issue to create new Y builds and supply the name of the new branch, usually `BETA_JAVA##`.

**Things to Do:**
  * Update the Y-build configuration in the (buildConfigurations.json)[JenkinsJobs/buildConfigurations.json]
    - Update `typeName` to the name of the new java version.
    - Remove the disablement of the current stream in the Y-build configuration (should be the only Y-build stream).
    - Update `branches` to point to the new BETA branch.
    - Add unit tests for the new java version and remove old ones.
