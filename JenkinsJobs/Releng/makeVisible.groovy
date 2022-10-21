job('Releng/makeVisible'){
  displayName('Make Visible')
  description('''
The first part of doing a promotion -- the Rename And Promote job -- creates some scripts that make this deferred "make visible" part possible.  

Therefore, they have to share a workspace, and the output of the first job must remain in place until its time to "make visible". 

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
  }

  jdk('adoptopenjdk-hotspot-jdk11-latest')

  label('centos-8')

  wrappers { //adds pre/post actions
    timestamps()
    sshAgent('projects-storage.eclipse.org-bot-ssh', 'git.eclipse.org-bot-ssh', 'github-bot-ssh')
    timeout {
      absolute(60)
    }
  }
  
  steps {
    shell('''
#!/bin/bash -x

wget https://download.eclipse.org/eclipse/relengScripts/cje-production/promotion/makeVisible.sh

chmod +x makeVisible.sh

${WORKSPACE}/makeVisible.sh
    ''')
  }

  publishers {
    downstreamParameterized {
      trigger('updateIndex') {
        triggerWithNoParameters(true)
      }
    }
    extendedEmail {
      recipientList("sravankumarl@in.ibm.com")
    }
  }
}
