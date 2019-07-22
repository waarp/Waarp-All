
Install prunmgr.exe and prunsrv.exe in the sub directory windows to be able to run Waarp R66 as a service under Windows.
Fix the service.bat file to fit your installation.
Note that you must use the correct version (32 or 64 bits) according to your system, and the same for the JVM used.

See:
http://commons.apache.org/proper/commons-daemon/binaries.html

Note that under windows, Apache daemon does not accept cleanly path with blank (" ") characters. So choose only path without blank character.
For Java, if needed (or other directories), the trick could be to make an alias of the directory to another one without blank character.

For instance:
This one necessites administrator rights.
mklink /D C:\Java "C:\Program Files\Java"
This one not, but can be not always functional.
mklink /J C:\Java "C:\Program Files\Java"

In addition, in spooled.conf file, note the following attention points:
- no " must be used
- \ must be replaced by / in all path, or doubled as \\ if really needed (for instance in regex such as .*\.zip would become .*\\.zip)
- for boolean value, false is meant by commented line, true by uncommenting. For instance:
parallel=
#sequential=
means parallel=true and sequential=false
- in multi value fields, separate multiple values by a comma (,):
to=host1,host2
- you have 2 choices to specify the configuration of the SpooledDirectoryTransfer daemon
a) all configuration is in the spooled.conf (as shown), then the xml configuration file is a standard client configuration
b) all configuration is in the xml configuration file in SpooledDirectory format (example shown too), then the spooled.conf contains only the line xmlfile=yourconfigration.xml

