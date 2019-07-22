@echo off

REM Script to allow to restart the R66 Server
RE% Specially useful when used in a task after upgrade of binary components

REM Change path if necesary
call C:\appli\R66\setvari.bat

REM Give a try to one or the other
REM same shell
REM %R66SHUTD% & ping -n 61 127.0.0.1 >nul & %R66SERVER%
REM new shell in background
start /b ( %R66SHUTD% & ping -n 61 127.0.0.1 >nul & %R66SERVER% )



