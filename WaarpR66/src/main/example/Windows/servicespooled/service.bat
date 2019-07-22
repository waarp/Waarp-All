@echo off

rem -- DO NOT CHANGE THIS ! OR YOU REALLY KNOW WHAT YOU ARE DOING ;)

rem -- Organization: 
rem -- EXEC_PATH is root (pid will be there)
rem -- EXEC_PATH\logs\ will be the log place
rem -- EXEC_PATH\conf\ will be the configuration place
rem -- EXEC_PATH\bin\windows\service\ is where prunsrv.exe is placed
rem -- DAEMON_ROOT = EXEC_PATH\lib by default, is where all you jars are (even commons-daemon)
rem -- NOTE: NO SPACE ARE ALLOWED in the DAEMON_ROOT path!!!
rem -- DAEMON_NAME will be the service name
rem -- SERVICE_DESCRIPTION will be the service description
rem -- MAIN_DAEMON_CLASS will be the start/stop class used

rem -- Root path where the executables are
set EXEC_PATH=J:\GG\R66

rem -- Change this by the path where all jars are
set DAEMON_ROOT=%EXEC_PATH%\lib

rem -- Service description
set SERVICE_DESCRIPTION="Waarp R66 SpooledDirectory Service"

rem -- Service name
set SERVICE_NAME=WaarpSpooled

rem -- Service CLASSPATH
set SERVICE_CLASSPATH=%DAEMON_ROOT%\WaarpR66-2.4.25.jar;%DAEMON_ROOT%\*

rem -- Service main class
set MAIN_SERVICE_CLASS=org.waarp.openr66.client.spooledService.SpooledServiceLauncher

rem -- Path for log files
set LOG_PATH=%EXEC_PATH%\logs

rem -- STDERR log file: IMPORTANT SINCE LOG will be there according to logback.xml
set ERR_LOG_FILE=%LOG_PATH%\stderr-%SERVICE_NAME%.txt

rem -- STDOUT log file: IMPORTANT SINCE LOG will be there according to logback.xml
set OUT_LOG_FILE=%LOG_PATH%\stdout-%SERVICE_NAME%.txt

rem -- Startup mode (manual or auto)
set SERVICE_STARTUP=auto

rem -- JVM option (auto or full path to jvm.dll, if possible pointing to server version)
rem example: set JVMMODE=--Jvm=C:\Java\jdk1.7.0_05\jre\bin\server\jvm.dll
rem example2 (note JAVA_HOME must be set too): set JAVA_HOME=C:\Outils\Java\jdk1.7.0_45
rem exemple2 continue: set JVMMODE=--Jvm=%JAVA_HOME%\jre\bin\server\jvm.dll --JavaHome %JAVA_HOME%
rem exemple3 with JRE: set JVMMODE=--Jvm=%JAVA_HOME%\bin\server\jvm.dll --JavaHome %JAVA_HOME%
rem exemple4 (may not work): set JVMMODE=--Jvm=auto
set JAVA_HOME=C:\Outils\Java\jdk1.7.0_45
set JVMMODE=--Jvm=%JAVA_HOME%\jre\bin\server\jvm.dll --JavaHome %JAVA_HOME%

rem -- Java memory options
set JAVAxMS=64m
set JAVAxMX=512m

rem -- Logback configuration file: ATTENTION recommendation is to configure output to STDOUT or STDERR
set LOGBACK_CONF=%EXEC_PATH%\conf\logback-client.xml

rem -- R66 Spooled configuration file
set R66_CONF=%EXEC_PATH%\conf\spooled.conf

rem -- prunsrv.exe location
set PRUNSRVEXEC=%EXEC_PATH%\bin\windows\service\prunsrv.exe

rem -- prunmgr.exe location
set PRUNMGREXEC=%EXEC_PATH%\bin\windows\service\prunmgr.exe

rem -- Loglevel of Daemon between debug, info, warn, error
set LOGLEVEL=info

rem ---------------------------------------------------------------------------
rem -- Various Java options
set JAVA_OPTS=%JVMMODE% --JvmMs=%JAVAxMS% --JvmMx=%JAVAxMX% ++JvmOptions=-Dlogback.configurationFile=%LOGBACK_CONF% ++JvmOptions=-Dorg.waarp.r66.config.file=%R66_CONF% ++JvmOptions=-Dopenr66.locale=en 

set SERVICE_OPTIONS=%JAVA_OPTS% --Description=%SERVICE_DESCRIPTION% --Classpath=%SERVICE_CLASSPATH% --StartMode=jvm --StartClass=%MAIN_SERVICE_CLASS% --StartMethod=windowsStart --StopMode=jvm --StopClass=%MAIN_SERVICE_CLASS% --StopMethod=windowsStop --LogPath=%LOG_PATH% --StdOutput=%OUT_LOG_FILE% --StdError=%ERR_LOG_FILE% --Startup=%SERVICE_STARTUP% --PidFile=service-%SERVICE_NAME%.pid --LogLevel=%LOGLEVEL%

set RESTART=0

:GETOPTS
if /I "%1" == "start" ( goto START )
if /I "%1" == "stop" ( goto STOP )
if /I "%1" == "console" ( goto CONSOLE )
if /I "%1" == "restart" ( goto RESTART )
if /I "%1" == "install" ( goto INSTALL )
if /I "%1" == "remove" ( goto REMOVE )
if /I "%1" == "monitor" ( goto MONITOR )

goto HELP

rem -- START ------------------------------------------------------------------
:START

echo Start service %SERVICE_NAME%
%PRUNSRVEXEC% //ES/%SERVICE_NAME% 

goto FIN

rem -- INSTALL ----------------------------------------------------------------
:INSTALL

echo Install service %SERVICE_NAME%
%PRUNSRVEXEC% //IS/%SERVICE_NAME% %SERVICE_OPTIONS%

goto FIN

rem -- STOP -------------------------------------------------------------------
:STOP

echo Stop service %SERVICE_NAME%
%PRUNSRVEXEC% //SS/%SERVICE_NAME% 

if "%RESTART%" == "1" ( goto START )
goto FIN

rem -- REMOVE -----------------------------------------------------------------
:REMOVE

echo Remove service %SERVICE_NAME%
%PRUNSRVEXEC% //DS/%SERVICE_NAME%

goto FIN

rem -- CONSOLE ----------------------------------------------------------------
:CONSOLE

%PRUNSRVEXEC% //TS/%SERVICE_NAME%

goto FIN

rem -- RESTART ----------------------------------------------------------------
:RESTART

set RESTART=1

goto STOP

rem -- MONITOR ----------------------------------------------------------------
:MONITOR

%PRUNMGREXEC% //MS/%SERVICE_NAME%

goto FIN

rem -- HELP -------------------------------------------------------------------
:HELP

echo "service.bat install|remove|start|stop|restart|console|monitor"
goto FIN

:FIN
