job('SmokeTests/ep-smoke-test-win32'){

  logRotator {
    numToKeep(10)
  }

  parameters {
    stringParam('buildId', null, 'Build Id to test (such as I20120717-0800, N20120716-0800). ')
    stringParam('testsToRun', 'ui', 'This can be any ant target from https://github.com/eclipse-platform/eclipse.platform.releng.aggregator/blob/master/production/testScripts/configuration/sdk.tests/testScripts/test.xml')
  }

  concurrentBuild()

  label('qa6xd-win11')

  authenticationToken('windows2012tests')
 
  wrappers { //adds pre/post actions
    timestamps()
    preBuildCleanup()
    timeout {
      absolute(901)
    }
    xvnc {
      useXauthority()
    }
  }
  
  steps {
    batchFile('''
@echo off
SETLOCAL ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION
echo start cleaning ...
IF NOT DEFINED WORKSPACE (
    echo ERROR: WORKSPACE variable was not defined.
    exit /B 1
    ) ELSE (
      IF NOT EXIST "%WORKSPACE%" (
        echo ERROR: WORKSPACE was defined, but it did not exist.
        echo     It was defined as %WORKSPACE%
        exit /B 1
        ) ELSE (
          echo WORSPACE defined as %WORKSPACE%
          echo Will delete contents, for clean run.
          rem Note that rmdir and rm do not return ERRORLEVEL.
          rem Which is why we "do while" until count of files is zero.
          rem (or, until max loops is reached).
          set /a maxLoops=15
          echo maxLoops: !maxLoops!
          set /a sleepTime=60000
          echo sleepTime: !sleepTime!
          set /a currentLoop=0

          set /a nFilesOrDirs=0
          for /D %%f in ("%WORKSPACE%\\*") do set /a nFilesOrDirs+=1
          for    %%f in ("%WORKSPACE%\\*") do set /a nFilesOrDirs+=1
          echo currentLoop: !currentLoop!   nFilesOrDirs:  !nFilesOrDirs!

          :LOOP
          IF !nFilesOrDirs! GTR 0 (
            rem this first for loop is for all subdirectories of workspace
            FOR /D %%p IN ("%WORKSPACE%\\*") DO (
              echo removing dir: %%p
              rmdir "%%p" /s /q
              )
            rem this for loop is for for all files remaining, directly under workspace
            FOR %%p IN ("%WORKSPACE%\\*") DO (
              echo deleting file: %%p
              del "%%p"  /q
              )
            set /a currentLoop+=1
            IF !currentLoop! GTR !maxLoops! GOTO MAXLOOPS
            set /a nFilesOrDirs=0
            for /D %%f in ("%WORKSPACE%\\*") do set /a nFilesOrDirs+=1
            for    %%f in ("%WORKSPACE%\\*") do set /a nFilesOrDirs+=1
            echo currentLoop: !currentLoop!   nFilesOrDirs:  !nFilesOrDirs!
            if !nFilesOrDirs! GTR 0 (
              rem Pause a bit before retrying, since if we could not delete, likely due to some process still running.
              rem 'timeout' causes "redirection not allowed" error. See bug 482598. 
              rem C:\\Windows\\System32\\timeout.exe /t !sleepTime!
              ping 127.0.0.1 -n1 -w !sleepTime! >NUL 
              GOTO LOOP
              )
            )
         )
     )
echo ... normal end of cleaning section (i.e. max loops NOT reached)
exit 0


:MAXLOOPS
echo Reached max loops waiting for files to be free to delete
rem note use of "hard exit" (no /B) as an attempt to get Hudson to fail.
exit 0
    ''')
    batchFile('''
rem May want to try and restrict path, as we do on cron jobs, so we
rem have more consistent conditions.
rem export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:~/bin

rem tmp must already exist, for Java to make use of it, in subsequent steps
rem no -p (or /p) needed on Windows. It creates 
mkdir tmp

rem Note: currently this file always comes from master, no matter what branch is being built/tested.
wget -O getEBuilder.xml --no-verbose https://download.eclipse.org/eclipse/relengScripts/production/testScripts/hudsonBootstrap/getEBuilder.xml 2>&1
set buildId
wget -O buildProperties.properties https://download.eclipse.org/eclipse/downloads/drops4/%buildId%/buildproperties.properties
echo off
For /F "tokens=1* delims==" %%A IN (buildProperties.properties) DO (
 IF "%%A"=="STREAM " set STREAM=%%B
 IF "%%A"=="EBUILDER_HASH " set EBUILDER_HASH=%%B
) 
echo on
set STREAM
set EBUILDER_HASH
set JAVA_HOME=C:\\openjdk\\jdk-11\\
set JAVA_HOME
rem set Path="C:\\openjdk\\jdk-11\\bin;C:\\Program Files\\AdoptOpenJDK\\jdk-8.0.202.08\\bin";C:\\ProgramData\\Boxstarter;C:\\Windows\\system32;C:\\Windows;C:\\Windows\\System32\\Wbem;C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\;C:\\Windows\\System32\\OpenSSH\\;C:\\ProgramData\\chocolatey\\bin;C:\\tools\\cygwin\\bin;C:\\Program Files\\IcedTeaWeb\\WebStart\\bin;C:\\WINDOWS\\system32;C:\\WINDOWS;C:\\WINDOWS\\System32\\Wbem;C:\\WINDOWS\\System32\\WindowsPowerShell\\v1.0\\;C:\\WINDOWS\\System32\\OpenSSH\\;C:\\Users\\jenkins_vnc\\AppData\\Local\\Microsoft\\WindowsApps;

ant -f getEBuilder.xml -Djava.io.tmpdir=%WORKSPACE%\\tmp -Djvm="C:\\\\openjdk\\\\jdk-11\\\\bin\\\\java.exe" -DbuildId=%buildId%  -DeclipseStream=%STREAM% -DEBUILDER_HASH=%EBUILDER_HASH%  -DdownloadURL="http://download.eclipse.org/eclipse/downloads/drops4/%buildId%" -Dargs=all -Dosgi.os=win32 -Dosgi.ws=win32 -Dosgi.arch=x86_64 -DtestSuite=%testsToRun%

    ''')
  }

  publishers {
    archiveJunit('**/eclipse-testing/results/xml/*.xml') {
      healthScaleFactor((1.0).doubleValue())
    }
    archiveArtifacts {
      pattern('**/eclipse-testing/results/**, **/eclipse-testing/directorLogs/**, *.properties, *.txt')
    }
    extendedEmail {
      triggers {
        unstable {
          recipientList("sravankumarl@in.ibm.com")
        }
      }
    }
  }
}
