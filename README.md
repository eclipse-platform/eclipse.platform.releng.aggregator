Aggregator repo for Eclipse SDK builds
======================================

This repo is used to build the Eclipse SDK which contains the framework for Eclipse based applications, the Java® development tools (JDT™) and the Plug-in Development Environment (PDE™).
To clone it, it is recommended to use one of the URLs found on this website: 
- https://github.com/eclipse-platform/eclipse.platform.releng.aggregator

An anonymous clone can be done via the following command:
```
git clone --recurse-submodules https://github.com/eclipse-platform/eclipse.platform.releng.aggregator.git
```
It will also clone all submodules.

How to build the Eclipse SDK
----------------------------
For a complete build run the following command from the root of this repository:
```
mvn clean verify
```
But this will result in a significant runtime of up to 10 hours, mainly due to test executions.
Skiping the execution of tests and building in parallel reduces the runtime to about 10-20min (depending on your computer):
```
mvn clean verify -DskipTests --threads 1C
```

After a successful build, the `Eclipse-SDK` and `Eclipse-Platform` products are located at
```
products/eclipse-sdk/target/products
products/eclipse-platform/target/products
```
and the assembled P2 update-site is at
```
sites/eclipse-platform-repository/target/repository
```

To update your local clone and all it's submodules to the latest state run:
```
git checkout master
git pull --recurse-submodules
git submodule update
```
Furthermore it's recommended to make sure your work-tree is clean before a build, which can be ensured by executing
```
git submodule foreach git clean -f -d -x
git submodule foreach git reset --hard HEAD
git clean -f -d -x
git reset --hard HEAD
```

Build with custom compiler
--------------------------

To compile the build itself with a custom compiler perform the following step after cloning the submodules:

```
# compile local version
mvn clean install -f eclipse.jdt.core/org.eclipse.jdt.core.compiler.batch -DlocalEcjVersion=99.99

# run build with local compiler
mvn clean verify -DskipTests=true -Dcbi-ecj-version=99.99
```

Build requirements
------------------

The build commands require the installation and setup of Java 25 or higher and Maven version 3.9.12 or higher.
See also the complete instructions on the [Platform Build wiki](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/wiki/Platform-Build). 
Note, it is highly recommended to use toolchains.xml and `-Pbree-libs` as described in [Using BREE Libs](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/wiki/Platform-Build#using-bree-libs).

Integration builds
------------------

The integrations (nightly) build jobs are hosted on Jenkins instance https://ci.eclipse.org/releng/job/builds/.

The job with the highest release number is the one that builds nightly SDK build, like `I-build-4.41` job for 4.41 SDK.

- The build artifacts and test results are accessible at https://download.eclipse.org/eclipse/downloads/
- If the tests fail to start, test jobs for each platform can be found at https://ci.eclipse.org/releng/job/automated-tests
- If the build is successful but relevant functionality is severely broken and the build shouldn't be used,
  the build can be marked as *unstable* using the [Mark build](https://ci.eclipse.org/releng/job/builds/job/mark-build) job.
- Daily Maven snapshots are provided by the [Deploy to Maven repository](https://ci.eclipse.org/releng/job/releng/job/deploy-maven) job
and are available at https://repo.eclipse.org/content/repositories/eclipse-snapshots/

The [Eclipse RelEng and build calendar](https://download.eclipse.org/eclipse/downloads/#releng-calendar) provides an overview of all scheduled builds and release related events.

Milestone and release tasks
-----------------
See [Releng-Tasks 2.1](RELEASE.md) (includes links to schedule, calendar etc)

How to contribute
-----------------
Contributions to Eclipse Platform are most welcome. There are many ways to contribute,
from entering high quality bug reports, to contributing code or documentation changes.
For a complete guide, see https://github.com/eclipse-platform/.github/blob/main/CONTRIBUTING.md.

Additional informations
-----------------------

Eclipse Platform Project committers should also read [Automated Platform Builds](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/wiki/Platform-Build-Automated).

Release Engineers should also be familiar with other documents on the [Releng Wiki](https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/wiki).

License
-------

[Eclipse Public License (EPL) v2.0][2]

[2]: https://www.eclipse.org/legal/epl-2.0/
