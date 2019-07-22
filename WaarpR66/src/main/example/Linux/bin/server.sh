echo Start R66Server
CONFIG_FILE=$1
. /appli/R66/ENV_R66
nohup ${JAVARUNSERVER} org.waarp.openr66.server.R66Server ${CONFIG_FILE} &
set MYPID=$!
sleep 60
MYTEST=`ps -eaf | grep -G '^\s*'${MYPID} | grep -v grep | grep openr66.server.R66Server`
if [ "${MYTEST}" != "" ] then 
	echo ${MYPID} > ${R66HOME}/log/lastpid
	echo R66Server started
else
	echo R66Server failed to start
fi
