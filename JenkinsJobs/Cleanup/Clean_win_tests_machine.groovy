job('Cleanup/Clean-win-tests-machine'){
  triggers {
    cron('@weekly')
  }

  label('qa6xd-win11')

  logRotator {
    daysToKeep(5)
    numToKeep(5)
  }

  disabled()
  
  wrappers { //adds pre/post actions
    preBuildCleanup()
    timestamps()
    timeout {
      elastic(150, 3, 60)
    }
  }
  
  steps {
    batchFile('''
cd ..

dir

rd /s /q Clean-win-tests-machine
rd /s /q ep424I-unit-win32-java11

    ''')
  }

  publishers {
    extendedEmail {
      recipientList("sravankumarl@in.ibm.com")
    }
  }
}
