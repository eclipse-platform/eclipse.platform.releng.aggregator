#!/usr/bin/env bash

#
# This script makes it easier to launch various Derby tools
# Usage: "sh derby.sh <command>"
#     where <command> is one of "start, stop, ij, look"
#
function usage
{
  printf "\n\tUsage: \t%s\n" "${0##//} <command>"
  printf "\t\t%s\n" "Where <command> is one of start, stop, ij, runtimeinfo, ping, sysinfo, help, serverhelp, dblookhelp "
}

# this localBuildProperties.shsource file is to ease local builds to override some variables.
# It should not be used for production builds.
source localBuildProperties.shsource 2>/dev/null

if [[ "${#}" -ne "1" ]]
then
  printf "\n\tERROR: \t%s\n" "Wrong number of arguments."
  usage
  exit 1
fi

export DERBY_INSTALL=/shared/eclipse/derby/derby10.11.1.1

# where the Derby libraries reside
export CSLIB=${DERBY_INSTALL}/lib

# where the DBs live
export DBROOT=/shared/eclipse/databases

# name of the default DB
export DBNAME=perfDB

# the Java VM
export JAVA_HOME=/shared/common/jdk1.7.0-latest
export JAVACMD=${JAVA_HOME}/bin/java

#MacOS: NSC="-Dderby.system.home=$DBROOT -Dderby.storage.fileSyncTransactionLog=true org.apache.derby.drda.NetworkServerControl"
# besides databases, DBROOT contains derby.properties
export NSC="-Dderby.system.home=${DBROOT} -jar $DERBY_INSTALL/lib/derbyrun.jar"

export CLASSPATH="${CSLIB}/derby.jar:${CSLIB}/derbytools.jar:${CSLIB}/derbynet.jar:${CSLIB}/derbyrun.jar:${CSLIB}/derbyclient.jar"
export eclipse_perf_dbloc_value=${eclipse_perf_dbloc_value:-"//172.25.25.57:1527"}

# Port and HOST also need to be defined in /shared/eclipse/databases/derby.properties, for example:
#derby.drda.host=192.168.1.10
#derby.drda.portNumber=1527
#derby.drda.startNetworkServer=true
#derby.system.bootAll=true
#derby.drda.keepAlive=true

case $1 in
  start )
    # temporarily use 'noSecurityManager'
    $JAVACMD -Xms256M -Xmx256M $NSC server start -noSecurityManager 1>derbyout.log.txt 2>derbyerr.log.txt &
    RC=$?
    if [[ $RC != 0 ]]
    then
      echo "derby start returned non-zero return code. RC: $RC"
    fi
    ;;

  help )
    $JAVACMD $NSC
    ;;

  serverhelp )
    $JAVACMD $NSC server
    ;;

  dblookhelp )
    $JAVACMD $NSC dblook
    ;;

  ping )
    $JAVACMD $NSC server ping
    ;;

  runtimeinfo )
    $JAVACMD $NSC server runtimeinfo
    ;;

  stop )
    $JAVACMD $NSC server shutdown
    ;;

  ij )
    $JAVACMD $NSC ij
    #$JAVACMD -Dij.protocol=jdbc:derby: -Dij.database=$DBROOT/$DBNAME org.apache.derby.tools.ij
    ;;

    # need work, here
  look )
    #$JAVACMD com.ihost.cs.tools.cslook -d jdbc:derby:$DBROOT/$DBNAME
    $JAVACMD $NSC dblook -d jdbc:derby:${eclipse_perf_dbloc_value}/${DBROOT}/${DBNAME}
    ;;

  sysinfo )
    $JAVACMD $NSC sysinfo
    ;;

  * )
    printf "\n\tERROR: \t%s\n" "unknown command: $1"
    usage
    ;;
esac

