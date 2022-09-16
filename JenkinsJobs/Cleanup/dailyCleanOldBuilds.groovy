job('dailyCleanOldBuilds'){
  displayName('Daily Cleanup for Old Builds')
  description('''
This job runs several types of "cleanup" on the build machine and downloads server to remove old builds and other left overs from old build.
It acts as a simple cron job, currently running at 16:00 every day, to execute 
.../sdk/cleaners/dailyCleanBuildMachine.sh
and other such scripts.
  ''')

  logRotator {
    daysToKeep(10)
    numToKeep(5)
  }

  jdk('oracle-jdk8-latest')

  label('migration')

  triggers {
    cron('''
0 4 * * * 
0 16 * * * 
    ''')
  }

  wrappers { //adds pre/post actions
    timestamps()
    preBuildCleanup()
    sshAgent('ssh://genie.releng@projects-storage.eclipse.org')
    timeout {
      absolute(30)
    }
  }
  
  steps {
    shell('''
#!/bin/bash -x

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

ssh genie.releng@projects-storage.eclipse.org wget -O ${workspace}/dailyCleanDownloads.sh https://raw.githubusercontent.com/eclipse-platform/eclipse.platform.releng.aggregator/master/cje-production/cleaners/dailyCleanDownloads.sh
ssh genie.releng@projects-storage.eclipse.org wget -O ${workspace}/cleanupNightlyRepo.sh https://raw.githubusercontent.com/eclipse-platform/eclipse.platform.releng.aggregator/master/cje-production/cleaners/cleanupNightlyRepo.sh

ssh genie.releng@projects-storage.eclipse.org bash -x ${workspace}/dailyCleanDownloads.sh
ssh genie.releng@projects-storage.eclipse.org bash -x ${workspace}/cleanupNightlyRepo.sh ${workspace}

ssh genie.releng@projects-storage.eclipse.org rm -rf ${workingDir}/${JOB_NAME}*
    ''')
  }

  publishers {
    email-ext {
      project_recipient_list('sravankumarl@in.ibm.com')
    }
  }
}
