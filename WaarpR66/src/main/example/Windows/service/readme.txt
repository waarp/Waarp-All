
Install prunmgr.exe and prunsrv.exe in the sub directory windows to be able to run Waarp R66 as a service under Windows.
Fix the service.bat file to fit your installation.

See:
http://commons.apache.org/proper/commons-daemon/binaries.html

Note that under windows, Apache daemon does not accept cleanly path with blank (" ") characters. So choose only path without blank character.
For Java, if needed (or other directories), the trick could be to make an alias of the directory to another one without blank character.

For instance:
This one necessites administrator rights.
mklink /D C:\Java "C:\Program Files\Java"
This one not, but can be not always functional.
mklink /J C:\Java "C:\Program Files\Java"
