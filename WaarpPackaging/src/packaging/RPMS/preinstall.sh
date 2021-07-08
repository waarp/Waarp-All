_install() {
    :
getent group waarp >/dev/null || groupadd -r waarp
getent passwd waarp >/dev/null || \
    useradd -r -g waarp -d /var/lib/waarp -s /bin/bash \
    -c "Waarp user" --create-home waarp
chkconfig --add waarp-gwftp
}
if [ "${1}" -eq 1 ]
then
    # "before install" goes here
    _install
fi
