job('rt.equinox.releng.cleanupDLsite'){
  triggers {
    cron('45 7 * * *')
  }

  label('centos-latest')

  logRotator {
    daysToKeep(10)
    numToKeep(10)
  }
 
  wrappers { //adds pre/post actions
    timestamps()
    sshAgent('projects-storage.eclipse.org-bot-ssh')
  }
  
  steps {
    shell('''
#!/bin/bash -x

wget --no-proxy https://raw.githubusercontent.com/eclipse-platform/eclipse.platform.releng.aggregator/master/cje-production/cleaners/cleanupEquinox.sh

ssh genie.releng@projects-storage.eclipse.org < ${WORKSPACE}/cleanupEquinox.sh
    ''')
  }

  publishers {
    extendedEmail {
      recipientList("sravankumarl@in.ibm.com")
    }
  }
}
