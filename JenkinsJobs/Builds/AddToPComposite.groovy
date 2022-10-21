def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('JenkinsJobs/JobDSL.json'))
def STREAMS = config.Streams

for (STREAM in STREAMS){
  job('Builds/AddTo' + STREAM + 'PComposite'){
    displayName('Add to ' + STREAM + 'PComposite')
    description('Hudson job to add a build to the composite.')

    parameters {
      stringParam('buildId', null, 'Build ID of the build to be added.')
    }

    label('migration')

    logRotator {
      daysToKeep(3)
      numToKeep(5)
    }

    jdk('openjdk-jdk11-latest')

    wrappers { //adds pre/post actions
      timestamps()
      sshAgent('projects-storage.eclipse.org-bot-ssh')
    }

    steps {
      shell('''
        #!/bin/bash -x

        # this function executes command passed as command line parameter and 
        # if that command fails it exit with the same error code as the failed command

        # Strip spaces from the buildId and eclipseStream
        buildId=$(echo $buildId|tr -d ' ')

        STREAMMajor=4
        STREAMMinor=25
        BUILD_TYPE='P'


        #If build Id is empty exit
        if [ -z "$buildId" ]
        then
          echo "BuildId is empty exiting"
          exit 1
        fi

        epDownloadDir=/home/data/httpd/download.eclipse.org/eclipse
        dropsPath=${epDownloadDir}/downloads/drops4
        p2RepoPath=${epDownloadDir}/updates

        workingDir=${epDownloadDir}/workingDir

        workspace=${workingDir}/${JOB_NAME}-${BUILD_NUMBER}

        ssh genie.releng@projects-storage.eclipse.org rm -rf ${workingDir}/${JOB_NAME}*

        ssh genie.releng@projects-storage.eclipse.org mkdir -p ${workspace}
        ssh genie.releng@projects-storage.eclipse.org cd ${workspace}

        #get latest Eclipse platform product
        epRelDir=$(ssh genie.releng@projects-storage.eclipse.org ls -d --format=single-column ${dropsPath}/R-*|sort|tail -1)
        ssh genie.releng@projects-storage.eclipse.org tar -C ${workspace} -xzf ${epRelDir}/eclipse-platform-*-linux-gtk-x86_64.tar.gz

        #get requisite tools
        ssh genie.releng@projects-storage.eclipse.org wget -O ${workspace}/addToComposite.xml https://download.eclipse.org/eclipse/relengScripts/cje-production/scripts/addToComposite.xml

        #triggering ant runner
        baseBuilderDir=${workspace}/eclipse
        javaCMD=/opt/public/common/java/openjdk/jdk-11_x64-latest/bin/java

        launcherJar=$(ssh genie.releng@projects-storage.eclipse.org find ${baseBuilderDir}/. -name "org.eclipse.equinox.launcher_*.jar" | sort | head -1 )

        repoDir=/home/data/httpd/download.eclipse.org/eclipse/updates/${STREAMMajor}.${STREAMMinor}-${BUILD_TYPE}-builds

        devworkspace=${workspace}/workspace-antRunner
        devArgs=-Xmx512m
        extraArgs="addToComposite -Drepodir=${repoDir} -Dcomplocation=${buildId}"
        ssh genie.releng@projects-storage.eclipse.org  ${javaCMD} -jar ${launcherJar} -nosplash -consolelog -debug -data $devworkspace -application org.eclipse.ant.core.antRunner -file ${workspace}/addToComposite.xml ${extraArgs} -vmargs $devArgs


        ssh genie.releng@projects-storage.eclipse.org rm -rf ${workingDir}/${JOB_NAME}*

        exit 0
      ''')
    }
  }
}
