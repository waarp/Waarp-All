@ECHO OFF
SETLOCAL ENABLEEXTENSIONS

SET SCRIPTNAME=%~nx0
SET BINDIR=%~dp0

CALL "%BINDIR%variables.bat"

%JAVARUNCLIENT% org.waarp.uip.WaarpPassword %*