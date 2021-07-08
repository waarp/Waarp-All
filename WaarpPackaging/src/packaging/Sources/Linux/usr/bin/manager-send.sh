#!/usr/bin/env bash
#set -x

CURDIR=$(cd "$(dirname "$0")" && pwd)
BINDIR=$CURDIR
MANAGER_DIR=${MANAGER_DIR:-$(pwd)}
HOSTID=
INST_ID=
CONF_PACKAGE=
TMPDIR=$(mktemp -d)

CONF_ROOT=$BINDIR/../etc
if [[ ! -d $CONF_ROOT/conf.d ]]; then
	CONF_ROOT=/etc/waarp
fi

MANAGER_CONF=${MANAGER_CONF:-${MANAGER_DIR}/etc/waarp-manager.ini}
if [[ ! -f $MANAGER_CONF ]]; then
	MANAGER_CONF=/etc/waarp-manager/waarp-manager.ini
fi


if [ -f "$CONF_ROOT/conf.d/$1/manager-send.conf" ]; then 
    .  "$CONF_ROOT/conf.d/$1/manager-send.conf" ]
fi


cleanup() {
    rm -rf "$TMPDIR"
}
trap cleanup EXIT

##
## FUNCTIONS 
##


set-hostid() {
  local file

  for i in $(seq $#); do
    if [ "${!i}" = "-file" ]; then
      i=$((i+1))
      CONF_PACKAGE=$(readlink -e ${!i})
      file=$(basename "${CONF_PACKAGE}")
      HOSTID=${file%-*}
    fi
  done
  INST_ID=$(pgquery "SELECT id from partners where hostid='$HOSTID'" -tA)
}

get-manager-db-conf() {
    local DB_HOST=$(awk -F "=" '/Host/ {print $2}' $MANAGER_CONF | sed 's/ //g')
    local DB_PORT=$(awk -F "=" '/Port/ {print $2}' $MANAGER_CONF | sed 's/ //g' | tail -n 1)
    local DB_USER=$(awk -F "=" '/User/ {print $2}' $MANAGER_CONF | sed 's/ //g')
    local DB_PASS=$(awk -F "=" '/Password/ {print $2}' $MANAGER_CONF | sed 's/ //g')
    local DB_NAME=$(awk -F "=" '/^\s*Name/ {print $2}' $MANAGER_CONF | sed 's/ //g')

    echo "postgresql://$DB_USER:$DB_PASS@$DB_HOST:$DB_PORT/$DB_NAME"
}

pgquery() {
    psql "$(get-manager-db-conf)" -c  "$@"
}

is-gwsftp() {
    [ $(pgquery "SELECT type FROM partners WHERE hostid='$HOSTID'" -tA) = "gwsftp" ]
}

add-passwd() {
    pgquery "select sftp_username, sftp_password from partners where sftp_gateway='$INST_ID'" -t -A -F, | while read line; do
        local user=$(echo $line | cut -d, -f 1)
        local pass=$(echo $line | cut -d, -f 2)

        if [ "$pass" = "" ]; then
            pass=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)
        fi

        local FTPASSWD=$BINDIR/../share/tools/ftpasswd
        if [[ ! -f $FTPASSWD ]]; then
            FTPASSWD=/usr/share/waarp/ftpasswd
        fi

        echo $pass | $FTPASSWD --passwd --file $TMPDIR/sftp.passwd \
            --name $user --uid 65534 --home /tmp \
            --shell /bin/false --stdin >/dev/null 2>&1
    done

    [ -f $TMPDIR/sftp.passwd ] || touch $TMPDIR/sftp.passwd
    add-to-package sftp.passwd
}

add-auth() {
    pgquery "select sftp_username from partners where sftp_gateway='$INST_ID'" -t -A -F, | while read line; do
        local user=$line
        local key="$(pgquery "select sftp_public_key from partners where sftp_gateway='$INST_ID' and sftp_username='$user'" -t -A -F,)"

        if [ ! "$key" = "" ]; then
            echo -e "$key" > $TMPDIR/$user.authorized_keys
            add-to-package $user.authorized_keys
        fi
    done
}

add-keys() {
    pgquery "select gwsftp_public_key from partners where id='$INST_ID'" -t -A -F, > $TMPDIR/id_rsa.pub
    add-to-package id_rsa.pub

    pgquery "select gwsftp_private_key from partners where id='$INST_ID'" -t -A -F, > $TMPDIR/id_rsa
    add-to-package id_rsa
}

add-get-files() {
    local hostid_column="hostid$([[ "$PULL_USE_SSL" == "true" ]] && echo "ssl")"
    local query="SELECT origin.$hostid_column, flows.name, rules.name
    FROM rules
    JOIN partners AS origin ON origin.id=rules.origin_id
    JOIN flows ON flows.id=rules.flow_id
    JOIN partners AS dest ON dest.id=rules.destination_id
    WHERE rules.mode='receive' AND dest.type='r66' AND dest.id='$INST_ID'
    ORDER BY origin.hostid"
    
    local content="$(pgquery "$query" -t -F, -A)"
    if [ ! "$content" = "" ]; then
        echo -e "$content" > $TMPDIR/get-files.list
        add-to-package get-files.list
    fi
}

add-get-sftp-files() {
    local query="SELECT flows.name,
	                    rules.name,
						origin.ip,
						origin.sftp_server_port,
						origin.sftp_authent,
						origin.sftp_username,
						origin.sftp_password,
						(CASE WHEN flows.params::json->>'SFTPREMOTEDIR'=''
						      THEN
							     '/'
							  ELSE
							     flows.params::json->>'SFTPREMOTEDIR'
						END)
				 FROM rules JOIN partners AS origin ON origin.id=rules.origin_id
				 JOIN flows ON flows.id=rules.flow_id
				 JOIN partners AS dest ON dest.id=rules.destination_id
				 WHERE rules.mode='send'
                   AND origin.type='sftp'
                   AND origin.is_server=true
                   AND dest.id='$INST_ID'
                 ORDER BY origin.hostid"
    
    local content="$(pgquery "$query" -t -F, -A)"
    if [ ! "$content" = "" ]; then
        echo -e "$content" > "$TMPDIR/get-files.list"
        add-to-package get-files.list
    fi
}

add-get-ftp-files() {
    local query="SELECT flows.name,
	                    rules.name,
						origin.ip,
						origin.ftp_server_port,
						origin.ftp_server_username,
						origin.ftp_server_password,
                        origin.ftp_server_directory,
                        origin.ftp_server_secure,
                        origin.ftp_server_mode
				 FROM rules JOIN partners AS origin ON origin.id=rules.origin_id
				 JOIN flows ON flows.id=rules.flow_id
				 JOIN partners AS dest ON dest.id=rules.destination_id
				 WHERE rules.mode='send'
                   AND origin.type='ftp'
                   AND origin.is_server=true
                   AND dest.id='$INST_ID'
                 ORDER BY origin.hostid"
    
    local content="$(pgquery "$query" -t -F, -A)"
    if [ ! "$content" = "" ]; then
        echo -e "$content" > "$TMPDIR/get-files.list"
        add-to-package get-files.list
    fi
}


add-user-dir() {
    local query="SELECT origin.sftp_username, flows.name
    FROM flows
    JOIN rules ON flows.id=rules.flow_id
    JOIN partners AS origin ON origin.id=rules.origin_id
    JOIN partners AS destination ON destination.id=rules.destination_id
    WHERE origin.type='sftp' AND destination.id='$INST_ID'"
    pgquery "$query" -t -A -F, | while read line; do
        local hostid=$(echo $line | cut -d, -f 1)
        local flow=$(echo $line | cut -d, -f 2)

        echo -e "$hostid"/"$flow" >> $TMPDIR/user_dir.list
    done
    if [ -f $TMPDIR/user_dir.list ]; then
        add-to-package user_dir.list
    fi
}

add-to-package() {
    cd $TMPDIR
    zip -q $CONF_PACKAGE $1
}

##
## MAIN 
##

set-hostid $@

is-gwsftp

if is-gwsftp; then
    add-passwd
    add-auth
    add-keys
    add-user-dir
fi
add-get-files
add-get-sftp-files
add-get-ftp-files

if [[ -f $BINDIR/waarp-r66client.sh ]]; then
	$BINDIR/waarp-r66client.sh $@
else
	$BINDIR/waarp-r66client $@
fi