#!/bin/bash

#*******************************************************************************
# Copyright (c) 2011 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# Contributors:
#     IBM Corporation - initial API and implementation
#*******************************************************************************

baseBuilder=/shared/eclipse/e4/build/e4/org.eclipse.releng.basebuilder
launcherJar=$( find $baseBuilder/ -name "org.eclipse.equinox.launcher_*.jar" | sort | head -1 )
java=/shared/common/jdk-1.5.0-22.x86_64/jre/bin/java

remoteBase=/home/data/httpd/download.eclipse.org

e4Builds=/shared/eclipse/e4/build/e4/downloads/drops/4.0.0
sdkBuilds=/shared/eclipse/e4/build/e4/downloads/drops/4.0.0/40builds
orionBuilds=/shared/eclipse/e4/orion

generateCleanupXML() {
	dash=""
	if [ ! -z "$1" ]; then
		dash=$1
	fi

        cat > cleanupScript.xml << "EOF"
<project>
        <target name="cleanup">
                <p2.composite.repository destination="file:${compositeRepo}">
			<remove>
EOF
        for f in $builds; do 
		d=${f:0:9}
                t=${f:9}
		echo "				<repository location=\"$d$dash$t\" />" >> cleanupScript.xml
        done
cat >> cleanupScript.xml << "EOF"
			</remove>
                </p2.composite.repository>
        </target>
</project>
EOF

}

clean() {
	project=$1
	prefix=$2
	compositeRepo=$3

	user=pwebster
	drops=drops
	createIndex=createIndex.php
	case "$project" in
		"e4")
			buildDir=$e4Builds
			p2RepoRoot=e4/updates
			urlFragment=e4/downloads;;
		"sdk") 
			buildDir=$sdkBuilds
			p2RepoRoot=eclipse/updates
			drops=drops4
			createIndex=createIndex4x.php
			urlFragment=eclipse/downloads;;
		"orion")
			buildDir=$orionBuilds
			user=aniefer
			p2RepoRoot=orion/updates
			urlFragment=orion;;
	esac

        pushd $buildDir

        builds=$( ls --format=single-column -d $prefix* | sort | head -n-3 )

	if [[ ! -z $builds ]]; then
		#remove from p2 composite repository
		generateCleanupXML
		$java -jar $launcherJar -data "/opt/buildhomes/e4Build/cleanupworkspaces/workspace-cleanup" -application org.eclipse.ant.core.antRunner -f cleanupScript.xml cleanup -DcompositeRepo=$compositeRepo

		for f in $builds; do
			rm -rf $f						#delete from build directory
			ssh $user@build.eclipse.org rm -rf $remoteBase/$urlFragment/$drops/$f		#delete from dev.eclipse.org drops
			rm -rf $compositeRepo/$f				#delete from composite repo

			case "$project" in
			"e4")
				rm -rf targets/$f targets/$f-p2;;
			"sdk")
				rm -rf /shared/eclipse/e4/sdk/$f $sdkBuilds/targets/helios-repo-$f;;
			"orion")
				d=${f:0:9}
				t=${f:9}
				rm -rf tests/$d-$t $compositeRepo/$d-$t;;
			esac
		done

		#update website index and rsync the repo
		if [[ $project != "orion" ]]; then
			wget -O index.txt http://download.eclipse.org/$urlFragment/$createIndex
			scp index.txt $user@build.eclipse.org:$remoteBase/$urlFragment/index.html
			rm index.txt
		fi
		rsync --delete --recursive $compositeRepo $user@build.eclipse.org:$remoteBase/$p2RepoRoot
	fi
	popd
}

clean e4    I $e4Builds/targets/updates/0.12-I-builds
clean e4    M $e4Builds/targets/updates/0.11-I-builds
clean sdk   I $e4Builds/targets/updates/4.2-I-builds
clean sdk   M $e4Builds/targets/updates/4.1-I-builds
clean orion I $orionBuilds/target/integration
