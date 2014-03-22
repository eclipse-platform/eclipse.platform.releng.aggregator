eclipse.platform.releng.aggregator
==================================

Aggregator repo for Eclipse SDK builds.

To run a complete build, on your local machine, run the commands from ./scripts/build.sh in this folder.

These commands requires the installation and setup of Java 1.8 or higher and Maven version 3.0.5 or higher Plus, other settings, alternatives, and other recommendations are made in the complete instructions on the [Platform Build wiki](http://wiki.eclipse.org/Platform-releng/Platform_Build "Platform Build"). Note, it is highly recommended to use toolchains.xml and -Pbree-libs as decribed in [Using BREE Libs](https://wiki.eclipse.org/Platform-releng/Platform_Build#Using_BREE_Libs "Using BREE Libs"). If you do use BREE Libs (with proper toolchains.xml file) you can use Java 1.7 to "run the build".

Eclipse Platform Project committers should also read [Automated Platform Builds](http://wiki.eclipse.org/Platform-releng/Automated_Platform_Build "Automated Platform Builds").

Release Engineers should also be familiar with other documents on the [Releng Wiki](http://wiki.eclipse.org/Category:Eclipse_Platform_Releng "Releng Wiki").

License
-------

[Eclipse Public License (EPL) v1.0][2]

[2]: http://wiki.eclipse.org/EPL
