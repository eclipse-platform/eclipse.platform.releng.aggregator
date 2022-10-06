job('Releng/checkCompositesValidity'){
  displayName('Check Composites Validity')
  description('This job periodically checks that our main repositories are still "valid". It does this by accessing the repository to list the IUs it finds there. A repository can be invalid if it is not "atomic" which would usually be the result of a manual editing mistake or a child repository being deleted without the composite files being updated.')

  logRotator {
    daysToKeep(15)
  }

  jdk('adoptopenjdk-hotspot-jdk11-latest')

  label('centos-8')

  triggers {
    cron {
      spec('@daily')
    }
  }

  wrappers { //adds pre/post actions
    timestamps()
    sshAgent('ssh://genie.releng@projects-storage.eclipse.org')
    buildTimeoutWrapper{
      strategy {
        absoluteTimeOutStrategy {
          timeoutMinutes('30')
        }
        timeoutEnvVar('')
      }
    }
  }

  publishers {
    extendedEmail {
      recipientList("sravankumarl@in.ibm.com")
    }
  }
  
  steps {
    shell('''
#!/bin/bash -x

wget https://download.eclipse.org/eclipse/relengScripts/cje-production/scripts/checkComposites/checkComposites.sh

bash -x checkComposites.sh
    ''')
  }
}
