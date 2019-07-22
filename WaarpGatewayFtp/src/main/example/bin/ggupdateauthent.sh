#
if [[ $# -gt 0 ]]
then
  AUTHCMD="`cat /usr/local/goldengateftp/config/authupdate.cmd` $1"
  ftp `hostname` << END
${AUTHCMD}
END

else
  ftp `hostname` < /usr/local/goldengateftp/config/authupdate.cmd
fi
