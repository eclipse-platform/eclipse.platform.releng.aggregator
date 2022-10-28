#!/bin/bash

useLocalFiles=0
cleanWorkspace=0
workspace=/tmp/collectResults

usage () {
    echo "
Arguments:
  '-id'|'--buildID'                 ID of the I-build being tested, i.e. I20221020-1800 
  '-url'                            URL of the test job to collect results from
  '-j'|'--triggeringJob'            Name of the test job results are being collected from, i.e. ep426I-unit-cen64-gtk3-java11
  '-w'|'--workspace'                [Optional] Set a custom workspace location, defaults to /tmp/collectResults
  '-p'|'--postingDirectory'         [Optional] Location for output files, defaults to \${workspace}/output
  '-L'|'--localFiles'               [Optional] Use local versions of collectTestResults.xml, genTestIndexes.xml and publish.xml. For testing changes.
  '-c'|'--cleanWorkspace'           [Optional] 
"
    exit
}

while [[ "$#" -gt 0 ]]; do
  case $1 in
    '-id'|'--buildID') buildID=$(echo $2|tr -d ' '); shift 2;;
    '-url') buildURL=$(echo $2|tr -d ' '|sed 's/\/$//'); shift 2;;
    '-j'|'--triggeringJob') triggeringJob=$(echo $2|tr -d ' '); shift 2;;
    '-w'|'--workspace') workspace=$(echo $2|tr -d ' '|sed 's/\/$//'); shift 2;;
    '-p'|'--postingDirectory') postingDirectory=$(echo $2|tr -d ' '|sed 's/\/$//'); shift 2;;
    '-L'|'--localFiles') useLocalFiles=1; shift 1;;
    '-c'|'--cleanWorkspace') cleanWorkspace=1; shift 1;;
    '-h'|'--help') usage;;
  esac
done

postingDirectory=${postingDirectory:-"${workspace}/output"}

if [[ ! -n $buildID ]]; then {
  echo "$buildID"
  usage
 }
fi
if [[ ! -n "${buildURL}" ]]; then {
  echo "url"
  usage
 }
 fi

#Clean workspace
if [[ $cleanWorkspace -eq 1 ]]; then
  rm -rf ${workspace}
  mkdir -p ${workspace}
  cd ${workspace}
fi


if [[ ! -d $postingDirectory ]]; then mkdir -p $postingDirectory; fi

#Source build variables
wget -O ${workspace}/buildproperties.shsource --no-check-certificate http://download.eclipse.org/eclipse/downloads/drops4/${buildID}/buildproperties.shsource
cat ${workspace}/buildproperties.shsource
source ${workspace}/buildproperties.shsource

#get latest Eclipse platform product
wget -O ${workspace}/eclipse-platform-${buildID}-linux-gtk-x86_64.tar.gz https://www.eclipse.org/downloads/download.php?file=/eclipse/downloads/drops4/${buildID}/eclipse-platform-${buildID}-linux-gtk-x86_64.tar.gz
tar -C ${workspace} -xzf ${workspace}/eclipse-platform-*-linux-gtk-x86_64.tar.gz

${workspace}/eclipse/eclipse -nosplash \
  -debug -consolelog -data ${workspace}/workspace-toolsinstall \
  -application org.eclipse.equinox.p2.director \
  -repository ${ECLIPSE_RUN_REPO},${BUILDTOOLS_REPO},${WEBTOOLS_REPO} \
  -installIU org.eclipse.platform.ide,org.eclipse.pde.api.tools,org.eclipse.releng.build.tools.feature.feature.group,org.eclipse.wtp.releng.tools.feature.feature.group \
  -destination ${workspace}/basebuilder \
  -profile SDKProfile

rm -rf ${workspace}/eclipse

#get requisite tools
if [[ $useLocalFiles -eq 0 ]]; then
  wget -O ${workspace}/collectTestResults.xml https://raw.githubusercontent.com/eclipse-platform/eclipse.platform.releng.aggregator/master/cje-production/scripts/collectTestResults.xml
  #wget -O ${workspace}/genTestIndexes.xml https://raw.githubusercontent.com/eclipse-platform/eclipse.platform.releng.aggregator/master/cje-production/scripts/genTestIndexes.xml
  wget -O ${workspace}/publish.xml https://raw.githubusercontent.com/eclipse-platform/eclipse.platform.releng.aggregator/master/eclipse.platform.releng.tychoeclipsebuilder/eclipse/buildScripts/publish.xml
fi

cd ${workspace}
git clone https://github.com/eclipse-platform/eclipse.platform.releng.aggregator.git
cd ${workspace}/eclipse.platform.releng.aggregator/eclipse.platform.releng.tychoeclipsebuilder/eclipse
scp -r publishingFiles ${workspace}/publishingFiles
cd ${workspace}

#triggering ant runner
baseBuilderDir=${workspace}/basebuilder
#javaCMD=/opt/public/common/java/openjdk/jdk-11_x64-latest/bin/java

launcherJar=$(find ${baseBuilderDir}/. -name "org.eclipse.equinox.launcher_*.jar" | sort | head -1 )

devworkspace=${workspace}/workspace-antRunner

java -jar ${launcherJar} -nosplash -consolelog -debug -data $devworkspace -application org.eclipse.ant.core.antRunner -file ${workspace}/collectTestResults.xml \
  -DpostingDirectory=${postingDirectory} \
  -DbuildURL=${buildURL} \
  -DbuildID=${buildID} \
  -DEBUILDER_HASH=${EBUILDER_HASH}
  
devworkspace=${workspace}/workspace-updateTestResults

java -jar ${launcherJar} -nosplash -consolelog -debug -data $devworkspace -application org.eclipse.ant.core.antRunner -file ${workspace}/publish.xml \
  -DpostingDirectory=${postingDirectory} \
  -Djob=${triggeringJob} \
  -DbuildID=${buildID} \
  -DeclipseStream=${STREAM} 
