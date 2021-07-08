JAVA_XMX=${WAARP_XMX:-1024m}
JAVA_XMS=${WAARP_XMS:-256m}

JAVA_HOME=${JAVA_HOME:-$(dirname $(dirname $(readlink -f $(which java))))}
JAVA_OPTS1="-server"
JAVA_OPTS2="-Xms${JAVA_XMS} -Xmx${JAVA_XMX}"
JAVA_RUN="${JAVA_HOME}/bin/java"

PATH=${JAVA_HOME}/bin:$PATH

#find first instance
if [[ -z $R66_INST ]]; then
    for inst in $(ls /etc/waarp/conf.d); do
        if [[ -e "/etc/waarp/conf.d/$inst/$R66_TYPE.xml" ]]; then
            R66_INST=$inst
            break
        fi
    done
else
    if [[ ! -e "/etc/waarp/conf.d/$R66_INST/$R66_TYPE.xml" ]]; then
        echo "L'instance Waarp R66 $R66_INST n'existe pas"
        exit 2
    fi
fi

CONFDIR=${CONFDIR:-/etc/waarp/conf.d/$R66_INST}
FTPPIDFILE=/var/lib/waarp/$R66_INST/gwftp.pid

LOGGWFTP=" -Dlogback.configurationFile=${CONFDIR}/logback-gwftp.xml "

FTP_CLASSPATH="/usr/share/waarp/lib/*"

JAVARUNFTPSERVER="${JAVA_RUN} ${JAVA_OPTS1} ${JAVA_OPTS2} -cp ${FTP_CLASSPATH} ${LOGGWFTP} "