if [ "${1}" -eq 0 ]
then
    :
service waarp-gwftp stop all
chkconfig --del waarp-gwftp

service waarp-r66server stop all
chkconfig --del waarp-r66server
fi
