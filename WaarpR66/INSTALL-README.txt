The primary installation of a server is done using the following steps:

1) Create the XML configuration files (Server, Limit, Rule, Authent, SNMP). The minimal configuration is Server and Authent (eventually limited to the created server).
2) Create the necessary GGP (GoldenGate Password files) using the des Key generated both by Waarp Password
3) Create the necessary SSL KeyStore for the HTTPS Administration interface and another one for the SSL server authentication (server only, or server AND client, depending on your choices)
4) Create the database (dependent on the one choose between Oracle, PostgreSql, MySQL or H2 database, the initial creation is out of this documentation).
5) Initialize the database (once created and the user/password and the JDBC access fill in the Server configuration XML file)
6) Now you can run the server and use the various commands or Administration interface to continue the configuration (Limit, Rule, Authent).
