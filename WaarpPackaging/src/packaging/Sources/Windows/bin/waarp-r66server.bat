@ECHO OFF
SETLOCAL ENABLEEXTENSIONS

:: Go root directory
SET BINDIR=%~dp0
for %%a in ("%BINDIR:~0,-1%") DO SET "R66HOME=%%~dpa"
cd /d "%R66HOME%"

:: Get Waarp R66 instance called
SET R66_TYPE=server
if exist "etc\conf.d\%1\%R66_TYPE%.xml" (
SET R66_INST=%1
    shift
)

:: set memory settings
SET JAVA_XMX=2048m
IF DEFINED WAARP_XMX SET JAVA_XMX=%WAARP_XMX%
SET JAVA_XMS=512m
IF DEFINED WAARP_XMS SET JAVA_XMS=%WAARP_XMS%

:: Initialize variables
CALL "%BINDIR%variables.bat"

IF NOT DEFINED CONFDIR SET CONFDIR=etc
IF NOT DEFINED SERVERCONF SET SERVERCONF=%CONFDIR%\server.xml
IF NOT DEFINED AUTHENTCONF SET AUTHENTCONF=%CONFDIR%\authent.xml
IF NOT DEFINED RULESDIR SET RULESDIR=%CONFDIR%
SET "_args=%*"

SET ACTION=%1
SHIFT
SET "_args=%_args:* =%"
SET "_args=%_args:* =%"

IF "%ACTION%"=="start" (
    CALL :r66_start
) ELSE IF "%ACTION%"=="install" (
    CALL :r66_install
) ELSE IF "%ACTION%"=="remove" (
    CALL :r66_remove
) ELSE IF "%ACTION%"=="initdb" (
    CALL :r66_initdb
) ELSE IF "%ACTION%"=="loadconf" (
    CALL :r66_loadconf
) ELSE IF "%ACTION%"=="loadauth" (
    CALL :r66_loadauth %1
) ELSE IF "%ACTION%"=="loadrule" (
    CALL :r66_loadrule %1
) ELSE (
    CALL :usage %_args%
)

EXIT /B %ERRORLEVEL%


::--------------------------------------------------------
::-- The function section starts below
::--------------------------------------------------------

:usage

    ECHO usage: %SCRIPTNAME% command options
    ECHO.
    ECHO This script controls Waarp R66 server.
    ECHO given options will be fed to Waarp.
    ECHO.
    ECHO Available commands:
    ECHO.
    ECHO     start          Starts the server
    ECHO     install        Installs Waarp R66 as a windows service
    ECHO     remove         Removes Waarp R66 from the Windows services
    ECHO     initdb         Initialize the database
    ECHO     loadauth       Loads the authentication information in database
    ECHO     loadrule       Loads transfer rules in database
    ECHO     loadconf       Loads the configuration in database
    ECHO     help           Displays this message
    EXIT /B 0



:r66_start
    ECHO "Starting Waarp R66 Server... "
    %JAVARUNSERVER% org.waarp.openr66.server.R66Server %SERVERCONF%
    ECHO "done"
    EXIT /B %ERRORLEVEL%


:r66_install
    %BINDIR%nssm install "WaarpR66 %R66_INST%" %BINDIR%waarp-r66server.bat %R66_INST% start
    %BINDIR%nssm set "WaarpR66 %R66_INST%" AppDirectory %R66HOME%
    %BINDIR%nssm set "WaarpR66 %R66_INST%" Description "Waarp R66 Server (%R66_INST%)"
    %BINDIR%nssm set "WaarpR66 %R66_INST%" AppRestartDelay 300000
    %BINDIR%nssm set "WaarpR66 %R66_INST%" AppStopMethodConsole 60000
    %BINDIR%nssm set "WaarpR66 %R66_INST%" AppnoConsole 1
    EXIT /B %ERRORLEVEL%

:r66_remove
    %BINDIR%nssm remove "WaarpR66 %R66_INST%" confirm
    EXIT /B %ERRORLEVEL%


:r66_initdb
    %JAVARUNSERVER% org.waarp.openr66.server.ServerInitDatabase %SERVERCONF% -initdb %*
    EXIT /B %ERRORLEVEL%

:r66_loadauth
    %JAVARUNSERVER% org.waarp.openr66.server.ServerInitDatabase %SERVERCONF% -auth %1
    EXIT /B %ERRORLEVEL%


:r66_loadrule
    %JAVARUNSERVER% org.waarp.openr66.server.ServerInitDatabase %SERVERCONF% -dir %1
    EXIT /B %ERRORLEVEL%

:r66_loadlimit
    %JAVARUNSERVER% org.waarp.openr66.server.ServerInitDatabase %SERVERCONF% -limit %1
    EXIT /B %ERRORLEVEL%

:r66_loadconf
    CALL :r66_loadauth %AUTHENTCONF% || EXIT /B 1
    CALL :r66_loadrule %RULESDIR% || EXIT /B 1
    EXIT /B 0