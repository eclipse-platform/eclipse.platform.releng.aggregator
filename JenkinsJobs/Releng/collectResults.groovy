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
  
  jdk('temurin-jdk21-latest')

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

wget -O ${WORKSPACE}/buildproperties.shsource --no-check-certificate http://download.eclipse.org/eclipse/downloads/drops4/${buildID}/buildproperties.shsource
cat ${WORKSPACE}/buildproperties.shsource
source ${WORKSPACE}/buildproperties.shsource

epDownloadDir=/home/data/httpd/download.eclipse.org/eclipse
dropsPath=${epDownloadDir}/downloads/drops4
buildDir=${dropsPath}/${buildID}

rsync -avzh genie.releng@projects-storage.eclipse.org:${buildDir} postingDir

#get latest Eclipse platform product
epRelDir=$(ssh genie.releng@projects-storage.eclipse.org ls -d --format=single-column ${dropsPath}/R-*|sort|tail -1)
scp genie.releng@projects-storage.eclipse.org:${epRelDir}/eclipse-platform-*-linux-gtk-x86_64.tar.gz .
 
tar -C . -xzf eclipse-platform-*-linux-gtk-x86_64.tar.gz

eclipse/eclipse -nosplash \\
  -consolelog -data workspace-toolsinstall \\
  -application org.eclipse.equinox.p2.director \\
  -repository ${ECLIPSE_RUN_REPO},${BUILDTOOLS_REPO},${WEBTOOLS_REPO} \\
  -installIU org.eclipse.releng.build.tools.feature.feature.group,org.eclipse.wtp.releng.tools.feature.feature.group 

#get requisite tools
wget -O collectTestResults.xml https://raw.githubusercontent.com/eclipse-platform/eclipse.platform.releng.aggregator/master/cje-production/scripts/collectTestResults.xml
wget -O publish.xml https://raw.githubusercontent.com/eclipse-platform/eclipse.platform.releng.aggregator/master/cje-production/scripts/publish.xml
wget -O testManifest.xml https://raw.githubusercontent.com/eclipse-platform/eclipse.platform.releng.aggregator/master/eclipse.platform.releng.tychoeclipsebuilder/eclipse/publishingFiles/testManifest.xml

#triggering ant runner
devworkspace=${WORKSPACE}/workspace-antRunner

eclipse/eclipse -nosplash -consolelog -debug -data $devworkspace -application org.eclipse.ant.core.antRunner -file collectTestResults.xml \\
  -DpostingDirectory=postingDir \\
  -Djob=${triggeringJob} \\
  -DbuildURL=${buildURL} \\
  -DbuildID=${buildID}

devworkspace=${WORKSPACE}/workspace-updateTestResults

eclipse/eclipse -nosplash -consolelog -debug -data $devworkspace -application org.eclipse.ant.core.antRunner -file publish.xml \\
  -DpostingDirectory=postingDir \\
  -Djob=${triggeringJob} \\
  -DbuildID=${buildID} \\
  -DeclipseStream=${STREAM} \\
  "-DtestsConfigExpected=${TEST_CONFIGURATIONS_EXPECTED}" \\
  -DmanifestFile=testManifest.xml

rsync -avzh postingDir/${buildID} genie.releng@projects-storage.eclipse.org:${dropsPath}

    ''')
  }

  publishers {
    downstream('Releng/updateIndex', 'SUCCESS')
  }
}
