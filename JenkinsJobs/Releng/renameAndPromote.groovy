job('Releng/renameAndPromote'){
  displayName('Rename and Promote')
  description('''
This job does the "stage 1" or first part of a promotion. It renames the files for Equinox and Eclipse, creates an appropriate repo on "downloads", rsync's everything to 'downloads', but leave everything "invisible" -- unless someone knows the exact URL. This allows two things. First, allows artifacts some time to "mirror" when that is needed. But, also, allows the sites and repositories to be examined for correctness before making them visible to the world. 

The second (deferred) step that makes things visible works, in part, based on some output of this first step. Hence, they must "share a workspace". 
  ''')

  parameters {
    stringParam('DROP_ID', null, '''
The name (or, build id) of the build to rename and promote. Typically would be a value such as I20160530-2000 or M20160912-1000. 
It must match the name of the build on the build machine.
    ''')
    stringParam('CHECKPOINT', null, 'M1, M3, RC1, RC2, RC3 etc (blank for final releases).')
    stringParam('SIGNOFF_BUG', null, 'The issue that was used to "signoff" the checkpoint. If there are no unit test failures, this can be left blank. Otherwise a link is added to test page explaining that "failing unit tests have been investigated".')
    stringParam('TRAIN_NAME', null, 'The name of the release stream, typically yyyy-mm. For example: 2022-09')
    stringParam('STREAM', null, 'Needs to be all three files of primary version for the release, such as 4.7.1 or 4.8.0.')
    stringParam('DL_TYPE', null, '''
This is the build type we are promoting TO. 
I-builds promote to 'S' until 'R'. 
    ''')
  }

  logRotator {
    daysToKeep(10)
    numToKeep(5)
  }

  jdk('adoptopenjdk-hotspot-jdk11-latest')

  label('migration')

  wrappers { //adds pre/post actions
    timestamps()
    sshAgent('projects-storage.eclipse.org-bot-ssh')
    timeout {
      absolute(60)
    }
  }
  
  steps {
    shell('''
#!/bin/bash -x

cd ${WORKSPACE}
wget https://download.eclipse.org/eclipse/relengScripts/cje-production/promotion/promoteSites.sh

chmod +x promoteSites.sh

${WORKSPACE}/promoteSites.sh
    ''')
  }

  publishers {
    archiveArtifacts {
      pattern('**/stage2output*/**')
    }
    extendedEmail {
      recipientList("sravankumarl@in.ibm.com")
    }
  }
}