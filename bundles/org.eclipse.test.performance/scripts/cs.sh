#!/bin/sh

# 
# This script makes it easier to launch various cloudscape tools
# Usage: "cs <command>"
#     where <command> is one of "start, stop, ij, look"
#

# the root of the Cloudscape directory 
CLOUDSCAPE_INSTALL=/Volumes/Stuff/Java/Cloudscape_10.0

# where DB live
DBROOT=/tmp/cloudscape

# name of the default DB
DBNAME=perfDB

# the Java VM
JAVA=/usr/bin/java


CSL=${CLOUDSCAPE_INSTALL}/lib
NSC="$JAVA -Dcloudscape.system.home=$DBROOT com.ihost.cs.drda.NetworkServerControl"

export CLASSPATH="${CSL}/cs.jar:${CSL}/cstools.jar:${CSL}/csnet.jar:${CLASSPATH}"

case $1 in
	start )
		$NSC start
		break;;
		
	stop )
		$NSC shutdown
		break;;

	ij )
		$JAVA -Dij.protocol=jdbc:cloudscape: -Dij.database=$DBROOT/$DBNAME com.ihost.cs.tools.ij
		break;;
	
	look )
		$JAVA com.ihost.cs.tools.cslook -d jdbc:cloudscape:$DBROOT/$DBNAME
		break;;
			
	* )
		echo "unknown command $1"
		break ;;
esac
