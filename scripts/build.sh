#!/bin/bash

clean up "dirt" from previous build
# see Bug 420078
git submodule foreach git clean -f -d -x
git submodule foreach git reset --hard HEAD
git clean -f -d -x
git reset --hard HEAD

# update master and submodules
git checkout master
git pull --recurse-submodules
git submodule update

# run the build
mvn clean verify

# find the results in
# eclipse.platform.releng.tychoeclipsebuilder/sdk/target/products/*
