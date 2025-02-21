job('Releng/ep-collectResults'){
  displayName('Collect Results')
  description('This job is to perform some summary analysis and then write unit test results to the download page.')

  parameters {
    stringParam('triggeringJob', null, 'Name of the job to collect results from: i.e. \'ep427I-unit-cen64-gtk3-java17\'.')
    stringParam('buildURL', null, 'Build URL of the triggering job.')
    stringParam('buildID', null, 'ID of the I-build being tested.')
  }

  label('basic')

  logRotator {
    daysToKeep(5)
    numToKeep(10)
  }

  wrappers { //adds pre/post actions
    timestamps()
    preBuildCleanup()
    sshAgent('projects-storage.eclipse.org-bot-ssh')
    timeout {
      absolute(30)
    }
  }

  steps {
    shell('''#!/bin/bash -xe

buildID=$(echo $buildID|tr -d ' ')
buildURL=$(echo $buildURL|tr -d ' ')
triggeringJob=$(echo $triggeringJob|tr -d ' ')

java_home=/opt/public/common/java/openjdk/jdk-17_x64-latest/bin

wget -O ${WORKSPACE}/buildproperties.shsource --no-check-certificate http://download.eclipse.org/eclipse/downloads/drops4/${buildID}/buildproperties.shsource
cat ${WORKSPACE}/buildproperties.shsource
source ${WORKSPACE}/buildproperties.shsource

epDownloadDir=/home/data/httpd/download.eclipse.org/eclipse
dropsPath=${epDownloadDir}/downloads/drops4
buildDir=${dropsPath}/${buildID}

workingDir=${epDownloadDir}/workingDir

workspace=${workingDir}/${JOB_BASE_NAME}-${BUILD_NUMBER}

ssh genie.releng@projects-storage.eclipse.org rm -rf ${workingDir}/${JOB_BASE_NAME}*

ssh genie.releng@projects-storage.eclipse.org mkdir -p ${workspace}

#get latest Eclipse platform product
epRelDir=$(ssh genie.releng@projects-storage.eclipse.org ls -d --format=single-column ${dropsPath}/R-*|sort|tail -1)
ssh genie.releng@projects-storage.eclipse.org tar -C ${workspace} -xzf ${epRelDir}/eclipse-platform-*-linux-gtk-x86_64.tar.gz

ssh genie.releng@projects-storage.eclipse.org PATH=${java_home}:$PATH ${workspace}/eclipse/eclipse -nosplash \\
  -debug -consolelog -data ${workspace}/workspace-toolsinstall \\
  -application org.eclipse.equinox.p2.director \\
  -repository ${ECLIPSE_RUN_REPO},${BUILDTOOLS_REPO},${WEBTOOLS_REPO} \\
  -installIU org.eclipse.platform.ide,org.eclipse.pde.api.tools,org.eclipse.releng.build.tools.feature.feature.group,org.eclipse.wtp.releng.tools.feature.feature.group \\
  -destination ${workspace}/basebuilder \\
  -profile SDKProfile

ssh genie.releng@projects-storage.eclipse.org rm -rf ${workspace}/eclipse

#get requisite tools
ssh genie.releng@projects-storage.eclipse.org wget -O ${workspace}/collectTestResults.xml https://raw.githubusercontent.com/eclipse-platform/eclipse.platform.releng.aggregator/master/cje-production/scripts/collectTestResults.xml
ssh genie.releng@projects-storage.eclipse.org wget -O ${workspace}/publish.xml https://raw.githubusercontent.com/eclipse-platform/eclipse.platform.releng.aggregator/master/cje-production/scripts/publish.xml

cd ${WORKSPACE}
git clone https://github.com/eclipse-platform/eclipse.platform.releng.aggregator.git
cd ${WORKSPACE}/eclipse.platform.releng.aggregator/eclipse.platform.releng.tychoeclipsebuilder
scp -r eclipse genie.releng@projects-storage.eclipse.org:${workspace}/eclipse
cd ${WORKSPACE}

#triggering ant runner
baseBuilderDir=${workspace}/basebuilder
javaCMD=${java_home}/java

launcherJar=$(ssh genie.releng@projects-storage.eclipse.org find ${baseBuilderDir}/. -name "org.eclipse.equinox.launcher_*.jar" | sort | head -1 )

scp genie.releng@projects-storage.eclipse.org:${buildDir}/buildproperties.shsource .
source ./buildproperties.shsource

devworkspace=${workspace}/workspace-antRunner

ssh genie.releng@projects-storage.eclipse.org  ${javaCMD} -jar ${launcherJar} -nosplash -consolelog -debug -data $devworkspace -application org.eclipse.ant.core.antRunner -file ${workspace}/collectTestResults.xml \\
  -DpostingDirectory=${dropsPath} \\
  -Djob=${triggeringJob} \\
  -DbuildURL=${buildURL} \\
  -DbuildID=${buildID}

devworkspace=${workspace}/workspace-updateTestResults

ssh genie.releng@projects-storage.eclipse.org  ${javaCMD} -jar ${launcherJar} -nosplash -consolelog -debug -data $devworkspace -application org.eclipse.ant.core.antRunner -file ${workspace}/publish.xml \\
  -DpostingDirectory=${dropsPath} \\
  -Djob=${triggeringJob} \\
  -DbuildID=${buildID} \\
  -DeclipseStream=${STREAM} \\
  "-DtestsConfigExpected=${TEST_CONFIGURATIONS_EXPECTED}" \\
  -DEBuilderDir=${workspace}

#Delete Workspace
ssh genie.releng@projects-storage.eclipse.org rm -rf ${workingDir}/${JOB_BASE_NAME}*
    ''')
  }

  publishers {
    downstream('Releng/updateIndex', 'SUCCESS')
  }
}
