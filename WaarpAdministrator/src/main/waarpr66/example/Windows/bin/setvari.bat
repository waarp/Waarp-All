@echo off
REM JDK SUN
SET JAVA_OPTS1=-server
SET JAVA_OPTS2=-Xms256m -Xmx4096m
SET JAVA_RUN="C:\Program Files\Java\jdk1.6.0_14\jre\bin\java" %JAVA_OPTS1% %JAVA_OPTS2% 
SET JAVASERVER_RUN="C:\Program Files\Java\jdk1.6.0_14\jre\bin\javaw" %JAVA_OPTS1% %JAVA_OPTS2% 

SET R66HOME=D:\GG\R66Client\
SET R66BIN=%R66HOME%\lib

REM command for Client

REM Logger
SET LOGSERVER=" -Dlogback.configurationFile=%R66HOME%\conf\logback.xml "
SET LOGCLIENT=" -Dlogback.configurationFile=%R66HOME%\conf\logback-client.xml "

SET R66_CLASSPATH=" %R66BIN%\WaarpR66-2.4.14.jar;%R66BIN%\* "

SET JAVARUNCLIENT=%JAVA_RUN% -cp %R66_CLASSPATH% %LOGCLIENT% 
SET JAVARUNSERVER=%JAVASERVER_RUN% -cp %R66_CLASSPATH% %LOGSERVER% 

SET SERVER_CONFIG="%R66HOME%/conf/config-serverA.xml"
SET CLIENT_CONFIG="%R66HOME%/conf/config-clientA.xml"

REM ################
REM # R66 COMMANDS #
REM ################

REM # SERVER SIDE #
REM ###############

REM # start the OpenR66 server
REM # no option
set R66SERVER=%JAVARUNSERVER% openr66.server.R66Server %SERVER_CONFIG%

REM # init database from argument
REM # [ -initdb ] [ -dir rulesDirectory ] [ -limit xmlFileLimit ] [ -auth xmlFileAuthent ]
set R66INIT=%JAVARUNCLIENT% openr66.server.ServerInitDatabase %SERVER_CONFIG%

REM # export configuration into directory
REM # directory
set R66CNFEXP=%JAVARUNCLIENT% org.waarp.openr66.server.ServerExportConfiguration %SERVER_CONFIG% 

REM # export configuration as arguments
REM # [-hosts] [-rules]
set R66CONFEXP=%JAVARUNCLIENT% org.waarp.openr66.server.ConfigExport %SERVER_CONFIG% 

REM # import configuration as arguments
REM # [-hosts host-configuration-file] [-purgehosts] [-rules rule-configuration-file] [-purgerules]
set R66CONFIMP=%JAVARUNCLIENT% org.waarp.openr66.server.ConfigImport %SERVER_CONFIG% 

REM # shutdown locally the server
REM # PID  optional PID of the server process
set R66SIGNAL="taskkill /T /PID "

REM # shutdown by network the server
REM # [-nossl|-ssl] default = -ssl
set R66SHUTD=%JAVARUNCLIENT% org.waarp.openr66.server.ServerShutdown %SERVER_CONFIG% 

REM # export the log
REM # [ -purge ] [ -clean ] [ -start timestamp ] [ -stop timestamp ] where timestamp are in yyyyMMddHHmmssSSS format eventually truncated and with possible ':- ' as separators
set R66EXPORT=%JAVARUNCLIENT% org.waarp.openr66.server.LogExport %SERVER_CONFIG% 

REM # change limits of bandwidth
REM # "[ -wglob x ] [ -rglob w ] [ -wsess x ] [ -rsess x ]"
set R66LIMIT=%JAVARUNCLIENT% org.waarp.openr66.server.ChangeBandwidthLimits %SERVER_CONFIG% 

REM # Administrator Gui
REM # no argument
set R66ADMIN=%JAVARUNCLIENT% org.waarp.administrator.AdminGui %SERVER_CONFIG% 

REM # CLIENT SIDE #
REM ###############

REM # asynchronous transfer
REM # (-to hostId -file filepath -rule ruleId) | (-to hostId -id transferId) [ -md5 ] [ -block size ] [ -nolog ] [-start yyyyMMddHHmmssSSS | -delay +durationInMilliseconds | -delay preciseTimeInMilliseconds] [ -info "information" ]
set R66SEND=%JAVARUNCLIENT% org.waarp.openr66.client.SubmitTransfer %CLIENT_CONFIG% 

REM # synchronous transfer
REM # (-to hostId -file filepath -rule ruleId) | (-to hostId -id transferId) [ -md5 ] [ -block size ] [ -nolog ] [-start yyyyMMddHHmmssSSS | -delay +durationInMilliseconds | -delay preciseTimeInMilliseconds] [ -info "information" ]
set R66SYNCSEND=%JAVARUNCLIENT% org.waarp.openr66.client.DirectTransfer %CLIENT_CONFIG% 

REM # get information on transfers
REM # (-id transferId -to hostId as requested | -id transferId -from hostId as requester) 
REM # follow by one of: (-cancel | -stop | -restart [ -start yyyyMMddHHmmss | -delay +durationInMilliseconds | -delay preciseTimeInMilliseconds ]
set R66REQ=%JAVARUNCLIENT% org.waarp.openr66.client.RequestTransfer %CLIENT_CONFIG% 

REM # get information on remote files or directory
REM # "-to host -rule rule [ -file file ] [ -exist | -detail | -list | -mlsx ]
set R66INFO=%JAVARUNCLIENT% org.waarp.openr66.client.RequestInformation  %CLIENT_CONFIG% 

REM # test the connectivity
REM # -to host -msg "message"
set R66MESG=%JAVARUNCLIENT% org.waarp.openr66.client.Message %CLIENT_CONFIG% 

REM # Gui interface
REM # no argument
set R66GUI=%JAVARUNCLIENT% org.waarp.openr66.r66gui.R66ClientGui %CLIENT_CONFIG% 


REM R66 Multiple Submit
REM (-to hostId,hostID -file filepath,filepath -rule ruleId) | (-to hostId -id transferId) [ -md5 ] [ -block size ] [ -nolog ] [-start yyyyMMddHHmmssSSS | -delay +durationInMilliseconds | -delay preciseTimeInMilliseconds] [ -info "information" ]
set R66MULTISEND=%JAVARUNCLIENT% org.waarp.openr66.client.MultipleSubmitTransfer %CLIENT_CONFIG% 

REM synchronous transfer
REM (-to hostId,hostid -file filepath,filepath -rule ruleId) | (-to hostId -id transferId) [ -md5 ] [ -block size ] [ -nolog ] [-start yyyyMMddHHmmssSSS | -delay +durationInMilliseconds | -delay preciseTimeInMilliseconds] [ -info "information" ]
set R66MULTISYNCSEND=%JAVARUNCLIENT% org.waarp.openr66.client.MultipleDirectTransfer %CLIENT_CONFIG%

