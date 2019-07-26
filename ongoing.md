ToDo
====
FTP
***
- Add XSITE SHAxxx (business handler) and Junits for XSITE xxx


R66 Proxy
*********
- Add Junits

GWFTP
*****
- Add Junits

All
***
- Allow 2 kinds of configuration (xml / json)
- Clean dependencies (not used)
- Change to Guava where possible
- Vitam plugin
- Fix memory

R66
***
- Add Junits on forward send
- Add Junits on snmp and http (rest/restV2/UI) and monitoring
- Check if properties used?
- Test with just start and stop for every DB types
- Test for XML DAO ?
- Test for RescheduleTransfer and Transfer and SpooledInform and
  RestartServer and LinkRename and FtpTransfer and ExecMove Tasks
- Test for Java Tasks (AddDigest, AddUuid)
- Test for SpooledDirectoryTransfer
- Test for kernel Exec (Execute, Java, LocalExec, LogJava, 
    R66PreparedTransfer) ?
- Look at REST V2
- See if Rest V1 is available while V2 active (right now only V2 active)

GatewayKernel
*************
- Improve Junits

Common
******
- Use new way for SslContext in Netty

Done
====
All
***
- Fix await on Netty Future
- Fix await on R66Future
- Fix Bootstrap (deprecated 2 thread groups)
- Remove unused main()
- Remove @author for Waarp team
- Add FIXME for wrong usage of DbAdmin, clean elsewhere
- Effort to remove duplicate codes

Common
******
- Add ConcurrentUtility.newConcurrentSet() utility (deprecation from Netty)
- R66Future deprecation of await/awaitUniterruptibly for awaitOrInterrupted
- Fix XML reading if possible (compatible with reformat)

FTP
***
- Fix Threads

R66
***
- Fix Threads (still some left)
- Fix memory (mainly KeepAlive and Startup/Validation)
- Fix Business request
- Add a specific file for Version of protocol (stick to 3.1.0)
