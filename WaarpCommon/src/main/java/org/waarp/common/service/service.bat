@echo off

rem -- DO NOT CHANGE THIS ! OR YOU REALLY KNOW WHAT YOU ARE DOING ;)

rem -- Organization: 
rem -- EXEC_PATH is root (pid will be there)
rem -- EXEC_PATH\..\logs\ will be the log place
rem -- EXEC_PATH\windows\ is where prunsrv.exe is placed
rem -- DAEMON_ROOT is where all you jars are (even commons-daemon)
rem -- DAEMON_NAME will be the service name
rem -- SERVICE_DESCRIPTION will be the service description
rem -- MAIN_DAEMON_CLASS will be the start/stop class used

rem inspired from: http://algorithmique.net/Dev/2011/04/11/crer-un-dmon-ou-un-service-pour-uix-et-windows-en-java-avec-commons-deamon.html
rem and http://tanakanbb.blogspot.fr/2012/04/commons-daemon-2.html
rem and http://commons.apache.org/daemon/procrun.html and associated apache daemon wiki

rem -- Root path where the executables are
set EXEC_PATH=C:\Waarp\Run

rem -- Change this by the path where all jars are
set DAEMON_ROOT=C:\Waarp\Classpath

rem -- Service description
set SERVICE_DESCRIPTION="My Java Service"

rem -- Service name
set SERVICE_NAME=MyService

rem -- Service CLASSPATH
set SERVICE_CLASSPATH=%DAEMON_ROOT%\myjar.jar

rem -- Service main class
set MAIN_SERVICE_CLASS=org.waarp.xxx.service.ServiceLauncher

rem -- Path for log files
set LOG_PATH=%EXEC_PATH%\..\logs

rem -- STDERR log file: IMPORTANT SINCE LOG will be there according to logback.xml
set ERR_LOG_FILE=%LOG_PATH%\stderr.txt

rem -- STDOUT log file: IMPORTANT SINCE LOG will be there according to logback.xml
set OUT_LOG_FILE=%LOG_PATH%\stdout.txt

rem -- Startup mode (manual or auto)
set SERVICE_STARTUP=auto

rem -- JVM option (auto or full path to jvm.dll, if possible pointing to server version)
rem example: C:\Program Files\Java\jdk1.7.0_05\jre\bin\server\jvm.dll
set JVMMODE=--Jvm=auto

rem -- Java memory options
set JAVAxMS=64m
set JAVAxMX=512m

rem -- Logback configuration file: IMPORTANT recommended to configure to STDOUT
set LOGBACK_CONF=%EXEC_PATH%\..\conf\logback.xml

rem -- prunsrv.exe location
set PRUNSRVEXEC=%EXEC_PATH%\windows\prunsrv.exe

rem -- Loglevel of Daemon between debug, info, warn, error
set LOGLEVEL=info

rem ---------------------------------------------------------------------------
rem -- Various Java options
set JAVA_OPTS=--JvmMs=%JAVAxMS% --JvmMx=%JAVAxMX% %JAVASERVER% ++JvmOptions=-Dlogback.configurationFile=%LOGBACK_CONF%

set SERVICE_OPTIONS=%JAVA_OPTS% --Description=%SERVICE_DESCRIPTION% %JVMMODE% --Classpath=%SERVICE_CLASSPATH% --StartMode=jvm --StartClass=%MAIN_SERVICE_CLASS% --StartMethod=windowsStart --StopMode=jvm --StopClass=%MAIN_SERVICE_CLASS% --StopMethod=windowsStop --LogPath=%LOG_PATH% --StdOutput=%OUT_LOG_FILE% --StdError=%ERR_LOG_FILE% --Startup=%SERVICE_STARTUP% --PidFile=service.pid --LogLevel=%LOGLEVEL%

set RESTART=0

:GETOPTS
if /I "%1" == "start" ( goto START )
if /I "%1" == "stop" ( goto STOP )
if /I "%1" == "console" ( goto CONSOLE )
if /I "%1" == "restart" ( goto RESTART )
if /I "%1" == "install" ( goto INSTALL )
if /I "%1" == "remove" ( goto REMOVE )

goto HELP

rem -- START ------------------------------------------------------------------
:START

echo Start service %SERVICE_NAME%
%PRUNSRVEXEC% //RS/%SERVICE_NAME% %SERVICE_OPTIONS%

goto FIN

rem -- INSTALL ----------------------------------------------------------------
:INSTALL

echo Install service %SERVICE_NAME%
%PRUNSRVEXEC% //IS/%SERVICE_NAME% %SERVICE_OPTIONS%

goto FIN

rem -- STOP -------------------------------------------------------------------
:STOP

echo Stop service %SERVICE_NAME%
%PRUNSRVEXEC% //SS/%SERVICE_NAME% %SERVICE_OPTIONS%

if "%RESTART%" == "1" ( goto START )
goto FIN

rem -- REMOVE -----------------------------------------------------------------
:REMOVE

echo Remove service %SERVICE_NAME%
%PRUNSRVEXEC% //DS/%SERVICE_NAME% %SERVICE_OPTIONS%

goto FIN

rem -- CONSOLE ----------------------------------------------------------------
:CONSOLE

%PRUNSRVEXEC% //TS/%SERVICE_NAME% %SERVICE_OPTIONS%

goto FIN

rem -- RESTART ----------------------------------------------------------------
:RESTART

set RESTART=1

goto STOP

rem -- HELP -------------------------------------------------------------------
:HELP

echo "service.bat install|remove|start|stop|restart"
goto FIN

:FIN
