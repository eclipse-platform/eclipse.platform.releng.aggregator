# Jenkins

## **Job Creation with Job DSL**

**Create Jobs**

The (Create Jobs)[https://ci.eclipse.org/releng/job/Create%20Jobs/] job is used to populate the jenkins subfolders with the jobs defined in (JenkinsJobs)[JenkinsJobs] groovy files. There are 2 Process Job DSLs steps, the first looks for FOLDER.groovy files and creates the folders, the second creates the jobs themselves.

Since not every folder needs to be updated every release cycle (for example (JenkinsJobs/YBuilds)[JenkinsJobs/YBuilds]) the currently active folders need to be explicitly listed in the Process Job DSLs step of the build. Likewise, unless you want the YBuilds to be recreated when they are no longer needed, the YBuilds folder will need to be removed from Create Jobs after the release. 

Create Jobs *must be run manually*. Unfortunately JobDSL needs to be run by a specific user, so the build cannot be automatically started by a timer or when it detects jenkins changes without installing an additional plugin like (Authorize Project)[https://plugins.jenkins.io/authorize-project/], which supposedly still works but is abandoned and I (Sam) have not had time to investigate further or find alternatives. This means that while any committer can make changes to the Jenkins Jobs in git, someone with Jenkins rights will have to start the build to implement those changes.

Exceptions: 
  - (StartSmokeTests)[https://ci.eclipse.org/releng/job/Start-smoke-tests/] predates the rest of the groovy migrations and changing the script to fit JobDSL would have just complicated it with little gain so it was left as is. The source file ((StartSmokeTests.groovy)[JenkinsJobs/SmokeTests/StartSmokeTests.groovy]) is kept with the rest of the smoke test groovy files, but if JobDSL tries to build it it fails so instead of following the normal `JenkinsJobs/FOLDER/*.groovy` format, smoke tests are listed in Create Jobs as `JenkinsJobs/SmokeTests/smoke_*.groovy` specifically.

Currently jobs also need to be deleted manually.

**The JenkinsJobs Folder**

As a general rule, the (JenkinsJobs)[JenkinsJobs] folder should match the layout of Jenkins itself. Every subfolder should contain a (FOLDER.groovy)[JenkinsJobs/Builds/FOLDER.groovy] file as well as groovy files for all individual jobs. 

Note: JobDSL does also support the creation of Views, so those can be added at some point but someone will either have to manually keep them up to date with the desired jobs or write a script to keep them up-to-date.

Many jobs have the release version in the job name because it is required for results parsing (i.e. the (automated tests)[https://ci.eclipse.org/releng/job/AutomatedTests/]). In order to minimize manual labor the currently active streams are listed in the (JobDSL.json)[JenkinsJobs/JobDSL.json] file. Version dependent jobs then parse this file during creation and make a job for each stream.

**New Jobs**

Adding a job to Jenkins should be as easy as adding a new groovy file to git, but here are some general pointers.

* **No dashes in filenames**, it breaks JobDSL. If there was a `-` in the job name I changed it to `_` in the file name.
* **Job Names**
  - No spaces, this is just for the good of scripts etc. The job NAME is used in the url, but you can always add a `displayName` field if you want the job to appear with a more natural looking name on the page. See (Releng/deployToMaven)[JenkinsJobs/Releng/FOLDER.groovy] as an example.
  - The folder needs to be part of the job name, otherwise Jenkins will just create the job at the dashboard level.
* **job vs pipelineJob**
  - Anything that can build on a regular jenkins node or via an available lable uses the (job)[https://jenkinsci.github.io/job-dsl-plugin/#path/job] template.
  - Anything that defines its own kubernetes pod needs to be a (pipelineJob)[https://jenkinsci.github.io/job-dsl-plugin/#path/pipelineJob]
* **Script Formatting** You can use `'''`triple apostrophes`'''` to define multiline scripts. I've found it's best to start the script on the same line and not the next, otherwise you end up with blank space at the top of the script step in Jenkins which can break the build (like the arm64 smoke tests).
* To see what plugins are installed and what methods are available in the Releng Jenkins you can consult the (api viewer)[https://ci.eclipse.org/releng/plugin/job-dsl/api-viewer/index.html#].


# Updating Java Versions

Every 6 months there is a new Java release which requires additional builds and testing. The java release usually corresponds to odd numbered streams so a new java version would be tested in additional builds run during the 4.25 stream and then included in the 4.26 release I-builds. 

## **Y and P Builds**
The **Y-build** is a full sdk build with the new java version for testing. 

The **P-build** is a patch build that contains modified plugins designed to be installed on top of the current I-build to test the new java version.
They are now managed by the JDT-project itself in (org.eclipse.jdt.releng)[https://github.com/eclipse-jdt/eclipse.jdt/tree/master/org.eclipse.jdt.releng].

The builds themselves and their unit tests are in the (Y Builds)[JenkinsJobs/YBuilds] folder in git and the (Y and P Builds)[https://ci.eclipse.org/releng/job/YPBuilds/] folder in jenkins.

## Setting Up New Builds

When the JDT team is ready they will raise an issue to create new Y and P builds and supply the name of the new branch, usually BETA_JAVA##.

**Things to Do:**
  * Update the Y-build (Y_build.groovy)[JenkinsJobs/YBuilds/Y_build.groovy].
    - Update `branchLabel` and `typeName` to the name of the new java version
  * Update and rename the java repository files in (cje-production/streams)[cje-production/streams]
    - Repos without a BETA_JAVA## branch should be set to master
  * Add unit tests for the new java version in (JenkinsJobs/YBuilds)[JenkinsJobs/YBuilds]
  * Add Y builds to (Create Jobs)[https://ci.eclipse.org/releng/job/Create%20Jobs/] in Jenkins if they've been removed
