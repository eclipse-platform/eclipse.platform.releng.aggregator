eclipse.platform.releng.aggregator
==================================

Aggregator repo for an Eclipse SDK build

To run a complete build, on your local machine, run the commands from ./scripts/build.sh in this folder

These commands requires the installation and setup of Java 1.7 or higher and Maven  
version 3.0.5 or higher Plus, other settings, alternatives, and other recommendations are made in the 
complete instructions on the Platform Build wiki [1]

Notes for BETA_JAVA8 branch
---------------------------

This branch is to (eventually) provide a patch for 4.3.2 Release, that adds 
support for Java 8, once its released. 

Outline of differences and procedures:

The "design" of implementing this has been done to intentionally allow a 
normal, complete "4.3.2 build", but with the BETA_JAVA8 branches of those projects 
that have specific changes for Java 8. This type of build can be done in the same 
way as a normal build, as described in the Platform Build wiki [1] (But, starting with
the BETA_JAVA8 branch of aggregator, naturally). This sort of "complete build" is 
desired primary as an occasional sanity check ... not plans to deliver a complete rebuild
of 4.3.2. 

The second use is to rebuild only the dozen or so bundles that are involved in the changes. 
This requires setting a view environment variables, and invoking maven with a specific pom file. 

Specifically, the key "environment variables" are to to set/export
export BUILD_TYPE=P
export BRANCH=BETA_JAVA8
And, most important, 
export PATCH_BUILD=java8patch

Note: for more details, see "mb4P.sh" in the master branch, in the "bootstrap" directory.

The "main pom work" to produce a patch is centered in the java8path directory of tychobuilder project, so to invoke that 
specific pom, instead of the default, you need to specify the '-f' parameter to maven. 

Plus, there is a special profile that names the 4.3.2 repository to avoid 
re-compiling things that have not changed. 

So all together, the mvn command line, in addition to what ever else you'd normally have, needs to be similar to 

mvn -f eclipse.platform.releng.tychoeclipsebuilder/java8patch/pom.xml -PpatchBuild

CAUTION: the above procedure is untested and is meant as an outline, 
so anyone in community that wants to try and build the patch, can at least have basics documented. 
If you find errors or improvements, feel free to fix this file (or open a bug in Platform-->Releng.)

License
-------

[Eclipse Public License (EPL) v1.0][2]

[1]: http://wiki.eclipse.org/Platform-releng/Platform_Build
[2]: http://wiki.eclipse.org/EPL