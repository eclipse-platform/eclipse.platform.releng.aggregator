This is the location where we will develop production scripts for use on Cloudbees Jenkins Enterprise

Here are some ground rules
1. Each script starts with mb<xxx>_<scriptname>.sh. xxx is a 3 digit number
	000-099 - preparing build environment like setting environment variables etc.
	200-299 - Maven operations(Updating pom with versions from manifest, create tar ball, build SDK/patch etc)
	300-399 - gather parts (collecting different artifacts into a temporary build location)
	500-599 - Generate build reports (running p2.repo.analyzers and dirt report, jdeps reports etc)
	600-699 - promote the build to download.eclipse.org.

2. Every script should accept $ENV_FILE. this envrironment file is created at the preparing the build environment stage.
3. Every script should source "common-functions.shsource". This will contain common methods used across the scripts
