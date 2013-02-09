@echo off
SETLOCAL

REM localTestsProperties.bat is not used or expected in production builds,
REM but is needed for production performance tests and
REM allows a place for people to have their own machines variables defined
REM there so they do not have to hand edit each time to do a local build.
REM a typical example is that their version/location/vendor of VM is likely to differ,
REM so they could redefine vmcmd to what's appropriate to their machine and setup.

IF EXIST localTestsProperties.bat CALL localTestsProperties.bat


REM vm.properties is used by default on production machines, but will
REM need to override on local setups and performance tests
IF NOT DEFINED propertyFile SET propertyFile=vm.properties

REM TODO: not sure it is good to put VM here? Is there a good default here; such as "java"?
REM currently, in practice, we sometimes set in hudson scripts.
REM https://bugs.eclipse.org/bugs/show_bug.cgi?id=390286
IF NOT DEFINED vmcmd SET vmcmd=c:\\java\\jdk1.7.0_07\\jre\\bin\\java.exe

ECHO vmcmd in testAll: %vmcmd%
ECHO extdir in testAll (if any): %extdir%
ECHO propertyFile in testAll: %propertyFile%

mkdir results\consolelogs

IF DEFINED extdir (
runtests.bat -extdirprop "%extdir%" -os win32 -ws win32 -arch x86_64 -vm %vmcmd% -properties %propertyFile%  %* > results\consolelogs\win7consolelog.txt
GOTO END
)

runtests.bat -os win32 -ws win32 -arch x86_64 -vm %vmcmd% -properties %propertyFile%  %* > results\consolelogs\win7consolelog.txt

:END
