job('Releng/newStreamRepos'){
  displayName('Create New Stream Repos')
  description('This job initializes the p2 repositories when new stream is started.')

  logRotator {
    daysToKeep(5)
    numToKeep(5)
  }

  parameters {
    stringParam('streamVersion', null, 'The stream version for which repos should be initialized.')
  }

  label('migration')

  wrappers { //adds pre/post actions
    timestamps()
    sshAgent('ssh://genie.releng@projects-storage.eclipse.org')
  }
  
  steps {
    shell('''
#!/bin/bash -x

epDownloadDir=/home/data/httpd/download.eclipse.org/eclipse/updates

ssh genie.releng@projects-storage.eclipse.org cp -r ${epDownloadDir}/template_repo/ ${epDownloadDir}/${streamVersion}/
ssh genie.releng@projects-storage.eclipse.org cp -r ${epDownloadDir}/template_repo/ ${epDownloadDir}/${streamVersion}-I-builds/
ssh genie.releng@projects-storage.eclipse.org cp -r ${epDownloadDir}/template_repo/ ${epDownloadDir}/${streamVersion}-Y-builds/
#ssh genie.releng@projects-storage.eclipse.org cp -r ${epDownloadDir}/template_repo/ ${epDownloadDir}/${streamVersion}-P-builds/
    ''')
  }
}
