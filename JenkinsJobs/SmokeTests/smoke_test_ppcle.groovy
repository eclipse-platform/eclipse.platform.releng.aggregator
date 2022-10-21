job('SmokeTests/ep-smoke-test-ppcle'){

  logRotator {
    numToKeep(10)
  }

  parameters {
    stringParam('buildId', null, 'Build Id to test (such as I20120717-0800, N20120716-0800). ')
    stringParam('javaDownload', 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17%2B35/OpenJDK17-jdk_ppc64le_linux_hotspot_17_35.tar.gz', 'fully qualified link to java download')
    stringParam('testsToRun', 'ui', 'This can be any ant target from https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/blob/master/production/testScripts/configuration/sdk.tests/testScripts/test.xml')
    stringParam('secManager', '-Djava.security.manager=allow', null)
  }

  concurrentBuild()

  label('ppctest')

  jdk('openjdk-jdk17-latest')

  authenticationToken('windows2012tests')
 
  wrappers { //adds pre/post actions
    timestamps()
    preBuildCleanup()
    timeout {
      absolute(60)
    }
    xvnc {
      useXauthority()
    }
    withAnt {
      installation('apache-ant-latest')
    }
  }
  
  steps {
    shell('''
#!/usr/bin/env bash

buildId=$(echo $buildId|tr -d ' ')



RAW_DATE_START="$(date +%s )"

echo -e "\\n\\tRAW Date Start: ${RAW_DATE_START} \\n"

echo -e "\\n\\t whoami:  $( whoami )\\n"
echo -e "\\n\\t uname -a: $(uname -a)\\n"


# 0002 is often the default for shell users, but it is not when ran from
# a cron job, so we set it explicitly, to be sure of value, so releng group has write access to anything
# we create on shared area.
oldumask=$(umask)
umask 0002
echo "umask explicitly set to 0002, old value was $oldumask"

# we want java.io.tmpdir to be in $WORKSPACE, but must already exist, for Java to use it.
mkdir -p tmp

wget -O getEBuilder.xml --no-verbose https://download.eclipse.org/eclipse/relengScripts/production/testScripts/hudsonBootstrap/getEBuilder.xml 2>&1
curl -o buildproperties.shsource https://download.eclipse.org/eclipse/downloads/drops4/${buildId}/buildproperties.shsource
cat buildproperties.shsource
source buildproperties.shsource
set +x

set -x
mkdir -p ${WORKSPACE}/java
pushd ${WORKSPACE}/java
wget -O jdk.tar.gz --no-verbose ${javaDownload}
tar xzf jdk.tar.gz
rm jdk.tar.gz
export JAVA_HOME=$(pwd)/$(ls)

popd
set +x

#export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.252.b09-2.el8_1.ppc64le/jre
export PATH=${JAVA_HOME}/bin:${PATH}

echo JAVA_HOME: $JAVA_HOME
echo PATH: $PATH

export ANT_OPTS="${ANT_OPTS} -Djava.io.tmpdir=${WORKSPACE}/tmp ${secMananger}"
export eclipseArch=ppc64le

env 1>envVars.txt 2>&1
ant -diagnostics 1>antDiagnostics.txt 2>&1
java -XshowSettings -version 1>javaSettings.txt 2>&1

ant -f getEBuilder.xml -Djava.io.tmpdir=${WORKSPACE}/tmp -DbuildId=$buildId  -DeclipseStream=${STREAM} -DEBUILDER_HASH=${EBUILDER_HASH}  -DdownloadURL=https://download.eclipse.org/eclipse/downloads/drops4/${buildId}  -Dosgi.os=linux -Dosgi.ws=gtk -Dosgi.arch=ppc64le -DtestSuite=${testsToRun}

RAW_DATE_END="$(date +%s )"

echo -e "\\n\\tRAW Date End: ${RAW_DATE_END} \\n"

TOTAL_TIME=$((${RAW_DATE_END} - ${RAW_DATE_START}))

echo -e "\\n\\tTotal elapsed time: ${TOTAL_TIME} \\n"
    ''')
  }

  publishers {
    archiveJunit('**/eclipse-testing/results/xml/*.xml') {
      healthScaleFactor((1.0).doubleValue())
    }
    archiveArtifacts {
      pattern('**/eclipse-testing/results/**, **/eclipse-testing/directorLogs/**, *.properties, *.txt')
    }
  }
}
