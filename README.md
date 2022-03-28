Aggregator repo for Eclipse SDK builds
======================================

This repo is used to build the Eclipse SDK which provides the framework for Eclipse based applications, the Java development tooling and the Plug-in development tooling.
To clone it, it is recommended to use one of the URLs found on the following website: 
https://github.com/eclipse-platform/eclipse.platform.releng.aggregator

An anonymous clone can be done via the following command:

```
git clone https://github.com/eclipse-platform/eclipse.platform.releng.aggregator.git
```

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
# eclipse.platform.releng.tychoeclipsebuilder/sdk/target/products/*
```

Build requirements
------------------

The build commands require the installation and setup of Java 11 or higher and Maven version 3.5.4 or higher.
See also the complete instructions on the [Platform Build wiki](https://wiki.eclipse.org/Platform-releng/Platform_Build "Platform Build"). 
Note, it is highly recommended to use toolchains.xml and -Pbree-libs as decribed in [Using BREE Libs](https://wiki.eclipse.org/Platform-releng/Platform_Build#Using_BREE_Libs "Using BREE Libs").

Performance Tests
-----------------
See [Performance README.md](production/README.md)

Additional informations
-----------------------

Eclipse Platform Project committers should also read [Automated Platform Builds](https://wiki.eclipse.org/Platform-releng/Automated_Platform_Build "Automated Platform Builds").

Release Engineers should also be familiar with other documents on the [Releng Wiki](https://wiki.eclipse.org/Category:Eclipse_Platform_Releng "Releng Wiki").

License
-------

[Eclipse Public License (EPL) v2.0][2]

[2]: https://www.eclipse.org/legal/epl-2.0/
