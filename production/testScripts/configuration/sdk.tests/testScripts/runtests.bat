@echo off
SETLOCAL

REM default java executable for outer and test vm, in case not 
REM passed into this script.
set jvm=java

REM reset list of ant targets in test.xml to execute
set tests=

REM default switch to determine if eclipse should be reinstalled between running of tests
set installmode=clean

REM property file to pass to Ant scripts
set properties=

REM default values for os, ws and arch
set os=win32
set ws=win32
set arch=x86

REM reset ant command line args
set ANT_CMD_LINE_ARGS=

IF EXIST localTestsProperties.bat CALL localTestsProperties.bat


REM ****************************************************************
REM
REM Install Eclipse if it does not exist
REM
REM ****************************************************************
if NOT EXIST eclipse unzip -qq -o eclipse-SDK-*.zip && unzip -qq -o -C eclipse-junit-tests*.zip plugins/org.eclipse.test* -d eclipse/dropins/


:processcmdlineargs

REM ****************************************************************
REM
REM Process command line arguments
REM
REM ****************************************************************

if x%1==x goto run
if x%1==x-ws set ws=%2 && shift && shift && goto processcmdlineargs
if x%1==x-os set os=%2 && shift && shift && goto processcmdlineargs
if x%1==x-arch set arch=%2 && shift && shift && goto processcmdlineargs
if x%1==x-noclean set installmode=noclean&& shift && goto processcmdlineargs
if x%1==x-properties set properties=-propertyfile %2 && shift && shift && goto processcmdlineargs
if x%1==x-vm set jvm=%2 && shift && shift && goto processcmdlineargs


set tests=%tests% %1 && shift && goto processcmdlineargs

echo Specified test targets (if any): %tests%

:run
REM ***************************************************************************
REM  Run tests by running Ant in Eclipse on the test.xml script
REM ***************************************************************************
REM get name of org.eclipse.equinox.launcher_*.jar with version label
dir /b eclipse\plugins\org.eclipse.equinox.launcher_*.jar>launcher-jar-name.txt
set /p launcher-jar=<launcher-jar-name.txt


echo "list all environment variables in effect as tests start"
set

IF NOT EXIST %jvm% (
ECHO ERROR: jvm not defined or does not exist: %jvm%
exit 1
)

REM -XshowSettings is supported on windows VMs but ... not every where. So where not supported
REM causes VM to not start at all. Can be handy for diagnostics. (without running ant <echoproperties/>

%jvm% %ANT_OPTS% -Dosgi.os=%os% -Dosgi.ws=%ws% -Dosgi.arch=%arch% -jar eclipse\plugins\%launcher-jar% -data workspace -application org.eclipse.ant.core.antRunner -file test.xml %tests% -Dws=%ws% -Dos=%os% -Darch=%arch% -D%installmode%=true %properties% -logger org.apache.tools.ant.DefaultLogger

:END

