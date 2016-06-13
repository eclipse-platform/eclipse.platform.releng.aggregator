#!/usr/bin/env bash
SCRIPT_PATH=$PWD
export BUILD_ENV_FILE=buildproperties.shsource
export BUILD_ENV_FILE_PHP=buildproperties.php
export BUILD_ENV_FILE_PROP=buildproperties.properties

source /home/davidw/gitLuna/eclipse.platform.releng.aggregator/production/build-functions.shsource
source /home/davidw/gitLuna/eclipse.platform.releng.aggregator/production/bashUtilities.shsource
fn-write-property-init

export JAVA_DOC_PROXIES=${JAVA_DOC_PROXIES:-"-J-Dhttps.proxyHost=proxy.eclipse.org -J-Dhttps.proxyPort=9898 -J-Dhttps.nonProxyHosts=\"172.30.206.*\""}

echo "JAVA_DOC_PROXIES: $JAVA_DOC_PROXIES"

fn-write-property JAVA_DOC_PROXIES

fn-write-property-close
