#!/bin/bash
# this is not really to be executed

# USAGE: fn-git-clone URL [BRANCH [TARGET_DIR] ]
#   URL: file:///gitroot/platform/eclipse.platform.releng.aggregator.git
#   BRANCH: R4_2_maintenance 
#   TARGET_DIR: e.p.releng.aggregator
fn-git-clone () {
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
fn-git-checkout () {
	BRANCH="$1"; shift
	echo git checkout "$BRANCH"
	git checkout "$BRANCH"
}

# USAGE: fn-git-pull
fn-git-pull () {
	echo git pull
	git pull
}

# USAGE: fn-git-submodule-update
fn-git-submodule-update () {
	echo git submodule init
	git submodule init
	echo git submodule update
	git submodule update
}


# USAGE: fn-git-clean 
fn-git-clean () {
	echo git clean -f -d
	git clean -f -d
}

# USAGE: fn-git-reset
fn-git-reset () {
	echo git reset --hard  HEAD
	git reset --hard  HEAD
}

# USAGE: fn-git-clean-submodules
fn-git-clean-submodules () {
	echo git submodule foreach git clean -f -d
	git submodule foreach git clean -f -d
}


# USAGE: fn-git-reset-submodules
fn-git-reset-submodules () {
	echo git submodule foreach git reset --hard HEAD
	git submodule foreach git reset --hard HEAD
}

# USAGE: fn-build-id BUILD_TYPE
#   BUILD_TYPE: I, M, N
fn-build-id () {
	BUILD_TYPE="$1"; shift
	echo $BUILD_TYPE$(date +%Y%m%d)-$(date +%H%M)
}

# USAGE: fn-local-repo URL [TO_REPLACE]
#   URL: git://git.eclipse.org/gitroot/platform/eclipse.platform.releng.aggregator.git
#   TO_REPLACE: git://git.eclipse.org
fn-local-repo () {
	TO_REPLACE='git://git.eclipse.org'
	URL="$1"; shift
	if [ $# -gt 0 ]; then
		TO_REPLACE="$1"; shift
	fi
	echo $URL | sed "s!$TO_REPLACE!file://!g"
}

# USAGE: cat repositories.txt | fn-local-repos [TO_REPLACE]
#   TO_REPLACE: git://git.eclipse.org
fn-local-repos () {
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
fn-git-clone-aggregator () {
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
fn-git-clean-aggregator () {
	AGGREGATOR_DIR="$1"; shift
	BRANCH="$1"; shift
	pushd "$AGGREGATOR_DIR"
	fn-git-clean
	fn-git-reset
	fn-git-clean-submodules
	fn-git-reset-submodules
	fn-git-checkout "$BRANCH"
	popd
}

# USAGE: fn-git-cache ROOT BRANCH
#   ROOT: /shared/eclipse/builds
#   BRANCH: R4_2_maintenance
fn-git-cache () {
	ROOT="$1"; shift
	BRANCH="$1"; shift
	echo $ROOT/$BRANCH/gitCache
}

# USAGE: fn-git-dir GIT_CACHE URL
#   GIT_CACHE: /shared/eclipse/builds/R4_2_maintenance/gitCache
#   URL: file:///gitroot/platform/eclipse.platform.releng.aggregator.git
fn-git-dir () {
	GIT_CACHE="$1"; shift
	URL="$1"; shift
	echo $GIT_CACHE/$( basename "$URL" .git )
}


