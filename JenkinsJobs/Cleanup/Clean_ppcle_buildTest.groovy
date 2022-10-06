job('Cleanup/Clean-ppcle-buildTest'){
  triggers {
    cron('@weekly')
  }

  label('ppcle-buildTest')

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
du -hs *
df -h
    ''')
  }

  publishers {
    extendedEmail {
      recipientList("sravankumarl@in.ibm.com")
    }
  }
}
