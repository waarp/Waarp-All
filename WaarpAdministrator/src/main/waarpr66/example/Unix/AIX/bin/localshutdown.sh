#!/bin/sh
LASTPID=$1
. /appli/R66/ENV_R66
if [[ "${LASTPID}x" != "x" ]]
then
   echo Will use PID ${LASTPID}
else
   LASTPID=`cat ${R66HOME}/log/lastpid`
fi
if [[ "${LASTPID}X" != "X" ]]
then
  echo try shutting down locally
  kill -s USR1 ${LASTPID}
else
  echo no process seems to be running
fi

