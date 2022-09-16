//for my sanity
def config = new groovy.json.JsonSlurper().parseText(readFileFromWorkspace('../JobDSL.json'))
def STREAMS = config.Streams

for (STREAM in STREAMS){
  job('Builds/AddTo' + STREAM + 'PComposite'){
    displayName('Add to ' + STREAM + 'PComposite')
    description('Hudson job to reenable a build if it is marked as unstable.')

    parameters {
      stringParam('buildId', 'I20220831-1800', 'Build ID of the build to be marked stable')
    }

    label('migration')

    logRotator {
      daysToKeep(3)
      numToKeep(5)
    }

    jdk('openjdk-jdk11-latest')

    wrappers { //adds pre/post actions
      timestamps()
      sshAgent('ssh://genie.releng@projects-storage.eclipse.org')
    }

    publishers {
      extendedEmail {
        recipientList('sravankumarl@in.ibm.com')
        contentType('default')
      }
    }

    steps {
      shell('''

      ''')
    }
  }
}


job(''){
  displayName('')
  description('')

  parameters {

  }

  triggers {
    cron('@daily')
  }

  label('migration')
  label('centos-8')
  label('qa6xd-win11')

  jdk('adoptopenjdk-hotspot-jdk11-latest')
  jdk('oracle-jdk8-latest')
  jdk('openjdk-jdk11-latest')

  logRotator {
    daysToKeep()
    numToKeep()
  }

  scm {
    git {
      remote{
        name('origin')
        url('')
        credentials('GitHub bot (SSH)')
      }
      branch('master')
      browser {
        githubWeb {
          url('')
        }
      }
      cloneOption {
        shallow(true)
      }
    }
  }
 
  wrappers { //adds pre/post actions
    preBuildCleanup()
    timestamps()
    sshAgent('ssh://genie.releng@projects-storage.eclipse.org', 'ssh://genie.releng@git.eclipse.org', 'GitHub bot (SSH)')
    timeout {
      absolute()
    }
  }
  
  steps {
    shell('''

    ''')
  }

  publishers {
    downstreamParameterized {
      trigger() {
        triggerWithNoParameters(true)
      }
    }
    archiveArtifacts {
      pattern()
    }
    email-ext {
      project_recipient_list('sravankumarl@in.ibm.com')
    }
  }
}
