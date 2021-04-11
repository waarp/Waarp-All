Waarp Proxy R66
=============

You will find in this web site the sub project Waarp Proxy R66.

The global license is GPL V3.

This package is a Proxy for R66 protocol.

By installing a Proxy R66 server, it will forward in both ways requests directly to external or internal R66 servers.
The interest is to have a minimalist R66 server in DMZ, with no configuration that could be a source of attack. The drawback 
is that no control is made within this Proxy R66 server, meaning that the packet are just transmistted as is to the internal
or external R66 partner. However, if some attacks as deny of service are made, this will be probably the first level of catch, 
then enhancing the security level of the R66 solution.

The configuration is made by pair, meaning that each listening interface (address, port, ssl mode) is linked to one and only one 
proxified interface (address, port, ssl mode). Therefore, let say that on internal side we have a R66 server named A, on external 
side a R66 server named B, the configuration will be as follow:

* Listening B' in DMZ through address/port/SSL mode (probably none) accessible from inside, linked to B
* Listening A' in DMZ through address/port/SSL mode (probably yes) accessible from outside, linked to A

Therefore, in A, the configuration to access to B is made through address/port/SSL mode defined in B', 
while the remote partner B will access to A through address/port/SSL mode defined in A'.

Proxy server is started and stopped as a R66 server (command line is java ... classpath ... java options .... config-proxy.xml).

It contains a specific administrator, close to R66 standard one, but will less functionalities. Note the
admin2 web site (in src/main/admin2) is a slightly modified version of the native R66 server.



Waarp is a project that provides, among other packages, 
an open source massive file transfer monitor 
in Java. Its goal is to unify several protocols (FTP, SSH, HTTP and proprietary 
protocols) in an efficient and secured way. Its purpose is to enable bridging between 
several protocols and to enable dynamic pre or post action on transfer or other commands.

Support
-------

Support is available through community and also through commercial support
with the company named [Waarp](http://www.waarp.fr/)

![Waarp Company](http://waarp.github.com/Waarp/res/waarp/waarp.gif "Waarp")

 * Installation and parameters
 * Integration, additional development
 * Support, maintenance, phone support
 
