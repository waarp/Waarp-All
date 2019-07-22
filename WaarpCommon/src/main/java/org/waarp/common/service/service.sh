#!/bin/bash

# DO NOT CHANGE THIS ! OR YOU REALLY KNOW WHAT YOU ARE DOING ;)

# Organization: 
# EXEC_PATH is root (pid will be there)
# EXEC_PATH/../logs/ will be the log place
# EXEC_PATH/../bin/ is where jsvc is placed
# DAEMON_ROOT is where all you jars are (even commons-daemon)
# DAEMON_NAME will be the service name
# MAIN_DAEMON_CLASS will be the start/stop class used

# Change this to adapt the path where the pid should be
export EXEC_PATH=/waarp/run
# could be also `dirname $0`

# Change this by the path where all jars are
export DAEMON_ROOT=/waarp/classpath

# Change this by the name of your daemon
export DAEMON_NAME="WaarpDaemon"

# Change this to match your classpath
export DAEMON_CLASSPATH=${DAEMON_ROOT}/commons-daemon-1.0.10.jar:${DAEMON_ROOT}/myjar.jar

# Change this to specify the PID file path and name
export PID_FILE=${EXEC_PATH}/service.pid

# Change this to match you Daemon class
export MAIN_DAEMON_CLASS=org.waarp.xxx.service.ServiceLauncher

# Change this to specify the stdout file
export STDOUT_FILE=${EXEC_PATH}/../logs/stdout.txt

# Change this to specify the stderr file
export STDERR_FILE=${EXEC_PATH}/../logs/stderr.txt

# Java memory options
export JAVAxMS=-Xms64m
export JAVAxMX=-Xmx512m

# JVM server option
export JAVASERVER=-jvm=server
#export JAVASERVER=-jvm=client

# Logback configuration file
export LOGBACK_CONF=-Dlogback.configurationFile=/waarp/conf/logback.xml

# Add -debug if you want to run in debug mode
export JSVC_OPTIONS=${JAVAxMS} ${JAVAxMX} ${JAVASERVER} ${LOGBACK_CONF}  

# -----------------------------------------------------------------------------

export OS_TYPE=`uname`
if [ "x${OS_TYPE}" == "xDarwin" ]; then
   export EXEC="arch -arch i386 "${EXEC_PATH}"/jsvc"
else
   export EXEC=${EXEC_PATH}"/jsvc"
fi

running() {
   if [ -f ${PID_FILE} ]; then
      echo ${DAEMON_NAME}" already running."
      exit 0
   fi
}

start() {
   running
   ${EXEC} \
      -cp ${DAEMON_CLASSPATH} \
      -outfile ${STDOUT_FILE} \
      -errfile ${STDERR_FILE} \
      -pidfile ${PID_FILE} \
      ${JSVC_OPTIONS} \
      ${MAIN_DAEMON_CLASS}
}

stop() {
   ${EXEC} \
      -cp ${DAEMON_CLASSPATH} \
      -outfile ${STDOUT_FILE} \
      -errfile ${STDERR_FILE} \
      -pidfile ${PID_FILE} \
      ${JSVC_OPTIONS} \
      -stop \
      ${MAIN_DAEMON_CLASS}
}

case "$1" in
   'start')
      echo "Starting "${DAEMON_NAME}"..."
      start
      ;;
   'stop')
      echo "Stopping "${DAEMON_NAME}"..."
      stop
      ;;
   'status')
      if [ -f ${PID_FILE} ]; then
         PID=`cat ${PID_FILE}`
         echo ${DAEMON_NAME}" is running PID: "${PID}
      else
         echo ${DAEMON_NAME}" is not running!"
      fi
      ;;
   'restart')
      $0 stop
      sleep 5
      $0 start
      ;;
   *)
      echo $0 "start|stop|status|restart"
      exit 1
      ;;
esac
exit 0
