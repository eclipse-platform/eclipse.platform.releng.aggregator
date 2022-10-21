job('Releng/createGenericComposites'){
  displayName('Create Generic Composites')
  description('Remove generic repositories (I-builds etc) for the previous stream and create new versions for the new stream.')

  logRotator {
    daysToKeep(5)
    numToKeep(5)
  }

  parameters {
    stringParam('currentStream', null, 'Current release stream, for example: 4.25')
    stringParam('previousStream', null, 'Previous release stream, for example: 4.24')
  }

  jdk('openjdk-jdk11-latest')

  label('centos-8')

  wrappers { //adds pre/post actions
    timestamps()
    sshAgent('projects-storage.eclipse.org-bot-ssh')
  }
  
  steps {
    shell('''
#!/bin/bash -x

epDownloadDir=/home/data/httpd/download.eclipse.org/eclipse
dropsPath=${epDownloadDir}/downloads/drops4

cd ${WORKSPACE}
epRelDir=$(ssh genie.releng@projects-storage.eclipse.org ls -d --format=single-column ${dropsPath}/R-*|sort|tail -1)
scp genie.releng@projects-storage.eclipse.org:${epRelDir}/eclipse-SDK-*-linux-gtk-x86_64.tar.gz eclipse-SDK.tar.gz

tar xvzf eclipse-SDK.tar.gz

ECLIPSE_EXE=${WORKSPACE}/eclipse/eclipse


# get the update script
wget https://download.eclipse.org/eclipse/relengScripts/cje-production/scripts/updateGenericComposites.xml

${ECLIPSE_EXE} --launcher.suppressErrors  -nosplash -console -data workspace-updateGenericComposite -application org.eclipse.ant.core.antRunner -f updateGenericComposites.xml ${extraArgs} -vmargs -DcurrentStream=${currentStream} -DpreviousStream=${previousStream} -Dworkspace=${WORKSPACE}

# remove existing generic repos 
ssh genie.releng@projects-storage.eclipse.org rm -rf ${epDownloadDir}/updates/I-builds
ssh genie.releng@projects-storage.eclipse.org rm -rf ${epDownloadDir}/updates/P-builds
ssh genie.releng@projects-storage.eclipse.org rm -rf ${epDownloadDir}/updates/Y-builds
ssh genie.releng@projects-storage.eclipse.org rm -rf ${epDownloadDir}/updates/latest

#copy newly created generic repos
scp -r I-builds genie.releng@projects-storage.eclipse.org:${epDownloadDir}/updates/.
scp -r P-builds genie.releng@projects-storage.eclipse.org:${epDownloadDir}/updates/.
scp -r Y-builds genie.releng@projects-storage.eclipse.org:${epDownloadDir}/updates/.
scp -r latest genie.releng@projects-storage.eclipse.org:${epDownloadDir}/updates/.
    ''')
  }
}
