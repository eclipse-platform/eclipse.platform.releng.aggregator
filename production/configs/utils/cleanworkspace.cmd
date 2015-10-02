@echo off
SETLOCAL ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION
echo start cleaning ...
IF NOT DEFINED WORKSPACE (
    echo ERROR: WORKSPACE variable was not defined.
    exit /B 1
    ) ELSE (
      IF NOT EXIST "%WORKSPACE%" (
        echo ERROR: WORKSPACE was defined but it did not exists.
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
          set /a sleepTime=60
          echo sleepTime: !sleepTime!
          set /a currentLoop=0

          set /a nFilesOrDirs=0
          for /D %%f in ("%WORKSPACE%\*") do set /a nFilesOrDirs+=1
          for    %%f in ("%WORKSPACE%\*") do set /a nFilesOrDirs+=1
          echo currentLoop: !currentLoop!   nFilesOrDirs:  !nFilesOrDirs!

          :LOOP
          IF !nFilesOrDirs! GTR 0 (
            rem this first for loop is for all subdirectories of workspace
            FOR /D %%p IN ("%WORKSPACE%\*") DO (
              echo removing dir: %%p
              rmdir "%%p" /s /q
              )
            rem this for loop is for for all files remaining, directly under workspace
            FOR %%p IN ("%WORKSPACE%\*") DO (
              echo deleting file: %%p
              del "%%p"  /q
              )
            set /a currentLoop+=1
            IF !currentLoop! GTR !maxLoops! GOTO MAXLOOPS
            set /a nFilesOrDirs=0
            for /D %%f in ("%WORKSPACE%\*") do set /a nFilesOrDirs+=1
            for    %%f in ("%WORKSPACE%\*") do set /a nFilesOrDirs+=1
            echo currentLoop: !currentLoop!   nFilesOrDirs:  !nFilesOrDirs!
            if !nFilesOrDirs! GTR 0 (
              rem Pause a bit before retrying, since if we could not delte, likely due to some process still running.
              C:\Windows\System32\timeout.exe /t !sleepTime! > NUL
              GOTO LOOP
              )
            )
         )
     )
GOTO END

:MAXLOOPS
echo Reached max loops waiting for files to be free to delete
GOTO END


:END
echo ... ending cleaning
ENDLOCAL
