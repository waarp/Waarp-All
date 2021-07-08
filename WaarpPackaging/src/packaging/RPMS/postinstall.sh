upgrade() {
    :
# only restart running instances
service waarp-gwftp restart $(service waarp-gwftp status | \
    grep running | sed -e 's|Waarp Gateway FTP \([^ ]\+\).*|\1|')
service waarp-r66server restart $(service waarp-r66server status | \
    grep running | sed -e 's|Waarp R66 Server \([^ ]\+\).*|\1|')

# running=`systemctl list-units --state=active --no-legend waarp-gwftp* | sed 's/\s.*$//'`
# for I in $running do
#   service $I restart
# done
# running=`systemctl list-units --state=active --no-legend waarp-r66server* | sed 's/\s.*$//'`
# for I in $running do
#   service $I restart
# done
}
_install() {
    :
chkconfig --add waarp-gwftp
chkconfig --add waarp-r66server
}
if [ "${1}" -eq 1 ]
then
    # "after install" goes here
    _install
elif [ "${1}" -gt 1 ]
then
    # "after upgrade" goes here
    upgrade
fi
