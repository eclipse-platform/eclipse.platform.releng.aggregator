#!/usr/bin/env bash
# this is not really to be executed, but sourced where needed

# general purpose utility for "hard exit" if return code not zero.
# especially useful to call/check after basic things that should normally
# easily succeeed.
# usage:
#   checkForErrorExit $? "Failed to copy file (for example)"
checkForErrorExit () 
{
    # arg 1 must be return code, $?
    # arg 2 (remaining line) can be message to print before exiting do to non-zero exit code
    exitCode=$1
    shift
    message="$*"
    if [[ -z "${exitCode}" ]]
    then
        echo "PROGRAM ERROR: checkForErrorExit called with no arguments"
        exit 1
    fi

    if [[ -z "${message}" ]]
    then
        echo "WARNING: checkForErrorExit called without message"
        message="(Calling program provided no message)"
    fi

    # first make sure exit code is well formed
    if [[ "${exitCode}" =~ [0] ]]
    then
        #echo "exitcode was zero"
        exitrc=0
    else
        if [[ "${exitCode}" =~ ^-?[0-9]+$ ]]  
        then
            #echo "exitcode was non-zero numeric"
            exitrc=exitCode
        else
            #echo "exitode was not numeric, so will force to 1"
            exitrc=1
        fi  
    fi 

    if [[ $exitrc != 0 ]] 
    then
        echo
        echo "   ERROR. exit code: ${exitrc}"
        echo "   ERROR. message: ${message}"
        echo
        exit $exitrc
    fi
}



# USAGE: fn-git-clone URL [BRANCH [TARGET_DIR] ]
#   URL: file:///gitroot/platform/eclipse.platform.releng.aggregator.git
#   BRANCH: R4_2_maintenance 
#   TARGET_DIR: e.p.releng.aggregator
fn-git-clone () 
{
    URL="$1"; shift
    if [ $# -gt 0 ]; then
        BRANCH="-b $1"; shift
    fi
    if [ $# -gt 0 ]; then
        TARGET_DIR="$1"; shift
    fi
    echo git clone $BRANCH $URL $TARGET_DIR
    git clone $BRANCH $URL $TARGET_DIR
}

# USAGE: fn-git-checkout BRANCH | TAG
#   BRANCH: R4_2_maintenance 
fn-git-checkout () 
{
    BRANCH="$1"; shift
    echo git checkout "$BRANCH"
    git checkout "$BRANCH"
}

# USAGE: fn-git-pull
fn-git-pull () 
{
    echo git pull
    git pull
}

# USAGE: fn-git-submodule-update
fn-git-submodule-update () 
{
    echo git submodule init
    git submodule init
    echo git submodule update
    git submodule update
}


# USAGE: fn-git-clean 
fn-git-clean () 
{
    echo git clean -f -d
    git clean -f -d
}

# USAGE: fn-git-reset
fn-git-reset () 
{
    echo git reset --hard  $@
    git reset --hard  $@
}

# USAGE: fn-git-clean-submodules
fn-git-clean-submodules () 
{
    echo git submodule foreach git clean -f -d
    git submodule foreach git clean -f -d
}


# USAGE: fn-git-reset-submodules
fn-git-reset-submodules () 
{
    echo git submodule foreach git reset --hard HEAD
    git submodule foreach git reset --hard HEAD
}

# USAGE: fn-build-id BUILD_TYPE
#   BUILD_TYPE: I, M, N
fn-build-id () 
{
     BUILD_TYPE="$1"; shift
     TIMESTAMP=$( date +%Y%m%d-%H%M --date='@'$RAWDATE )
echo ${BUILD_TYPE}${TIMESTAMP}
}

# USAGE: fn-local-repo URL [TO_REPLACE]
#   URL: git://git.eclipse.org/gitroot/platform/eclipse.platform.releng.aggregator.git
#   TO_REPLACE: git://git.eclipse.org
fn-local-repo () 
{
    FORCE_LOCAL_REPO=false
    URL="$1"; shift
    if [ build = $(hostname) -o $FORCE_LOCAL_REPO = true ]; then
        TO_REPLACE='git://git.eclipse.org'
        if [ $# -gt 0 ]; then
            TO_REPLACE="$1"; shift
        fi
        echo $URL | sed "s!$TO_REPLACE!file://!g"
    else
        echo $URL
    fi
}

# USAGE: cat repositories.txt | fn-local-repos [TO_REPLACE]
#   TO_REPLACE: git://git.eclipse.org
fn-local-repos () 
{
    TO_REPLACE='git://git.eclipse.org'
    if [ $# -gt 0 ]; then
        TO_REPLACE="$1"; shift
    fi
    sed "s!$TO_REPLACE!file://!g"
}

# USAGE: fn-git-clone-aggregator GIT_CACHE URL BRANCH 
#   GIT_CACHE: /shared/eclipse/builds/R4_2_maintenance/gitCache
#   URL: file:///gitroot/platform/eclipse.platform.releng.aggregator.git
#   BRANCH: R4_2_maintenance
fn-git-clone-aggregator () 
{
    GIT_CACHE="$1"; shift
    URL="$1"; shift
    BRANCH="$1"; shift
    if [ ! -e "$GIT_CACHE" ]; then
        mkdir -p "$GIT_CACHE"
    fi
    pushd "$GIT_CACHE"
    fn-git-clone "$URL" "$BRANCH"
    popd
    pushd  $(fn-git-dir "$GIT_CACHE" "$URL" )
    fn-git-submodule-update
    popd
}

# USAGE: fn-git-clean-aggregator AGGREGATOR_DIR BRANCH 
#   AGGREGATOR_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   BRANCH: R4_2_maintenance
fn-git-clean-aggregator () 
{
    AGGREGATOR_DIR="$1"; shift
    BRANCH="$1"; shift
    pushd "$AGGREGATOR_DIR"
    fn-git-clean
    fn-git-clean-submodules
    fn-git-reset-submodules
    fn-git-checkout "$BRANCH"
    fn-git-reset origin/$BRANCH
    popd
}

# USAGE: fn-git-cache ROOT BRANCH
#   ROOT: /shared/eclipse/builds
#   BRANCH: R4_2_maintenance
fn-git-cache () 
{
    ROOT="$1"; shift
    BRANCH="$1"; shift
    echo $ROOT/$BRANCH/gitCache
}

# USAGE: fn-git-dir GIT_CACHE URL
#   GIT_CACHE: /shared/eclipse/builds/R4_2_maintenance/gitCache
#   URL: file:///gitroot/platform/eclipse.platform.releng.aggregator.git
fn-git-dir () 
{
    GIT_CACHE="$1"; shift
    URL="$1"; shift
    echo $GIT_CACHE/$( basename "$URL" .git )
}

# USAGE: fn-build-dir ROOT BRANCH BUILD_ID STREAM
#   ROOT: /shared/eclipse/builds
#   BRANCH: R4_2_maintenance
#   BUILD_ID: M20121119-1900
#   STREAM: 4.3.0
# TODO: no longer need branch
fn-build-dir () 
{
    ROOT="$1"; shift
    BRANCH="$1"; shift
    BUILD_ID="$1"; shift
    STREAM="$1"; shift
    eclipseStreamMajor=${STREAM:0:1}
    dropDirSegment=siteDir/eclipse/downloads/dropscbibased
    if [[ $eclipseStreamMajor > 3 ]] 
    then
        dropDirSegment=siteDir/eclipse/downloads/drops4cbibased
    fi
    echo $ROOT/$dropDirSegment/$BUILD_ID
}

# USAGE: fn-basebuilder-dir ROOT BRANCH BASEBUILDER_TAG
#   ROOT: /shared/eclipse/builds
#   BRANCH: R4_2_maintenance
#   BASEBUILDER_TAG: R38M6PlusRC3D
fn-basebuilder-dir () 
{
    ROOT="$1"; shift
    BRANCH="$1"; shift
    BASEBUILDER_TAG="$1"; shift
    echo $ROOT/org.eclipse.releng.basebuilder_$BASEBUILDER_TAG
}



# USAGE: fn-maven-signer-install REPO_DIR LOCAL_REPO
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/org.eclipse.cbi.maven.plugins
#   LOCAL_REPO: /shared/eclipse/builds/R4_2_maintenance/localMavenRepo
fn-maven-signer-install () 
{
    REPO_DIR="$1"; shift
    LOCAL_REPO="$1"; shift
    pushd "$REPO_DIR"
    mvn -f eclipse-jarsigner-plugin/pom.xml \
        clean install \
        -Dmaven.repo.local=$LOCAL_REPO
    popd
}

# USAGE: fn-maven-parent-install REPO_DIR LOCAL_REPO
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   LOCAL_REPO: /shared/eclipse/builds/R4_2_maintenance/localMavenRepo
fn-maven-parent-install () 
{
    REPO_DIR="$1"; shift
    LOCAL_REPO="$1"; shift
    pushd "$REPO_DIR"
    mvn -f eclipse-parent/pom.xml \
        clean install \
        -q \
        -Dmaven.repo.local=$LOCAL_REPO
    popd
}

# USAGE: fn-maven-cbi-install REPO_DIR LOCAL_REPO
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   LOCAL_REPO: /shared/eclipse/builds/R4_2_maintenance/localMavenRepo
fn-maven-cbi-install () 
{
    REPO_DIR="$1"; shift
    LOCAL_REPO="$1"; shift
    pushd "$REPO_DIR"
    mvn -f maven-cbi-plugin/pom.xml \
        clean install \
        -Dmaven.repo.local=$LOCAL_REPO
    popd
}

# USAGE: fn-maven-build-aggregator BUILD_ID REPO_DIR LOCAL_REPO VERBOSE SIGNING UPDATE_BRANDING MAVEN_BREE
#   BUILD_ID: I20121116-0700
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   LOCAL_REPO: /shared/eclipse/builds/R4_2_maintenance/localMavenRepo
#   VERBOSE: true
#   SIGNING: true
#   UPDATE_BRANDING: true
fn-maven-build-aggregator () 
{
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    LOCAL_REPO="$1"; shift
    MARGS="-DbuildId=$BUILD_ID"
    if $VERBOSE; then
        MARGS="$MARGS -X"
    fi
    shift
    if $SIGNING; then
        MARGS="$MARGS -Peclipse-sign"
    fi
    shift
    if $UPDATE_BRANDING; then
        MARGS="$MARGS -Pupdate-branding-plugins"
    fi
    shift
    MARGS="$MARGS ${MAVEN_BREE}"
    echo "DEBUG: BUILD_ID: $BUILD_ID"
    echo "DEBUG: REPO_DIR: $REPO_DIR"
    echo "DEBUG: LOCAL_REPO: $LOCAL_REPO"
    echo "DEBUG: VERBOSE: $VERBOSE"
    echo "DEBUG: UPDATE_BRANDING: $UPDATE_BRANDING"
    echo "DEBUG: MAVEN_BREE: $MAVEN_BREE"
    pushd "$REPO_DIR"
    mvn $MARGS \
        clean install \
        -Dmaven.test.skip=true \
        -Dmaven.repo.local=$LOCAL_REPO
    popd
}

# USAGE: fn-submodule-checkout BUILD_ID REPO_DIR REPOSITORIES_TXT
#   BUILD_ID: M20121116-1100
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   SCRIPT: /shared/eclipse/builds/scripts/git-submodule-checkout.sh
#   REPOSITORIES_TXT: /shared/eclipse/builds/streams/repositories.txt
fn-submodule-checkout () 
{
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    SCRIPT="$1"; shift
    REPOSITORIES_TXT="$1"; shift
    pushd "$REPO_DIR"
    git submodule foreach "/bin/bash $SCRIPT $REPOSITORIES_TXT \$name"
    uninit=$( git submodule | grep "^-" | cut -f2 -d" " | sort -u )
    if [ ! -z "$uninit" ]; then
        echo Some modules are not initialized: $uninit
        return
    fi
    conflict=$( git submodule | grep "^U" | cut -f2 -d" " | sort -u )
    if [ ! -z "$conflict" ]; then
        echo Some modules have conflicts: $conflict
        return
    fi
    adds=$( git submodule | grep "^+" | cut -f2 -d" " )
    if [ -z "$adds" ]; then
        echo No updates for the submodules
        return
    fi
    popd
}

# USAGE: fn-add-submodule-updates REPO_DIR 
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
fn-add-submodule-updates () 
{
    REPO_DIR="$1"; shift
    pushd "$REPO_DIR"
    adds=$( git submodule | grep "^+" | cut -f2 -d" " )
    if [ -z "$adds" ]; then
        echo No updates for the submodules
        return
    fi
    echo git add $adds
    git add $adds
    popd
}

# USAGE: fn-submodule-checkout BUILD_ID REPO_DIR REPOSITORIES_TXT
#   BUILD_ID: M20121116-1100
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   REPOSITORIES_TXT: /shared/eclipse/builds/streams/repositories.txt
fn-tag-build-inputs () 
{
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    REPOSITORIES_TXT="$1"; shift
    pushd "$REPO_DIR"
    git submodule foreach "if grep \"^\${name}:\" $REPOSITORIES_TXT >/dev/null; then git tag $BUILD_ID; $GIT_PUSH origin $BUILD_ID; else echo Skipping \$name; fi"
    git tag $BUILD_ID
    $GIT_PUSH origin $BUILD_ID
    popd
}

# USAGE: fn-pom-version-updater REPO_DIR LOCAL_REPO
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   LOCAL_REPO: /shared/eclipse/builds/R4_2_maintenance/localMavenRepo
fn-pom-version-updater () 
{
    REPO_DIR="$1"; shift
    LOCAL_REPO="$1"; shift

    # fail fast if not set up correctly
    rc=$(fn-check-dir-exists TMP_DIR)
    checkForErrorExit "$rc" "$rc"
    
    report=${TMP_DIR}/pom_${BUILD_ID}.txt
    pushd "$REPO_DIR"
    mvn $MARGS \
        org.eclipse.tycho:tycho-versions-plugin:update-pom \
        -Dmaven.repo.local=$LOCAL_REPO
    changes=$( git status --short -uno | cut -c4- )
    if [ -z "$changes" ]; then
        echo No changes in pom versions
        return
    else
        echo Changes in pom versions
    fi
    popd
}

# USAGE: fn-pom-version-update-with-commit BUILD_ID REPO_DIR LOCAL_REPO
#   BUILD_ID: I20121116-0700
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   LOCAL_REPO: /shared/eclipse/builds/R4_2_maintenance/localMavenRepo
#   VERBOSE: true
#   SIGNING: true
fn-pom-version-update-with-commit () 
{
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    LOCAL_REPO="$1"; shift
    
    # fail fast if not set up correctly
    rc=$(fn-check-dir-exists TMP_DIR)
    checkForErrorExit "$rc" "$rc"
    
    report=${TMP_DIR}/pom_${BUILD_ID}.txt
    MARGS="-DbuildId=$BUILD_ID"
    pushd "$REPO_DIR"
    mvn $MARGS \
        org.eclipse.tycho:tycho-versions-plugin:update-pom \
        -Dmaven.repo.local=$LOCAL_REPO
    changes=$( git status --short -uno | cut -c4- )
    if [ -z "$changes" ]; then
        echo No changes in pom versions
        return
    fi
    repos=$( git status --short -uno | cut -c4- | grep -v pom.xml )
    for CURRENT_REPO in $repos; do
        pushd "$CURRENT_REPO"
        pom_only=$( git status --short -uno | grep -v pom.xml | wc -l )
        if (( pom_only == 0 )); then
            git add $( git status --short -uno | cut -c4- )
            git commit -m "Update pom versions for build $BUILD_ID"
            echo $GIT_PUSH origin HEAD
        else
            echo Unable to update poms for $CURRENT_REPO
        fi
        popd
    done
    echo git add $changes
    git add $changes
    popd
}

# USAGE: fn-gather-repo BUILD_ID REPO_DIR BUILD_DIR
#   BUILD_ID: I20121116-0700
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   BUILD_DIR: /shared/eclipse/builds/R4_2_maintenance/dirs/M20121120-1747
fn-gather-repo () 
{
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    BUILD_DIR="$1"; shift
    REPO_DIR_REPOSITORY=$REPO_DIR/eclipse.platform.repository/target/repository
    if [[ -d "$REPO_DIR_REPOSITORY" ]]
    then
        pushd "$REPO_DIR"
        cp -r eclipse.platform.repository/target/repository $BUILD_DIR
        popd
    else
        echo "   ERROR: $REPO_DIR_REPOSITORY did not exist in fn-gather-repo"
    fi
}

# USAGE: fn-gather-static-drop BUILD_ID REPO_DIR BUILD_DIR
#   BUILD_ID: I20121116-0700
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   BUILD_DIR: /shared/eclipse/builds/R4_2_maintenance/dirs/M20121120-1747
fn-gather-static-drop () 
{
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    BUILD_DIR="$1"; shift
    REPO_DIR_BUILDER=$REPO_DIR/eclipse.platform.releng.tychoeclipsebuilder
    if [[ -d "$REPO_DIR_BUILDER" ]]
    then
        pushd "$REPO_DIR"
        cp -r eclipse.platform.releng.tychoeclipsebuilder/eclipse/publishingFiles/staticDropFiles/* $BUILD_DIR
        cp -r eclipse.platform.releng.tychoeclipsebuilder/eclipse/clickThroughs $BUILD_DIR
        # FIXME workaround the download page temp directory

        # fail fast if not set up correctly
        rc=$(fn-check-dir-exists TMP_DIR)
        checkForErrorExit "$rc" "$rc"

        sed 's!downloads/drops!staging/cbi/drops!g' $BUILD_DIR/download.php >${TMP_DIR}/t1_$$
        mv {TMP_DIR}/t1_$$ $BUILD_DIR/download.php
        popd
    else
        echo "   ERROR: $REPO_DIR_BUILDER did not exist in fn-gather-static-drop"
    fi
}

# USAGE: fn-gather-sdk BUILD_ID REPO_DIR BUILD_DIR
#   BUILD_ID: I20121116-0700
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   BUILD_DIR: /shared/eclipse/builds/R4_2_maintenance/dirs/M20121120-1747
fn-gather-sdk () 
{
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    BUILD_DIR="$1"; shift
    TARGET_PRODUCTS="$REPO_DIR"/eclipse.platform.releng.tychoeclipsebuilder/sdk/target/products
    if [[ -d "$TARGET_PRODUCTS" ]]
    then
        pushd "$TARGET_PRODUCTS"
        cp org.eclipse.sdk.ide-aix.gtk.ppc64.zip "$BUILD_DIR"/eclipse-SDK-${BUILD_ID}-aix-gtk-ppc64.zip
        cp org.eclipse.sdk.ide-aix.gtk.ppc.zip "$BUILD_DIR"/eclipse-SDK-${BUILD_ID}-aix-gtk-ppc.zip
        cp org.eclipse.sdk.ide-hpux.gtk.ia64.zip "$BUILD_DIR"/eclipse-SDK-${BUILD_ID}-hpux-gtk-ia64.zip
        cp org.eclipse.sdk.ide-linux.gtk.ppc.tar.gz "$BUILD_DIR"/eclipse-SDK-${BUILD_ID}-linux-gtk-ppc.tar.gz
        cp org.eclipse.sdk.ide-linux.gtk.ppc64.tar.gz "$BUILD_DIR"/eclipse-SDK-${BUILD_ID}-linux-gtk-ppc64.tar.gz
        cp org.eclipse.sdk.ide-linux.gtk.s390.tar.gz "$BUILD_DIR"/eclipse-SDK-${BUILD_ID}-linux-gtk-s390.tar.gz
        cp org.eclipse.sdk.ide-linux.gtk.s390x.tar.gz "$BUILD_DIR"/eclipse-SDK-${BUILD_ID}-linux-gtk-s390x.tar.gz
        cp org.eclipse.sdk.ide-linux.gtk.x86_64.tar.gz "$BUILD_DIR"/eclipse-SDK-${BUILD_ID}-linux-gtk-x86_64.tar.gz
        cp org.eclipse.sdk.ide-linux.gtk.x86.tar.gz "$BUILD_DIR"/eclipse-SDK-${BUILD_ID}-linux-gtk.tar.gz
        cp org.eclipse.sdk.ide-macosx.cocoa.x86_64.tar.gz "$BUILD_DIR"/eclipse-SDK-${BUILD_ID}-macosx-cocoa-x86_64.tar.gz
        cp org.eclipse.sdk.ide-macosx.cocoa.x86.tar.gz "$BUILD_DIR"/eclipse-SDK-${BUILD_ID}-macosx-cocoa.tar.gz
        cp org.eclipse.sdk.ide-solaris.gtk.sparc.zip "$BUILD_DIR"/eclipse-SDK-${BUILD_ID}-solaris-gtk.zip
        cp org.eclipse.sdk.ide-solaris.gtk.x86.zip "$BUILD_DIR"/eclipse-SDK-${BUILD_ID}-solaris-gtk-x86.zip
        cp org.eclipse.sdk.ide-win32.win32.x86_64.zip "$BUILD_DIR"/eclipse-SDK-${BUILD_ID}-win32-x86_64.zip
        cp org.eclipse.sdk.ide-win32.win32.x86.zip "$BUILD_DIR"/eclipse-SDK-${BUILD_ID}-win32.zip
        popd
    else
        echo "   ERROR: $TARGET_PRODUCTS did not exist in fn-gather-sdks"
    fi
}

# USAGE: fn-gather-platform BUILD_ID REPO_DIR BUILD_DIR
#   BUILD_ID: I20121116-0700
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   BUILD_DIR: /shared/eclipse/builds/R4_2_maintenance/dirs/M20121120-1747
fn-gather-platform () 
{
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    BUILD_DIR="$1"; shift
    TARGET_PRODUCTS="$REPO_DIR"/eclipse.platform.releng.tychoeclipsebuilder/rcp.sdk/target/products
    if [[ -d "$TARGET_PRODUCTS" ]]
    then
        pushd "$TARGET_PRODUCTS"
        cp org.eclipse.rcp.sdk.id-aix.gtk.ppc64.zip "$BUILD_DIR"/eclipse-platform-${BUILD_ID}-aix-gtk-ppc64.zip
        cp org.eclipse.rcp.sdk.id-aix.gtk.ppc.zip "$BUILD_DIR"/eclipse-platform-${BUILD_ID}-aix-gtk-ppc.zip
        cp org.eclipse.rcp.sdk.id-hpux.gtk.ia64.zip "$BUILD_DIR"/eclipse-platform-${BUILD_ID}-hpux-gtk-ia64.zip
        cp org.eclipse.rcp.sdk.id-linux.gtk.ppc.tar.gz "$BUILD_DIR"/eclipse-platform-${BUILD_ID}-linux-gtk-ppc.tar.gz
        cp org.eclipse.rcp.sdk.id-linux.gtk.ppc64.tar.gz "$BUILD_DIR"/eclipse-platform-${BUILD_ID}-linux-gtk-ppc64.tar.gz
        cp org.eclipse.rcp.sdk.id-linux.gtk.s390.tar.gz "$BUILD_DIR"/eclipse-platform-${BUILD_ID}-linux-gtk-s390.tar.gz
        cp org.eclipse.rcp.sdk.id-linux.gtk.s390x.tar.gz "$BUILD_DIR"/eclipse-platform-${BUILD_ID}-linux-gtk-s390x.tar.gz
        cp org.eclipse.rcp.sdk.id-linux.gtk.x86_64.tar.gz "$BUILD_DIR"/eclipse-platform-${BUILD_ID}-linux-gtk-x86_64.tar.gz
        cp org.eclipse.rcp.sdk.id-linux.gtk.x86.tar.gz "$BUILD_DIR"/eclipse-platform-${BUILD_ID}-linux-gtk.tar.gz
        cp org.eclipse.rcp.sdk.id-macosx.cocoa.x86_64.tar.gz "$BUILD_DIR"/eclipse-platform-${BUILD_ID}-macosx-cocoa-x86_64.tar.gz
        cp org.eclipse.rcp.sdk.id-macosx.cocoa.x86.tar.gz "$BUILD_DIR"/eclipse-platform-${BUILD_ID}-macosx-cocoa.tar.gz
        cp org.eclipse.rcp.sdk.id-solaris.gtk.sparc.zip "$BUILD_DIR"/eclipse-platform-${BUILD_ID}-solaris-gtk.zip
        cp org.eclipse.rcp.sdk.id-solaris.gtk.x86.zip "$BUILD_DIR"/eclipse-platform-${BUILD_ID}-solaris-gtk-x86.zip
        cp org.eclipse.rcp.sdk.id-win32.win32.x86_64.zip "$BUILD_DIR"/eclipse-platform-${BUILD_ID}-win32-x86_64.zip
        cp org.eclipse.rcp.sdk.id-win32.win32.x86.zip "$BUILD_DIR"/eclipse-platform-${BUILD_ID}-win32.zip
        popd
    else
        echo "   ERROR: $TARGET_PRODUCTS did not exist in fn-gather-platform"
    fi
}

# USAGE: fn-gather-swt-zips BUILD_ID REPO_DIR BUILD_DIR
#   BUILD_ID: I20121116-0700
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   BUILD_DIR: /shared/eclipse/builds/R4_2_maintenance/dirs/M20121120-1747
fn-gather-swt-zips () 
{
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    BUILD_DIR="$1"; shift
    # TODO: this directory sanity check does not accomplish much, since "binaries/bundles" always
    # exists. Results in "not found" msg. Doubt there's any simple solution.
    SWT_BUNDLES_DIR="$REPO_DIR"/eclipse.platform.swt.binaries/bundles
    if [[ -d "$SWT_BUNDLES_DIR" ]]
    then
        pushd "$SWT_BUNDLES_DIR"
        cp  */target/*.zip "$BUILD_DIR"
        popd
    else
        echo "   ERROR: $SWT_BUNDLES_DIR did not exist in fn-gather-swt-zips"
    fi
}

# USAGE: fn-gather-test-zips BUILD_ID REPO_DIR BUILD_DIR
#   BUILD_ID: I20121116-0700
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   BUILD_DIR: /shared/eclipse/builds/R4_2_maintenance/dirs/M20121120-1747
fn-gather-test-zips () 
{
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    BUILD_DIR="$1"; shift
    TEST_ZIP_DIR="$REPO_DIR"/eclipse.platform.releng.tychoeclipsebuilder/eclipse-junit-tests/target
    if [[ -d "$TEST_ZIP_DIR" ]]
    then 
        pushd "$TEST_ZIP_DIR"
        cp eclipse-junit-tests-bundle.zip "$BUILD_DIR"/eclipse-Automated-Tests-${BUILD_ID}.zip
        TEST_FRAMEWORK_DIR=$TEST_ZIP_DIR/eclipse-test-framework
        if [[ -d "$TEST_FRAMEWORK_DIR" ]]
        then
            pushd "$TEST_FRAMEWORK_DIR"
            zip -r "$BUILD_DIR"/eclipse-test-framework-${BUILD_ID}.zip *
            popd
        else
            echo "   ERROR: $TEST_FRAMEWORK_DIR did not exist in fn-gather-test-zips." 
        fi
        popd
    else
        echo "   ERROR: $TEST_ZIP_DIR did not exist in fn-gather-test-zips."
    fi
}

# USAGE: fn-slice-repos BUILD_ID REPO_DIR BUILD_DIR BASEBUILDER_LAUNCHER
#   BUILD_ID: I20121116-0700
#   ANT_SCRIPT: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   BUILD_DIR: /shared/eclipse/builds/R4_2_maintenance/dirs/M20121120-1747
#   BASEBUILDER_LAUNCHER: /shared/eclipse/builds/R4_2_maintenance/org.eclipse.releng.basebuilder_R3_7/plugins/org.eclipse.equinox.launcher_1.2.0.v20110502.jar
fn-slice-repos () 
{
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    BUILD_DIR="$1"; shift
    BASEBUILDER_LAUNCHER="$1"; shift
    ANT_SCRIPT="$REPO_DIR"/eclipse.platform.releng.tychoeclipsebuilder/repos/buildAll.xml
    REPO_DIR_DIR="$REPO_DIR"/eclipse.platform.repository/target/repository
    if [[ -d "$REPO_DIR_DIR" ]]
    then
        pushd "$REPO_DIR"
        java -jar "$BASEBUILDER_LAUNCHER" \
            -application org.eclipse.ant.core.antRunner \
            -buildfile "$ANT_SCRIPT" \
            -Declipse.build.configs="$REPO_DIR"/eclipse.platform.releng.tychoeclipsebuilder \
            -DbuildId="$BUILD_ID" \
            -DbuildRepo="$REPO_DIR_DIR" \
            -DpostingDirectory=$(dirname "$BUILD_DIR") \
            -DequinoxPostingDirectory="$BUILD_ROOT/siteDir/equinox/drops" \
            -DbuildLabel="$BUILD_ID" \
            -DbuildDirectory="$BUILD_DIR"
        popd
    else
        echo "   ERROR: $REPO_DIR_DIR did not exist in fn-slice-repo"
    fi
}


# USAGE: fn-gather-repo-zips BUILD_ID REPO_DIR BUILD_DIR
#   BUILD_ID: I20121116-0700
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   BUILD_DIR: /shared/eclipse/builds/R4_2_maintenance/dirs/M20121120-1747
fn-gather-repo-zips () 
{
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    BUILD_DIR="$1"; shift
    if [[ -d "$REPO_DIR" ]]
    then
        pushd "$REPO_DIR"/eclipse.platform.repository/target/repos
        for r in org.eclipse.*; do
            pushd $r
            zip -r "$BUILD_DIR"/${r}-${BUILD_ID}.zip * 
            popd
        done
        popd
    else
        echo "   ERROR: $REPO_DIR did not exist in fn-gather-repo-zips"
    fi
}

# USAGE: fn-gather-compile-logs BUILD_ID REPO_DIR BUILD_DIR
#   BUILD_ID: I20121116-0700
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   BUILD_DIR: /shared/eclipse/builds/R4_2_maintenance/dirs/M20121120-1747
fn-gather-compile-logs () 
{
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    BUILD_DIR="$1"; shift
    if [[ -d "$REPO_DIR" ]]
    then
        mkdir -p "$BUILD_DIR"/compilelogs/plugins
        pushd "$REPO_DIR"
        for dot in $( find * -name "@dot.xml" ); do
            echo "Processing $dot" # org.eclipse.e4.core.di/target/@dot.xml
            targetDir=$( dirname "$dot" )
            if [ ! -r "$targetDir"/MANIFEST.MF ]; then
                echo "**Failed to process $dot"
            else
                BUNDLE_ID=$( grep Bundle-SymbolicName "$targetDir"/MANIFEST.MF | cut -f2 -d" " |  cut -f1 -d\; | tr -d '\f\r\n\t' )
                BUNDLE_VERSION=$(  grep Bundle-Version "$targetDir"/MANIFEST.MF | cut -f2 -d" " | tr -d '\f\r\n\t' )
                mkdir "$BUILD_DIR"/compilelogs/plugins/${BUNDLE_ID}_${BUNDLE_VERSION}
                cp "$dot" "$BUILD_DIR"/compilelogs/plugins/${BUNDLE_ID}_${BUNDLE_VERSION}
            fi
        done
        popd
    else
        echo "   ERROR: $REPO_DIR did not exist in fn-gather-compile-logs"
    fi
}


# USAGE: fn-gather-main-index BUILD_ID REPO_DIR BUILD_DIR STREAM BUILD_TYPE BUILD_PRETTY_DATE
#   BUILD_ID: I20121116-0700
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   BUILD_DIR: /shared/eclipse/builds/R4_2_maintenance/dirs/M20121120-1747
#   STREAM: 4.2.2
#   BUILD_TYPE: M, I, N
#   BUILD_PRETTY_DATE: Thu Nov 20 17:47:35 EST 2012
fn-gather-main-index () 
{
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    BUILD_DIR="$1"; shift
    STREAM="$1"; shift
    BUILD_TYPE="$1"; shift
    # BUILD_TYPE_NAME=Integration
    #if [ "$BUILD_TYPE" = M ]; then
        #    BUILD_TYPE_NAME=Maintenance
    #fi
    BUILD_PRETTY_DATE="$1"; shift
    pushd "$REPO_DIR"/eclipse.platform.releng.tychoeclipsebuilder/eclipse/templateFiles

    # Simplified by creating PHP variables in buildproperties.php
    cp "index.php.template" "$BUILD_DIR"/index.php

    #    # fail fast if not set up correctly
    #rc=$(fn-check-dir-exists TMP_DIR)
    #checkForErrorExit "$rc" "$rc"

    #T1=${TMP_DIR}/t1_$$
    #T2=${TMP_DIR}/t2_$$
    #sed "s/@eclipseStream@/$STREAM/g" index.php.template >$T1
    #sed "s/@type@/$BUILD_TYPE_NAME/g" $T1 >$T2
    #sed "s/@build@/$BUILD_ID/g" $T2 >$T1
    #sed "s/@date@/$BUILD_PRETTY_DATE/g" $T1 >$T2
    #sed "s/@buildlabel@/$BUILD_ID/g" $T2 >$T1
    #cp $T1 "$BUILD_DIR"/index.php
    #rm $T1 $T2
    popd
}

# USAGE: fn-parse-compile-logs BUILD_ID ANT_SCRIPT BUILD_DIR BASEBUILDER_LAUNCHER
#   BUILD_ID: I20121116-0700
#   ANT_SCRIPT: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   BUILD_DIR: /shared/eclipse/builds/R4_2_maintenance/dirs/M20121120-1747
#   BASEBUILDER_LAUNCHER: /shared/eclipse/builds/R4_2_maintenance/org.eclipse.releng.basebuilder_R3_7/plugins/org.eclipse.equinox.launcher_1.2.0.v20110502.jar
fn-parse-compile-logs () 
{
    BUILD_ID="$1"; shift
    ANT_SCRIPT="$1"; shift
    BUILD_DIR="$1"; shift
    BASEBUILDER_LAUNCHER="$1"; shift
    pushd "$BUILD_DIR"
    java -jar "$BASEBUILDER_LAUNCHER" \
        -application org.eclipse.ant.core.antRunner \
        -buildfile "$ANT_SCRIPT" \
        -DbuildDirectory=$(dirname "$BUILD_DIR" ) \
        -DpostingDirectory=$(dirname "$BUILD_DIR" ) \
        -DbuildId="$BUILD_ID" \
        -DbuildLabel="$BUILD_ID" \
        verifyCompile
    popd
}

# USAGE: fn-publish-eclipse BUILD_TYPE BUILD_STREAM BUILD_ID REPO_DIR BUILD_DIR BASEBUILDER_LAUNCHER
#   BUILD_TYPE: I
#   BUILD_STREAM: 4.2.2
#   BUILD_ID: I20121116-0700
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   BUILD_DIR: /shared/eclipse/builds/R4_2_maintenance/dirs/M20121120-1747
#   BASEBUILDER_LAUNCHER: /shared/eclipse/builds/R4_2_maintenance/org.eclipse.releng.basebuilder_R3_7/plugins/org.eclipse.equinox.launcher_1.2.0.v20110502.jar
fn-publish-eclipse () 
{
    BUILD_TYPE="$1"; shift
    BUILD_STREAM="$1"; shift
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    BUILD_DIR="$1"; shift
    BASEBUILDER_LAUNCHER="$1"; shift
    pushd "$BUILD_DIR"
    java -jar "$BASEBUILDER_LAUNCHER" \
        -application org.eclipse.ant.core.antRunner \
        -v \
        -buildfile "$REPO_DIR"/eclipse.platform.releng.tychoeclipsebuilder/eclipse/helper.xml \
        -Declipse.build.configs="$REPO_DIR"/eclipse.platform.releng.tychoeclipsebuilder \
        -DbuildId="$BUILD_ID" \
        -DbuildRepo="$REPO_DIR"/eclipse.platform.repository/target/repository \
        -DpostingDirectory=$(dirname "$BUILD_DIR") \
        -DequinoxPostingDirectory="$BUILD_ROOT/siteDir/equinox/drops" \
        -DpublishingContent="$REPO_DIR"/eclipse.platform.releng.tychoeclipsebuilder/eclipse/publishingFiles \
        -DbuildLabel="$BUILD_ID" \
        -Dhudson=true \
        -DeclipseStream=$BUILD_STREAM \
        -DbuildType="$BUILD_TYPE" \
        -Dbase.builder=$(dirname $(dirname "$BASEBUILDER_LAUNCHER" ) ) \
        -DbuildDirectory=$(dirname "$BUILD_DIR") \
        publish
    popd
}

# USAGE: fn-checkout-basebuilder BUILDER_DIR BASEBUILDER_TAG
#   BUILDER_DIR: /shared/eclipse/builds/R4_2_maintenance/org.eclipse.releng.basebuilder_R3_7
#   BASEBUILDER_TAG: R3_7
fn-checkout-basebuilder () 
{
    BUILDER_DIR="$1"; shift
    BASEBUILDER_TAG="$1"; shift
    if [ -e "$BUILDER_DIR" ]; then
        return
    fi
    pushd $( dirname "$BUILDER_DIR" )
    wget --no-verbose -O basebuilder-${BASEBUILDER_TAG}.zip http://git.eclipse.org/c/platform/eclipse.platform.releng.basebuilder.git/snapshot/eclipse.platform.releng.basebuilder-${BASEBUILDER_TAG}.zip 2>&1
    unzip -q basebuilder-${BASEBUILDER_TAG}.zip
    mv eclipse.platform.releng.basebuilder-${BASEBUILDER_TAG} "$BUILDER_DIR"
    rm basebuilder-${BASEBUILDER_TAG}.zip
    popd
}

# USAGE: fn-basebuilder-launcher BUILDER_DIR
#   BUILDER_DIR: /shared/eclipse/builds/R4_2_maintenance/org.eclipse.releng.basebuilder_R3_7
fn-basebuilder-launcher () 
{
    BUILDER_DIR="$1"; shift
    find "$BUILDER_DIR" -name "org.eclipse.equinox.launcher_*.jar" | tail -1
}

# USAGE: fn-pom-version-report BUILD_ID REPO_DIR BUILD_DIR LOCAL_REPO
#   BUILD_ID: I20121116-0700
#   REPO_DIR: /shared/eclipse/builds/R4_2_maintenance/gitCache/eclipse.platform.releng.aggregator
#   BUILD_DIR: /shared/eclipse/builds/R4_2_maintenance/dirs/M20121120-1747
fn-pom-version-report () 
{
    BUILD_ID="$1"; shift
    REPO_DIR="$1"; shift
    BUILD_DIR="$1"; shift
    pushd "$REPO_DIR"
    mkdir -p "$BUILD_DIR"/pom_updates
    git submodule foreach "if (git status -s -uno | grep pom.xml >/dev/null ); then git diff >$BUILD_DIR/pom_updates/\$name.diff; fi "
    pushd "$BUILD_DIR"/pom_updates
    echo "<html>"  >index.html 
    echo "<head>"  >>index.html
    echo "<title>POM version report for $BUILD_ID</title>"  >>index.html
    echo "</head>"  >>index.html
    echo "<body>"  >>index.html
    echo "<h1>POM version report for $BUILD_ID</h1>"  >>index.html
    echo "<p>These repositories need patches to bring their pom.xml files up to the correct version.</p>"  >>index.html
    echo "<ul>"  >>index.html

    for f in *.diff; do
        FNAME=$( basename $f .diff )
        echo "<li><a href=\"$f\">$FNAME</a></li>" >> index.html
    done
    echo "</ul>" >> index.html
    echo "</html>" >> index.html
    popd
    popd
}

# USAGE: fn-check-dir-exists DIR_VAR_NAME
#   DIR_VAR_NAME: JAVA_HOME (not, $JAVA_HOME, for better error messages)
#   callers should check for non-zero returned value, which itself is suitable for message, 
#   but must be quoted. Such as
#        rc=${fn-check-dir-exists JAVA_HOME)
#        checkForErrorExit "$rc" "$rc"
fn-check-dir-exists () 
{
    DIR_VAR_NAME=$1
    if [[ -z "${!DIR_VAR_NAME}" ]]
    then
        echo "DIR_VAR_NAME, ${DIR_VAR_NAME}, must be defined before running this script."
    else 
        if [[ ! -d  "${!DIR_VAR_NAME}" ]]
        then
            echo "The directory DIR_VAR_NAME, ${DIR_VAR_NAME} (\"${!DIR_VAR_NAME}\"), must exist before running this script."
        else
            echo 0
        fi
    fi
}

# USAGE: fn-write-property VAR_NAME
#   VAR_NAME: Variable name to write as "variable=value" form
# This script assumes the following variables have been defined and are pointing 
# to an appropriate file (see master-build.sh): 
# BUILD_ENV_FILE=${buildDirectory}/buildproperties.shsource
# BUILD_ENV_FILE_PHP=${buildDirectory}/buildproperties.php
# BUILD_ENV_FILE_PROP=${buildDirectory}/buildproperties.properties

# Note we always append to file, assuming if doesn't exist yet and will be 
# created, and for each build, it won't exist, so will be written fresh for 
# each build. 

# TODO: Could add some sanity checking of if variable name appropriate 
# for various language (e.g. I forget all the rules, but bash variables 
# can not start with numerial, PHP variables (or is it Ant) can't have hyphens
# (or is it underscore :), etc. But may need to add some mangling, or warning? 
# Similarly, not sure at the moment of what to 
# write if value is null/empty. For now will leave empty string, but some might need blank?
# Or literally nothing? Also, unsure of effects of full quoting or if always needed? 

fn-write-property () 
{
    VAR_NAME=$1
    if [[ -z "${VAR_NAME}" ]]
    then
        echo "VAR_NAME must be passed to this script, $0."
        return 1
     fi 

     # bash scripts (export may be overkill ... but, just in case needed)
     echo "export ${VAR_NAME}=\"${!VAR_NAME}\"" >> $BUILD_ENV_FILE
     # PHP, suitable for direct "include"
     echo "\$${VAR_NAME} = \"${!VAR_NAME}\";" >> $BUILD_ENV_FILE_PHP
     # standard properties file
     echo "${VAR_NAME} = \"${!VAR_NAME}\"" >> $BUILD_ENV_FILE_PROP

}

# USAGE: fn-write-property-init
# Must be called (exactly) once before writing properties. 
fn-write-property-init () 
{

     # nothing really required for bash shsource, but we'll put in some niceties
     echo "#!/usr/bin/env bash" > $BUILD_ENV_FILE
     echo "# properties written for $BUILD_ID" >> $BUILD_ENV_FILE
     # PHP, suitable for direct "include": needs to start and end with <?php ... ?>
     echo "<?php " > $BUILD_ENV_FILE_PHP
     echo "// properties written for $BUILD_ID " >> $BUILD_ENV_FILE_PHP
     # standard properties file: nothing special required
     echo "! properties written for $BUILD_ID" > $BUILD_ENV_FILE_PROP

}

# USAGE: fn-write-property-close
# Must be called (exactly) once when completely finished writing properties. 
fn-write-property-close () 
{

     # nothing really required for bash shsource, but we'll put in some niceties
     echo "# finished properties for $BUILD_ID" >> $BUILD_ENV_FILE
     # PHP, suitable for direct "include": needs to start and end with <?php ... ?>
     # Note: technically may not need closing ?> for an 'include' ? 
     echo "// finished properties for $BUILD_ID " >> $BUILD_ENV_FILE_PHP
     echo "?>"  >> $BUILD_ENV_FILE_PHP
     # standard properties file: nothing special required
     echo "! finshed properties for $BUILD_ID" >> $BUILD_ENV_FILE_PROP

}

