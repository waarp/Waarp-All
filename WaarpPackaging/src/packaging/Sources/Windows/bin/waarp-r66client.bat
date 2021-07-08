@ECHO OFF
SETLOCAL ENABLEEXTENSIONS

:: Go root directory
SET BINDIR=%~dp0
for %%a in ("%BINDIR:~0,-1%") DO SET "ROOT=%%~dpa"
cd /d "%ROOT%"

:: Get Waarp R66 instance called
SET R66_TYPE=client
if exist "etc\conf.d\%1\%R66_TYPE%.xml" (
SET R66_INST=%1
    shift
)

:: set memory settings
SET JAVA_XMX=1024m
IF DEFINED WAARP_XMX SET JAVA_XMX=%WAARP_XMX%
SET JAVA_XMS=128m
IF DEFINED WAARP_XMS SET JAVA_XMS=%WAARP_XMS%

CALL "%BINDIR%variables.bat"

IF NOT DEFINED CONFDIR SET CONFDIR=etc
IF NOT DEFINED CLIENTCONF SET CLIENTCONF=%CONFDIR%\client.xml
IF NOT DEFINED AUTHENTCONF SET AUTHENTCONF=%CONFDIR%\authent.xml
IF NOT DEFINED RULESDIR SET RULESDIR=%CONFDIR%
SET "_args=%*"

SET ACTION=%1
SHIFT
SET "_args=%_args:* =%"
SET "_args=%_args:* =%"

IF "%ACTION%"=="send" (
    CALL :r66_send %_args%
) ELSE IF "%ACTION%"=="asend" (
    CALL :r66_asend %_args%
) ELSE IF "%ACTION%"=="msend" (
    CALL :r66_msend %_args%
) ELSE IF "%ACTION%"=="masend" (
    CALL :r66_masend %_args%
) ELSE IF "%ACTION%"=="spool" (
    CALL :r66_spool %_args%
) ELSE IF "%ACTION%"=="gui" (
    CALL :r66_gui %_args%
) ELSE IF "%ACTION%"=="getinfo" (
    CALL :r66_getinfo %_args%
) ELSE IF "%ACTION%"=="transfer" (
    CALL :r66_transfer %_args%
) ELSE IF "%ACTION%"=="initdb" (
    CALL :r66_initdb
) ELSE IF "%ACTION%"=="loadauth" (
    CALL :r66_loadauth %1
) ELSE IF "%ACTION%"=="loadrule" (
    CALL :r66_loadrule %1
) ELSE IF "%ACTION%"=="loadconf" (
    CALL :r66_loadconf
) ELSE IF "%ACTION%"=="log-export" (
    CALL :r66_logexport
) ELSE IF "%ACTION%"=="config-export" (
    CALL :r66_configexport
) ELSE IF "%ACTION%"=="watcher" (
    CALL :r66_watcher %1
) ELSE IF "%ACTION%"=="message" (
    CALL :r66_message %_args%
) ELSE IF "%ACTION%"=="icaptest" (
    CALL :r66_icaptest %_args%
) ELSE (
    CALL :usage
)

EXIT /B %ERRORLEVEL%


::--------------------------------------------------------
::-- The function section starts below
::--------------------------------------------------------

:usage
    ECHO usage: %SCRIPTNAME% INSTANCE command options
    ECHO.
    ECHO This script controls Waarp R66 client.
    ECHO given options will be fed to Waarp.
    ECHO.
    ECHO Available commands:
    ECHO.
    ECHO     send            Sends a file to the server
    ECHO     asend           Submits an asynchronous transfer
    ECHO     msend           Sends multiple files to the server
    ECHO     masend          Submits multiple asynchronous transfers
    ECHO     spool           Watches a directory to send files created there
    ECHO     gui             Starts the GUI transfer tool
    ECHO     getinfo         Gets information of files on remote hosts
    ECHO     transfer        Gets information about a transfer.
    ECHO                     Gives the possibility to restart/stop/cancel it.
    ECHO     initdb          Initialize the database
    ECHO     log-export      Exports transfer history
    ECHO     config-export   Exports configuration
    ECHO     loadauth        Loads the authentication information in database
    ECHO     loadrule        Loads transfer rules in database
    ECHO     loadconf        Loads the configuration in database
    ECHO     watcher start   Starts the filewatcher
    ECHO     watcher install Installs the filewatcher in the service manager
    ECHO     watcher remove  Removes the filewatcher from the service manager
    ECHO     message         Ping a message to check connectivity
    ECHO     icaptest        Sends a file to an icap server
    ECHO     help            Displays this message
    EXIT /B 0

:r66_send
    %JAVARUNCLIENT% org.waarp.openr66.client.DirectTransfer %CLIENTCONF% %*
    EXIT /B %ERRORLEVEL%

:r66_asend
    %JAVARUNCLIENT% org.waarp.openr66.client.SubmitTransfer %CLIENTCONF% %*
    EXIT /B %ERRORLEVEL%

:r66_msend
    %JAVARUNCLIENT% org.waarp.openr66.client.MultipleDirectTransfer %CLIENTCONF% %*
    EXIT /B %ERRORLEVEL%

:r66_masend
    %JAVARUNCLIENT% org.waarp.openr66.client.MultipleSubmitTransfer %CLIENTCONF% %*
    EXIT /B %ERRORLEVEL%

:r66_spool
    %JAVARUNCLIENT% org.waarp.openr66.client.MultipleSubmitTransfer %CLIENTCONF% %*
    EXIT /B %ERRORLEVEL%

:r66_gui
    %JAVARUNCLIENT% org.waarp.openr66.r66gui.R66ClientGui %CLIENTCONF% %*
    EXIT /B %ERRORLEVEL%

:r66_getinfo
    %JAVARUNCLIENT% org.waarp.openr66.client.RequestInformation %CLIENTCONF% %*
    EXIT /B %ERRORLEVEL%

:r66_message
    %JAVARUNCLIENT% org.waarp.openr66.client.Message %CLIENTCONF% %*
    EXIT /B %ERRORLEVEL%

:r66_transfer
    %JAVARUNCLIENT% org.waarp.openr66.client.RequestTransfer %CLIENTCONF% %*
    EXIT /B %ERRORLEVEL%

:r66_initdb
    %JAVARUNCLIENT% org.waarp.openr66.server.ServerInitDatabase %CLIENTCONF% -initdb %*
    EXIT /B %ERRORLEVEL%

:r66_loadauth
    %JAVARUNCLIENT% org.waarp.openr66.server.ServerInitDatabase %CLIENTCONF% -auth %1
    EXIT /B %ERRORLEVEL%

:r66_loadrule
    %JAVARUNCLIENT% org.waarp.openr66.server.ServerInitDatabase %CLIENTCONF% -dir %1
    EXIT /B %ERRORLEVEL%

:r66_loadlimit
    %JAVARUNCLIENT% org.waarp.openr66.server.ServerInitDatabase %CLIENTCONF% -limit %1
    EXIT /B %ERRORLEVEL%

:r66_loadconf
    CALL :r66_loadauth %AUTHENTCONF% || EXIT /B 1
    CALL :r66_loadrule %RULESDIR% || EXIT /B 1
    EXIT /B 0

:r66_logexport
    %JAVARUNCLIENT% org.waarp.openr66.server.LogExport %CLIENTCONF% %*
    EXIT /B 0

:r66_configexport
    %JAVARUNCLIENT% org.waarp.openr66.server.ConfigExport %CLIENTCONF% %*
    EXIT /B 0

:r66_watcher
    SET SUBACTION=%1
    SHIFT

    IF "%SUBACTION%"=="start" (
        CALL :r66_fw_start
    ) ELSE IF "%SUBACTION%"=="install" (
        CALL :r66_fw_install
    ) ELSE IF "%SUBACTION%"=="remove" (
        CALL :r66_fw_remove
    ) ELSE IF "%SUBACTION%"=="restart" (
        CALL :r66_fw_restart
    ) ELSE (
        CALL :usage
    )
    EXIT /B %ERRORLEVEL%

:r66_fw_start
    ECHO "Starting Waarp R66 Filewatcher... "
    %JAVARUNCLIENT% org.waarp.openr66.client.SpooledDirectoryTransfer %CLIENTCONF%
    ECHO "done"
    EXIT /B %ERRORLEVEL%


:r66_fw_install
	%BINDIR%nssm install "WaarpR66 %R66_INST% Filewatcher" %BINDIR%waarp-r66client.bat %R66_INST% watcher start
	%BINDIR%nssm set "WaarpR66 %R66_INST% Filewatcher" AppDirectory %ROOT%
	%BINDIR%nssm set "WaarpR66 %R66_INST% Filewatcher" Description "Waarp R66 Filewatcher (%R66_INST%)"
	%BINDIR%nssm set "WaarpR66 %R66_INST% Filewatcher" AppStopMethodConsole 60000
    %BINDIR%nssm set "WaarpR66 %R66_INST% Filewatcher" AppRestartDelay 300000
    %BINDIR%nssm set "WaarpR66 %R66_INST% Filewatcher" AppnoConsole 1
    EXIT /B %ERRORLEVEL%

:r66_fw_remove
	%BINDIR%nssm remove "WaarpR66 %R66_INST% Filewatcher" confirm
    EXIT /B %ERRORLEVEL%

:r66_fw_restart
    Powershell -command "Restart-Service 'WaarpR66 %R66_INST% Filewatcher' -Force"
    EXIT /B %ERRORLEVEL%

:r66_icaptest
    %JAVARUNCLIENT% org.waarp.icap.IcapScanFile %*
    EXIT /B %ERRORLEVEL%