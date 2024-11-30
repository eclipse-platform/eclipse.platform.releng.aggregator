job('Releng/updateIndex'){
  displayName('Update Download Index')
  description('This is a Job for recreating the download indexes.')

  logRotator {
    numToKeep(10)
  }

  jdk('openjdk-jdk11-latest')

  label('basic')

  wrappers { //adds pre/post actions
    timestamps()
    sshAgent('projects-storage.eclipse.org-bot-ssh')
  }
  
  steps {
    shell('''
#!/bin/bash -x

PHP_PAGE="createIndex4x.php"
HTML_PAGE="index.html"
TEMP_INDEX="tempIndex.html"
SITE_HOST=${SITE_HOST:-download.eclipse.org}

wget --no-verbose -O ${TEMP_INDEX} https://${SITE_HOST}/eclipse/downloads/${PHP_PAGE} 2>&1

scp ${TEMP_INDEX} genie.releng@projects-storage.eclipse.org:/home/data/httpd/download.eclipse.org/eclipse/downloads/${HTML_PAGE}
    ''')
  }

  publishers {
    extendedEmail {
      recipientList("sravankumarl@in.ibm.com")
    }
  }
}
