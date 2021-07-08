@ECHO OFF

SET JAVA_OPTS1=-server
IF NOT DEFINED JAVA_XMS call :defineJavaXms
:NextXmx
IF NOT DEFINED JAVA_XMX call :defineJavaXmx
:NextJavaOpts
SET JAVA_OPTS2=-Xms%JAVA_XMS% -Xmx%JAVA_XMX%
IF NOT DEFINED JAVA_HOME call :defineJavaRun
SET JAVA_RUN="%JAVA_HOME%\bin\java"
SET PATH=%JAVA_HOME%\bin;%PATH%

:R66inst
:: Find first instance
IF NOT DEFINED R66_INST (
  for /D %%i in (etc\conf.d\*) do (
    if exist "etc\conf.d\%%i\%R66_TYPE%.xml" (
      SET R66_INST=%%~ni
      GOTO :main
    )
  )
) ELSE IF NOT EXIST "etc\conf.d\%R66_INST%\%R66_TYPE%.xml" (
  echo "L'instance Waarp Gateway FTP %R66_INST% n'existe pas"
  exit /B 2
)

:defineJavaXms
SET JAVA_XMS=256m
goto :NextXmx

:defineJavaXmx
SET JAVA_XMX=1024m
goto :NextJavaOpts

:defineJavaRun
SET JAVA_RUN="java"
goto :R66inst

:main
:: Inialize Conf and Log directory
IF NOT DEFINED CONFDIR SET CONFDIR=etc\conf.d\%R66_INST%

SET LOGGWFTP=-Dlogback.configurationFile="%CONFDIR%\logback-gwftp.xml"

SET FTP_CLASSPATH="share\lib\*"

SET JAVARUNFTPSERVER=%JAVA_RUN% %JAVA_OPTS2% -cp %FTP_CLASSPATH% %LOGGWFTP%