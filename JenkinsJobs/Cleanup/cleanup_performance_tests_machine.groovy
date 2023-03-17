job('Cleanup/cleanup-performance-tests-machine'){
  triggers {
    cron('@weekly')
  }

  label('zyt5z-centos83')

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

cd ${WORKSPACE}/../..
pwd
ls -al
rm -rf Build-Docker-images PerformanceTests
df -h
    ''')
  }

  publishers {
    extendedEmail {
      recipientList("sravankumarl@in.ibm.com")
    }
  }
}
