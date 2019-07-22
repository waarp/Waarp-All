echo Start GGFTPServer
. /usr/local/goldengateftp/ENV_GGFTP
nohup ${GGJAVARUNSERVER} goldengate.ftp.exec.ExecGatewayFtpServer ${GGSERVER} 0</dev/null 2>&1 >> ${GGHOME}/log/GGFTPServer.log &
echo $! > ${GGHOME}/log/ggftplastpid
echo GGFTPServer started
