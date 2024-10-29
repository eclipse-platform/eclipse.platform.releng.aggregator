@echo off
SETLOCAL

REM localTestsProperties.bat is not used or expected in production builds,
REM but is needed for production performance tests and
REM allows a place for people to have their own machines variables defined
REM there so they do not have to hand edit each time to do a local build.
REM a typical example is that their version/location/vendor of VM is likely to differ,
REM so they could redefine jvm to what's appropriate to their machine and setup.

IF EXIST localTestsProperties.bat CALL localTestsProperties.bat


IF NOT DEFINED propertyFile (
  echo expect 'propertyFile' as environment variable for production runs
  exit 1
)
IF NOT DEFINED jvm (
  echo expect 'jvm' as environment variable for production runs
  exit 1
)
IF NOT DEFINED testedPlatform (
  echo expect 'testedPlatform' as environment variable for production runs
  exit 1
)

IF NOT DEFINED eclipseArch SET eclipseArch=x86_64

@echo on
ECHO === properties in testAll.bat
ECHO     DOWNLOAD_HOST: %DOWNLOAD_HOST%
ECHO     jvm in testAll: %jvm%
ECHO     extdir in testAll (if any): %extdir%
ECHO     propertyFile in testAll: %propertyFile%
ECHO     buildId in testAll: %buildId%
ECHO     testedPlatform: %testedPlatform%
ECHO     ANT_OPTS: %ANT_OPTS%

mkdir results\consolelogs

set consolelogs=results\consolelogs\%testedPlatform%_consolelog.txt

IF DEFINED extdir (
runtests.bat -extdirprop "%extdir%" -os win32 -ws win32 -arch %eclipseArch% -vm "%jvm%" -properties %propertyFile% %* > %consolelogs%
GOTO END
)

runtests.bat -os win32 -ws win32 -arch %eclipseArch% -vm "%jvm%" -properties %propertyFile% %* > %consolelogs%

:END
