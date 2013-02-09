@echo off
cd %1
REM test script

REM add the extra binaries to the system path
set PATH=%PATH%;%1\..\windowsBin

REM run all tests
call runtests.bat -vm %cd%\..\jdk7_02\jdk1.7.0_02\jre\bin\javaw -properties vm.properties 1> %2 2>&1

exit