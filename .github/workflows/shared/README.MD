# Platform Shared Workflows

This repository contains some workflows that can be shared across all platform repositories, se below for some details of the workflows.

## verifyFreezePeriod.yml

Check if there is currently a freeze period in place.

## checkMergeCommits.yml

Check if a PR contains unwanted merge commits.

## publishTestResults.yml

Publish the test results of a ci action.

It has the follwoing inputs:

- gist-url - if given creates a badge to publish the test results
- gist-file - if a gist-url is given, this can control the name of the created file, defaults to badge.svg

If a gist-url is given it requires the follwoing secret:

- gist-token - the token for the gist, must be requested with the eclipse help-desk to be added as a secret to the repository

## mavenBuild.yml

A unified maven matrix build that covers the usual workflow for a build verification of a platform repository.

## checkDependencies.yml

Workflow that can be used as a timed action to check if any bundles need to update their version ranges of dependencies.
It checks all artifacts that match the provided range and suggest a lower bound depending on the references in the classfiles.

## cleanCode.yml

Workflow that could be used as a timed action to check if any automatic code cleanup can be performed.
Actions to perform must be defined in the configuration of the [quickfix-and-cleanup-mojo](https://github.com/eclipse-tycho/tycho/blob/main/RELEASE_NOTES.md#new-quickfix-and-cleanup-mojo)
that is called by this action. For this purpose it uses the follwoing execution ids `cleanups`, `quickfixes` and `manifest`.

## updateTarget.yml

A Workflow that automatically checks a target file for updates of its dependencies.

It has the follwoing inputs:
- author - desired commit author / email 
- token - token to use to create the PR while this action can work with the default github token, it is recommended to use a PAT if used as a timed action because otherwhise PR checks can't run, see [here](https://github.com/marketplace/actions/create-pull-request#token) for details.
- path - path to a project that contains the target to update

and expects that the pom configures an execution named `update-target` for its configuration, or otherwhise is running with the defaults.
An example might look like this:

```
<plugin>
  <groupId>org.eclipse.tycho.extras</groupId>
  <artifactId>tycho-version-bump-plugin</artifactId>
  <version>${tycho.version}</version>
  <executions>
    <execution>
        <id>update-target</id>
        <configuration>
            <allowMajorUpdates>false</allowMajorUpdates>
            <updateSiteDiscovery>parent</updateSiteDiscovery>
            <mavenRulesUri>${project.basedir}/update-rules.xml</mavenRulesUri>
        </configuration>
    </execution>
  </executions>
</plugin>
```

See the [tycho-version-bump:update-target](https://tycho.eclipseprojects.io/doc/latest/tycho-extras/tycho-version-bump-plugin/update-target-mojo.html) for further details.

## checkVersions.yml

A workflow that checks for required but missing service-version increments for projects that are changed for the first time in the current development-cycle and applies them.
It creates a git-commit with the required changes applied.

## publishVersionCheckResults.yml

Publishes the results of the `checkVersions` workflow by adding a comment to the affected PR that lists files to change and additionally pushes the created git-commit to the PR's branch.
