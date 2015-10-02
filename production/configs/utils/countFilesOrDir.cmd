@echo off
setlocal enableextensions
set nFilesOrDirs=0
for /D %%f in ("%WORKSPACE%\*") do set /a nFilesOrDirs+=1
for    %%f in ("%WORKSPACE%\*") do set /a nFilesOrDirs+=1
echo %nFilesOrDirs%
endlocal
