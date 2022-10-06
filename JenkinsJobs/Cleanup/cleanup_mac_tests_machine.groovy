job('Cleanup/cleanup-mac-build-machine'){
  triggers {
    cron('@weekly')
  }

  label('nc1ht-macos11-arm64')

  logRotator {
    numToKeep(5)
  }
  
  wrappers { //adds pre/post actions
    preBuildCleanup()
    timestamps()
  }
  
  steps {
    shell('''
#!/bin/bash -x

cd ${WORKSPACE}

pwd

ls -al

df -h
    ''')
  }

  publishers {
    extendedEmail {
      recipientList("sravankumarl@in.ibm.com")
    }
  }
}
