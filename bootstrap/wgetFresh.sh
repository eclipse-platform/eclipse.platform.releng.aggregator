#!/usr/bin/env bash

# gets a fresh copy of the primary/working "bootstrap" scripts needed in build home.

# codifying the branch (or tag) to use, so it can be set/chagned in one place
initScriptTag=master

# tags:
# http://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/plain/bootstrap/wgetFresh.sh?tag=vI20120417-0700


# to build, all that's needed is the appropriate mbabcX.sh scripts. along with "bootstrap.sh". 
# It gets what ever else it needs.

wget --no-verbose -O mb382M.sh http://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/plain/bootstrap/mb382M.sh?h=$initScriptTag 2>&1;
wget --no-verbose -O mb422M.sh http://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/plain/bootstrap/mb422M.sh?h=$initScriptTag 2>&1;
wget --no-verbose -O mb430I.sh http://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/plain/bootstrap/mb430I.sh?h=$initScriptTag 2>&1;
wget --no-verbose -O mb430N.sh http://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/plain/bootstrap/mb430N.sh?h=$initScriptTag 2>&1;
wget --no-verbose -O bootstrap.sh http://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/plain/bootstrap/bootstrap.sh?h=$initScriptTag 2>&1;


# handy script to "wrap" a normal build script such as mb4I.sh to set global test/debug settings
#wget --no-verbose -O testBuild.sh http://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/plain/bootstrap/testBuild.sh?h=$initScriptTag 2>&1;

# get this script itself (would have to run twice to make use changes, naturally)
# and has trouble "writing over itself" so we put in a file with 'NEW' suffix
# but will remove it if no differences found.
# and a command line like the following works well

wget --no-verbose -O wgetFresh.NEW.sh http://git.eclipse.org/c/platform/eclipse.platform.releng.aggregator.git/plain/bootstrap/wgetFresh.sh?h=$initScriptTag 2>&1;

differs=`diff wgetFresh.NEW.sh wgetFresh.sh`
echo "differs: ${differs}"
if [ -z "${differs}" ]
then
    # 'new' not different from existing, so remove 'new' one
    rm wgetFresh.NEW.sh
else
    echo " "
    echo "     wgetFresh.sh has changed. Compare with and consider replacing with wgetFresh.NEW.sh"
    echo "  "
fi

chmod +x *.sh
