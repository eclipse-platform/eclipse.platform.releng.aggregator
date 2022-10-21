job('SmokeTests/ep-smoke-test-arm64'){

  logRotator {
    numToKeep(10)
  }

  parameters {
    stringParam('buildId', null, 'Build Id to test (such as I20120717-0800, N20120716-0800). ')
    stringParam('javaDownload', 'https://download.java.net/java/GA/jdk19/877d6127e982470ba2a7faa31cc93d04/36/GPL/openjdk-19_linux-aarch64_bin.tar.gz', 'fully qualified link to java download')
    stringParam('testsToRun', 'ui', 'This can be any ant target from https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/blob/master/production/testScripts/configuration/sdk.tests/testScripts/test.xml')
    stringParam('secManager', '-Djava.security.manager=allow', null)
  }

  concurrentBuild()

  label('arm64')

  jdk('openjdk-jdk19-latest')
 
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

export PATH=${JAVA_HOME}/bin:${PATH}

echo JAVA_HOME: $JAVA_HOME
echo PATH: $PATH

export ANT_OPTS="${ANT_OPTS} -Djava.io.tmpdir=${WORKSPACE}/tmp ${secManager}"
export eclipseArch=aarch64

env 1>envVars.txt 2>&1
ant -diagnostics 1>antDiagnostics.txt 2>&1
java -XshowSettings -version 1>javaSettings.txt 2>&1

ant -f getEBuilder.xml -Djava.io.tmpdir=${WORKSPACE}/tmp -DbuildId=$buildId  -DeclipseStream=${STREAM} -DEBUILDER_HASH=${EBUILDER_HASH}  -DdownloadURL=https://download.eclipse.org/eclipse/downloads/drops4/${buildId}  -Dosgi.os=linux -Dosgi.ws=gtk -Dosgi.arch=aarch64 -DtestSuite=${testsToRun}

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
    extendedEmail {
      triggers {
        unstable {
          recipientList("sravankumarl@in.ibm.com akurtakov@gmail.com")
        }
      }
    }
  }
}
