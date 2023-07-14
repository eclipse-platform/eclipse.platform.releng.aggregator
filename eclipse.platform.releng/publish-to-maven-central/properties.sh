#*******************************************************************************
# Copyright (c) 2016, 2019 GK Software SE and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Stephan Herrmann - initial API and implementation
#********************************************************************************

# ECLIPSE:
DROPS4=/home/data/httpd/download.eclipse.org/eclipse/downloads/drops4
SDK_BUILD_DIR=R-4.28-202306050440
SDK_VERSION=4.28
FILE_ECLIPSE=${DROPS4}/${SDK_BUILD_DIR}/eclipse-SDK-${SDK_VERSION}-linux-gtk-x86_64.tar.gz

# AGGREGATOR:
URL_AGG_UPDATES=https://download.eclipse.org/cbi/updates/p2-aggregator/products/nightly/latest

# LOCAL TOOLS:
LOCAL_TOOLS=${WORKSPACE}/tools
DIR_AGGREGATOR=aggregator
AGGREGATOR=${LOCAL_TOOLS}/${DIR_AGGREGATOR}/cbiAggr
ECLIPSE=${LOCAL_TOOLS}/eclipse/eclipse

# ENRICH POMS tool:
ENRICH_POMS_JAR=${WORKSPACE}/work/EnrichPoms.jar
ENRICH_POMS_PACKAGE=org.eclipse.platform.releng.maven.pom

# AGGREGATION MODEL:
FILE_SDK_AGGR=${WORKSPACE}/work/SDK4Mvn.aggr
