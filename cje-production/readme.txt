This is the location where we will develop production scripts for use on Cloudbees Jenkins Enterprise

Here are some ground rules
1. Each script starts with mb<xxx>_<scriptname>.sh. xxx is a 3 digit number
	000-099 - preparing build environment like setting environment variables etc.
	100-199 - cloning repos and preparing them with right branches/tags and tagging(git operations)
	200-299 - Maven operations(Updating pom with versions from manifest, create tar ball, build SDK/patch etc)
	300-399 - gather parts (collecting different artifacts into a temporary build location)
	400-499 - copy eclipse, repo and equinox artifacts to respective temporary download locations and generate necessary web pages
	500-599 - Generate build reports (running p2.repo.analyzers and dirt report, jdeps reports etc)
	600-699 - promote the build to download.eclipse.org.
	700-799 - trigger tests and send mails

2. Every script should accept $ENV_FILE. this envrironment file is created at the preparing the build environment stage.
3. Every script should source "common-functions.shsource". This will contain common methods used across the scripts


Folder structure
root (cje-production)
	readme.txt
	mbscripts
	common
	master-build.sh
	siteDir (temporary location to hold the website)


