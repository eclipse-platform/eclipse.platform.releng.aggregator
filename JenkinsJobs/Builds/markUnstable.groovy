job('Builds/markUnstable'){
  displayName('Mark Unstable')
  description('Hudson job to mark a build unstable.')

  parameters {
    stringParam('buildId', null, 'ID of the build to be marked unstable.')
    stringParam('issueNum', null, 'Issue report id to track instability in the build.')
    stringParam('issueUrl', null, 'URL for the issue.')
  }

  label('centos-8')

  logRotator {
    daysToKeep(3)
    numToKeep(5)
  }

  jdk('openjdk-jdk11-latest')

  wrappers { //adds pre/post actions
    timestamps()
    sshAgent('ssh://genie.releng@projects-storage.eclipse.org')
  }

  steps {
    shell('''
      #!/bin/bash -x

      # Strip spaces from the buildId and bugId
      buildId=$(echo $buildId|tr -d ' ')
      issueNum=$(echo $issueNum|tr -d ' ')

      #If bug id is empty we need to exit.
      if [ -z "$issueNum" ]
      then
        exit 1
      fi

      #If build Id is empty exit
      if [ -z "$buildId" ]
      then
        echo "BuildId is empty exiting"
        exit 1
      fi

      epDownloadDir=/home/data/httpd/download.eclipse.org/eclipse
      dropsPath=${epDownloadDir}/downloads/drops4
      p2RepoPath=${epDownloadDir}/updates
      buildDir=${dropsPath}/${buildId}

      workingDir=${epDownloadDir}/workingDir

      workspace=${workingDir}/${JOB_NAME}-${BUILD_NUMBER}

      ssh genie.releng@projects-storage.eclipse.org rm -rf ${workingDir}/${JOB_NAME}*

      ssh genie.releng@projects-storage.eclipse.org mkdir -p ${workspace}
      ssh genie.releng@projects-storage.eclipse.org cd ${workspace}

      #get latest Eclipse platform product
      epRelDir=$(ssh genie.releng@projects-storage.eclipse.org ls -d --format=single-column ${dropsPath}/R-*|sort|tail -1)
      ssh genie.releng@projects-storage.eclipse.org tar -C ${workspace} -xzf ${epRelDir}/eclipse-platform-*-linux-gtk-x86_64.tar.gz

      #get requisite tools
      ssh genie.releng@projects-storage.eclipse.org wget -O ${workspace}/removeFromComposite.xml https://download.eclipse.org/eclipse/relengScripts/cje-production/scripts/removeFromComposite.xml

      #triggering ant runner
      baseBuilderDir=${workspace}/eclipse
      javaCMD=/opt/public/common/java/openjdk/jdk-11_x64-latest/bin/java

      launcherJar=$(ssh genie.releng@projects-storage.eclipse.org find ${baseBuilderDir}/. -name "org.eclipse.equinox.launcher_*.jar" | sort | head -1 )

      scp genie.releng@projects-storage.eclipse.org:${buildDir}/buildproperties.shsource .
      source ./buildproperties.shsource
      repoDir=/home/data/httpd/download.eclipse.org/eclipse/updates/${STREAMMajor}.${STREAMMinor}-${BUILD_TYPE}-builds

      devworkspace=${workspace}/workspace-antRunner
      devArgs=-Xmx512m
      extraArgs="removeFromComposite -Drepodir=${repoDir} -Dcomplocation=${buildId}"
      ssh genie.releng@projects-storage.eclipse.org  ${javaCMD} -jar ${launcherJar} -nosplash -consolelog -debug -data $devworkspace -application org.eclipse.ant.core.antRunner -file ${workspace}/removeFromComposite.xml ${extraArgs} -vmargs $devArgs

      #add unstable tags
      ssh genie.releng@projects-storage.eclipse.org rm -f ${buildDir}/buildUnstable
      touch buildUnstable
      echo "<p>This build is marked unstable due to <a href='${issueUrl}'>issue ${issueNum}</a>.</p>" > buildUnstable
      scp buildUnstable genie.releng@projects-storage.eclipse.org:${buildDir}/buildUnstable

      ssh genie.releng@projects-storage.eclipse.org rm -rf ${workingDir}/${JOB_NAME}*

      exit 0
    ''')
  }

  publishers {
    extendedEmail {
      recipientList('sravankumarl@in.ibm.com')
      contentType('default')
    }

    downstreamParameterized {
      trigger('updateIndex') {
        condition('STABLE')
        triggerWithNoParameters(true)
      }
    }
  }
}
