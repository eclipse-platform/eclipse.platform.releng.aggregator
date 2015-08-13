@echo off
SETLOCAL

REM localTestsProperties.bat is not used or expected in production builds,
REM but is needed for production performance tests and
REM allows a place for people to have their own machines variables defined
REM there so they do not have to hand edit each time to do a local build.
REM a typical example is that their version/location/vendor of VM is likely to differ,
REM so they could redefine jvm to what's appropriate to their machine and setup.

IF EXIST localTestsProperties.bat CALL localTestsProperties.bat


REM vm.properties is used by default on production machines, but will
REM need to override on local setups and performance tests
IF NOT DEFINED propertyFile SET propertyFile=vm.properties


REM TODO: not sure it is good to put VM here? Is there a good default here; such as "java"?
REM currently, in practice, we sometimes set in Hudson scripts.
REM https://bugs.eclipse.org/bugs/show_bug.cgi?id=390286
IF NOT DEFINED jvm SET jvm=c:\Program Files\Java\jdk1.7.0_80\jre\bin\java.exe

ECHO jvm in testAll.bat: %jvm%
ECHO extdir in testAll.bat (if any): %extdir%
ECHO propertyFile in testAll.bat: %propertyFile%

mkdir results\consolelogs

set consolelogs=results\consolelogs\%testedPlatform%_consolelog.txt

IF DEFINED extdir (
runtests.bat -extdirprop "%extdir%" -os win32 -ws win32 -arch x86 -vm "%jvm%" -properties %propertyFile%  %* > %consolelogs%
GOTO END
)

runtests.bat -os win32 -ws win32 -arch x86 -vm "%jvm%" -properties %propertyFile%  %* > %consolelogs%

:END
