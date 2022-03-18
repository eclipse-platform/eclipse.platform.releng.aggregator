# Contributing to Eclipse Platform

Thanks for your interest in this project.

## Project description

Eclipse Platform defines the set of frameworks and common services that
collectively make up infrastructure required to support the use of Eclipse as a
component model, as a Rich Client Platform (RCP) and as a comprehensive tool
integration platform. These services and frameworks include a standard workbench
user interface model and portable native widget toolkit, a project model for
managing resources, automatic resource delta management for incremental
compilers and builders, language-independent debug infrastructure, and
infrastructure for distributed multi-user versioned resource management.

* https://projects.eclipse.org/projects/eclipse.platform

## Developer resources

Information regarding source code management, builds, coding standards, and
more.

* https://projects.eclipse.org/projects/eclipse.platform/developer

The project issues and source code are maintained in GitHub
* https://github.com/eclipse-platform/eclipse.platform.releng.aggregator

Be sure to search for existing issues before you create another one. Remember that
contributions are always welcome!

### Setting up Git Hub account

Create an account at github.com using the same email id used for eclipse account. 
In case contributor already have an account add the email to your account using https://github.com/settings/emails

Add github account id to contributor's eclipse account under social media links section. 
If contributor is already a commiter in any of the projects, a mail containing invite to join 
that project's github organisation will be received within 2 hours. The contributor should accept the invite 
to maintain committer status in the github organisation

To create commits it is recommended to add ssh public keys to github account. This can be done using https://github.com/settings/keys

### Recommended workflow

Recommended way of developing code is inside a private branch. Here are the steps

1. Create a fork using the fork button available on the github repository page. Creates a private repository under github account.
2. If fork already exists, click of fetch upstream to get latest source code.
3. Clone the forked repository to eclipse workspace. Repository link can be found by clicking code button on the top right in "<> code" tab
4. Create a local branch
5. Delevelop fix
6. Commit code changes to local branch. Make multiple commits if necessary as history can be retained.
7. Once the fix is ready, push the branch to origin(in this case forked repositoy)
8. Open forked repository page, switch to the branch where the fix is developed, a new option appears just below code button "Contribute" Click on this to open "Pull Request" also called PR.
9. Verify the branch information like contributing to master or contributiong to any other branch and list of commits. If every thing OK, please click on "Create pull request" button at the bottom right
10. Can add reviewers if necessary. 
11. Any commits pushed to the devlopment branch(used to create PR) automatically gets added to opened PR.
12. Reviewer can review PR on the github portal itself or fetch the PR using egit menu "Fetch github PR"
13. All PRs will get verified for eca and PR validations(same as gerrit validation)
14. Once the PR is approved there two options to merge. Can select either of these based on the requirement the following are recommendations only. Need to select based on the requirement
  * Rebase and Merge (retains commit history from PR useful in developing a feature)
  * Squash and Merge (All commits in PR gets squashed in to a single commit useful in bug fixes)
15. Once PR gets merged the development branch used for the PR can be deleted. 

Commit message recommendations

  \<issue title\> \#\<issue number\>
  
  Example: The eclipse-test-framework deliverable contains unsigned bundles \#32
  
  Again this is a recommendation on the issue title part. Instead of issue title, if needed provide a concise description of changes. 
  Please do not forget to add issue number to the commit message. This is used to link with github issue.
  

## Eclipse Contributor Agreement

Before your contribution can be accepted by the project team contributors must
electronically sign the Eclipse Contributor Agreement (ECA).

* https://www.eclipse.org/legal/ECA.php

Commits that are provided by non-committers must have a Signed-off-by field in
the footer indicating that the author is aware of the terms by which the
contribution has been provided to the project. The non-committer must
additionally have an Eclipse Foundation account and must have a signed Eclipse
Contributor Agreement (ECA) on file.

For more information, please see the Eclipse Committer Handbook:
https://www.eclipse.org/projects/handbook/#resources-commit

## Contact

Contact the project developers via the project's "dev" list.

* https://dev.eclipse.org/mailman/listinfo/platform-dev
