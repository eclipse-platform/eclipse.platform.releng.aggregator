job('Cleanup/cleanup-win-builds-machine'){
  triggers {
    cron('@weekly')
  }

  label('rs68g-win10')

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
cd

dir

cd ..
dir
    ''')
  }

  publishers {
    extendedEmail {
      recipientList("sravankumarl@in.ibm.com")
    }
  }
}
