#!/bin/sh -e

mkdir -p /var/run/c-icap
chown c-icap:c-icap /var/run/c-icap

exec c-icap -D -N -f /etc/c-icap/c-icap.conf
