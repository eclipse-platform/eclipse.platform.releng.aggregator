Aggregator repo for Eclipse SDK builds
======================================

This repo is used to build the Eclipse SDK which provides the framework for Eclipse based applications, the Java development tooling and the Plug-in development tooling.
To clone it, it is recommended to use one of the URLs found on the following website: 
https://github.com/eclipse-platform/eclipse.platform.releng.aggregator

An anonymous clone can be done via the following commands:

```
git clone https://github.com/eclipse-platform/eclipse.platform.releng.aggregator.git
cd eclipse.platform.releng.aggregator
git submodule update --init --recursive
```

The latter command will clone all submodules.

How to build the Eclipse SDK
----------------------------

To run a complete build, on your local machine, run the following commands.
The `-DskipTests=true` will skip the tests which take a significant time to run, e.g., up to 10 hours.

```
# clean up "dirt" from previous build see Bug 420078
git submodule foreach git clean -f -d -x
git submodule foreach git reset --hard HEAD
git clean -f -d -x
git reset --hard HEAD

# update master and submodules
git checkout master
git pull --recurse-submodules
git submodule update

# run the build
mvn clean verify  -DskipTests=true

# find the results in
# eclipse.platform.releng.tychoeclipsebuilder/eclipse.platform.repository/target/products
```

Build with custom compiler
--------------------------

To compile the build itself with a custom compiler perform the follwoing step after cloning the submodules:

```
# compile local version
mvn clean install -f eclipse.jdt.core/org.eclipse.jdt.core.compiler.batch -DlocalEcjVersion=99.99

# run build with local compiler
mvn clean verify  -DskipTests=true -Dcbi-ecj-version=99.99
```

Build requirements
------------------

The build commands require the installation and setup of Java 17 or higher and Maven version 3.5.4 or higher.
See also the complete instructions on the [Platform Build wiki](https://wiki.eclipse.org/Platform-releng/Platform_Build "Platform Build"). 
Note, it is highly recommended to use toolchains.xml and -Pbree-libs as decribed in [Using BREE Libs](https://wiki.eclipse.org/Platform-releng/Platform_Build#Using_BREE_Libs "Using BREE Libs").

Integration builds
------------------

The integrations (nightly) build jobs are hosted on Jenkins instance https://ci.eclipse.org/releng/job/Builds/.

The job with the highest release number is the one that builds nightly SDK build, like https://ci.eclipse.org/releng/job/Builds/job/I-build-4.39/ job for 4.39 SDK.

- The build artifacts and test results are accessible at https://download.eclipse.org/eclipse/downloads/
- If the tests fail to start, test jobs for each platform can be found at https://ci.eclipse.org/releng/job/AutomatedTests/
- If the build is successful but relevant functionality is severely broken and the build shouldn't be used, the build can be marked as unstable via the [Mark Build](https://ci.eclipse.org/releng/job/Builds/job/markBuild/) job.
- Daily Maven snapshots are provided by the [Deploy To Maven](https://ci.eclipse.org/releng/job/Releng/job/deployToMaven) job
and are available from https://repo.eclipse.org/content/repositories/eclipse-snapshots/

Milestone and release tasks
-----------------
See [Releng-Tasks 2.0](RELEASE.md) (includes links to schedule, calendar etc)

How to contribute
-----------------
Contributions to Eclipse Platform are most welcome. There are many ways to contribute,
from entering high quality bug reports, to contributing code or documentation changes.
For a complete guide, see https://github.com/eclipse-platform/.github/blob/main/CONTRIBUTING.md.

Additional informations
-----------------------

Eclipse Platform Project committers should also read [Automated Platform Builds](https://wiki.eclipse.org/Platform-releng/Automated_Platform_Build "Automated Platform Builds").

Release Engineers should also be familiar with other documents on the [Releng Wiki](https://wiki.eclipse.org/Category:Eclipse_Platform_Releng "Releng Wiki").

License
-------

[Eclipse Public License (EPL) v2.0][2]

[2]: https://www.eclipse.org/legal/epl-2.0/
