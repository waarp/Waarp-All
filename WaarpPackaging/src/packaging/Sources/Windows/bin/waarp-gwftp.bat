@ECHO OFF
SETLOCAL ENABLEEXTENSIONS

:: Go root directory
SET BINDIR=%~dp0
for %%a in ("%BINDIR:~0,-1%") DO SET "R66HOME=%%~dpa"
cd /d "%R66HOME%"

:: Get Waarp R66 instance called
SET R66_TYPE=gwftp
if exist "etc\conf.d\%1\%R66_TYPE%.xml" (
SET R66_INST=%1
    shift
)

:: set memory settings
SET JAVA_XMX=1024m
IF DEFINED WAARP_XMX SET JAVA_XMX=%WAARP_XMX%
SET JAVA_XMS=512m
IF DEFINED WAARP_XMS SET JAVA_XMS=%WAARP_XMS%

:: Initialize variables
CALL "%BINDIR%variables-gwftp.bat"

IF NOT DEFINED CONFDIR SET CONFDIR=etc
IF NOT DEFINED R66CONF SET R66CONF=%CONFDIR%\client.xml
IF NOT DEFINED GWFTPCONF SET GWFTPCONF=%CONFDIR%\gwftp.xml
IF NOT DEFINED AUTHENTCONF SET AUTHENTCONF=%CONFDIR%\authent-ftp.xml

SET ACTION=%1
SHIFT

IF "%ACTION%"=="start" (
    CALL :r66_start %*
) ELSE IF "%ACTION%"=="install" (
    CALL :r66_install %*
) ELSE IF "%ACTION%"=="remove" (
    CALL :r66_remove %*
) ELSE IF "%ACTION%"=="initdb" (
    CALL :r66_initdb %*
) ELSE (
    CALL :usage %*
)

EXIT /B %ERRORLEVEL%


::--------------------------------------------------------
::-- The function section starts below
::--------------------------------------------------------

:usage

    ECHO usage: %SCRIPTNAME% command options
    ECHO.
    ECHO This script controls Waarp Gateway FTP server.
    ECHO Given options will be fed to Waarp.
    ECHO.
    ECHO Available commands:
    ECHO.
    ECHO     start          Starts the server
    ECHO     install        Installs Waarp R66 as a windows service
    ECHO     remove         Removes Waarp R66 from the Windows services
    ECHO     initdb         Initialize the database
    ECHO     help           Displays this message
    EXIT /B 0



:r66_start
    ECHO "Starting Waarp R66 Server... "
    %JAVARUNFTPSERVER% org.waarp.gateway.ftp.ExecGatewayFtpServer %GWFTPCONF% %R66CONF%
    ECHO "done"
    EXIT /B %ERRORLEVEL%


:r66_install
    %BINDIR%nssm install "GWFTP%R66_INST%" %BINDIR%waarp-gwftp.bat %R66_INST% start
    %BINDIR%nssm set "GWFTP%R66_INST%" DisplayName "Waarp Gateway FTP %R66_INST%"
    %BINDIR%nssm set "GWFTP%R66_INST%" AppDirectory %R66HOME%
    %BINDIR%nssm set "GWFTP%R66_INST%" Description "Waarp Gateway FTP (%R66_INST%)"
    %BINDIR%nssm set "GWFTP%R66_INST%" AppRestartDelay 300000
    %BINDIR%nssm set "GWFTP%R66_INST%" AppStopMethodConsole 60000
    EXIT /B %ERRORLEVEL%

:r66_remove
    %BINDIR%nssm remove "GWFTP%R66_INST%" confirm
    EXIT /B %ERRORLEVEL%


:r66_initdb
    %JAVARUNFTPSERVER% org.waarp.gateway.ftp.ServerInitDatabase %GWFTPCONF% -initdb %*
    EXIT /B %ERRORLEVEL%
