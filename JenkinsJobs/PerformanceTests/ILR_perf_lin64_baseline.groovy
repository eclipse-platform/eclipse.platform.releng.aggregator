def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('JenkinsJobs/JobDSL.json'))
def STREAMS = config.Streams

for (STREAM in STREAMS){
  def MAJOR = STREAM.split('\\.')[0]
  def MINOR = STREAM.split('\\.')[1]

  job('PerformanceTests/ep' + MAJOR + MINOR + 'ILR-perf-lin64-baseline'){
    parameters {
      stringParam('buildId', null, 'Build ID to test, such as I20140821-0800 or M20140822-0800')
      stringParam('testToRun', 'selectPerformance', '''
Name of test suite (or test suite collection) to run. 
Collections:
selectPerformance  (a group of tests that complete in about 3 hours)
otherPerformance   (a small group of tests that either are not working, or take greater than one hour each). 

Individual Tests Suites, per collection: 
selectPerformance:

antui
compare
coreresources
coreruntime
jdtdebug
jdtui
osgi
pdeui
swt
teamcvs
ua
uiforms
uiperformance
uircp

otherPerformance:
equinoxp2ui
pdeapitooling
jdtcoreperf
jdttext
jdtuirefactoring
''')
    }

    logRotator {
      numToKeep(5)
    }

    jdk('openjdk-jdk11-latest')

    label('performance')

    authenticationToken('windows2012tests')

    wrappers { //adds pre/post actions
      timestamps()
      preBuildCleanup()
      xvnc {
        useXauthority()
      }
      withAnt {
        installation('apache-ant-latest')
        jdk('openjdk-jdk11-latest')
      }
    }

    steps {
      shell('''
#!/usr/bin/env bash

if [[ -z "${WORKSPACE}" ]]
then
  echo -e "\\n\\tERROR: WORKSPACE variable was not defined"
  exit 1
else
  if [[ ! -d "${WORKSPACE}" ]]
  then
    echo -e "\\n\\tERROR: WORKSPACE was defined, but did not exist?"
    echo -e "\\t\\tIt was defined as ${WORKSPACE}"
    exit 1
  else
    echo -e "\\n\\tINFO: WORKSPACE was defined as ${WORKSPACE}"
    echo -e "\\t\\tWill delete contents, for clean run"
    MaxLoops=15
    SleepTime=60
    currentLoop=0
    nFilesOrDirs=$( find "${WORKSPACE}" -mindepth 1 -maxdepth 1 | wc -l )
    while [[ ${nFilesOrDirs} > 0 ]]
    do
      currentLoop=$(( ${currentLoop} + 1 ))
      if [[ ${currentLoop} -gt ${MaxLoops} ]]
      then
        echo -e "\\n\\tERROR: Number of re-try loops, ${currentLoop}, exceeded maximum, ${MaxLoops}. "
        echo -e " \\t\\tPossibly due to files still being used by another process?"
        exit 1
        break
      fi
      echo -e "\\tcurrentLoop: ${currentLoop}   nFilesOrDirs:  ${nFilesOrDirs}"
      find "${WORKSPACE}" -mindepth 1 -maxdepth 1 -execdir rm -fr '{}' \\;
      nFilesOrDirs=$( find "${WORKSPACE}" -mindepth 1 -maxdepth 1 | wc -l )
      if [[ ${nFilesOrDirs} -gt 0 ]]
      then
        sleep ${SleepTime}
      fi
    done
  fi
fi
echo -e "\\t... ending cleaning"

exit 0
      ''')
      shell('''
#!/usr/bin/env bash

buildId=$(echo $buildId|tr -d ' ')
testToRun=$(echo $testToRun|tr -d ' ')

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

export JAVA_HOME=`readlink -f /usr/bin/java | sed "s:jre/::" | sed "s:bin/java::"`
echo $JAVA_HOME
#export ANT_HOME=/shared/common/apache-ant-1.9.6

export PATH=${JAVA_HOME}/bin:${PATH}
export baselinePerf=true

echo JAVA_HOME: $JAVA_HOME
echo ANT_HOME: $ANT_HOME
echo PATH: $PATH
echo baselinePerf: $baselinePerf

export ANT_OPTS="${ANT_OPTS} -Djava.io.tmpdir=${WORKSPACE}/tmp"

env 1>envVars.txt 2>&1
ant -diagnostics 1>antDiagnostics.txt 2>&1
java -XshowSettings -version 1>javaSettings.txt 2>&1

ant -f getEBuilder.xml -Djava.io.tmpdir=${WORKSPACE}/tmp -DbuildId=$buildId -Djvm=$JAVA_HOME/bin/java -DeclipseStream=${STREAM} -DEBUILDER_HASH=${EBUILDER_HASH}  -DbaselinePerf=${baselinePerf} -DdownloadURL=http://download.eclipse.org/eclipse/downloads/drops4/${buildId}  -Dosgi.os=linux -Dosgi.ws=gtk -Dosgi.arch=x86_64 -DtestSuite=${testToRun} -Dtest.target=performance

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
        pattern('**/eclipse-testing/results/**, **/eclipse-testing/directorLogs/**,  *.properties, *.txt')
      }
      extendedEmail {
        recipientList("sdawley@redhat.com")
      }
      downstreamParameterized {
        trigger('PerformanceTests/ep' + MAJOR + MINOR + 'ILR-perf-lin64') {
          condition('UNSTABLE_OR_BETTER')
          parameters {
            currentBuildParameters()
            predefinedProp('testToRun', '${testToRun}') 
          }
        }
        trigger('Releng/collectPerfResults') {
          condition('UNSTABLE_OR_BETTER')
          parameters {
            currentBuildParameters()
            predefinedProp('triggeringJob', '$JOB_BASE_NAME')
            predefinedProp('buildURL', '$BUILD_URL')
            predefinedProp('buildID', '$buildId')
          }
        }
      }
    }
  }
}
