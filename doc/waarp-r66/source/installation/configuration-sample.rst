Exemple de Configuration
########################

Les configurations suivantes sont des configurations minimales pour un serveur fonctionel.

client.xml
**********

.. code-block:: xml

  <?xml version="1.0" encoding="UTF-8"?>
  <config xmlns:x0="http://www.w3.org/2001/XMLSchema">
    <comment>Client configuration for server1</comment>
    <identity>
      <hostid>server1</hostid>
      <sslhostid></sslhostid>
      <cryptokey>etc/certs/cryptokey.des</cryptokey>
    </identity>
    <directory>
      <serverhome>.</serverhome>
      <in>./data/in</in>
      <out>./data/out</out>
      <arch>./temp/arch</arch>
      <work>./work/</work>
      <conf>./temp/conf</conf>
      <extendedtaskfactories>org.waarp.openr66.s3.taskfactory.S3TaskFactory</extendedtaskfactories>
    </directory>
    <db>
      <dbdriver>postgresql</dbdriver>
      <dbserver>jdbc:postgresql://localhost:5432/server1</dbserver>
      <dbuser>waarp</dbuser>
      <dbpasswd>waarp</dbpasswd>
      <dbcheck>false</dbcheck>
    </db>
  </config>

server.xml
**********

.. code-block:: xml

  <?xml version="1.0" encoding="UTF-8"?>
  <config xmlns:x0="http://www.w3.org/2001/XMLSchema">
    <comment>Configuration file for a server with a Postgresql database</comment>
    <identity>
      <hostid>server1</hostid>
      <sslhostid></sslhostid>
      <cryptokey>etc/certs/cryptokey.des</cryptokey>
    </identity>
    <server>
      <serveradmin>admin</serveradmin>
      <serverpasswd>5a4b7c6a66065cbb622acefec8c3a302</serverpasswd>
      <usenossl>True</usenossl>
      <usessl>False</usessl>
      <usehttpcomp>False</usehttpcomp>
      <uselocalexec>False</uselocalexec>
      <httpadmin>share/admin-i18n</httpadmin>
      <admkeypath>etc/certs/adminkey.jks</admkeypath>
      <admkeystorepass>password</admkeystorepass>
      <admkeypass>password</admkeypass>
      <checkaddress>False</checkaddress>
      <checkclientaddress>False</checkclientaddress>
      <pastlimit>86400000</pastlimit>
      <minimaldelay>5000</minimaldelay>
      <multiplemonitors>1</multiplemonitors>
      <pushmonitorurl>http://127.0.0.1:8999</pushmonitorurl>
      <pushmonitorendpoint>/log</pushmonitorendpoint>
      <pushmonitordelay>1000</pushmonitordelay>
    </server>
    <network>
      <serverport>6666</serverport>
      <serversslport>6667</serversslport>
      <serverhttpport>8066</serverhttpport>
      <serverhttpsport>8067</serverhttpsport>
    </network>
    <directory>
      <serverhome>.</serverhome>
      <in>./data/in</in>
      <out>./data/out</out>
      <arch>./temp/arch</arch>
      <work>./work/</work>
      <conf>./temp/conf</conf>
      <extendedtaskfactories>org.waarp.openr66.s3.taskfactory.S3TaskFactory</extendedtaskfactories>
    </directory>
    <db>
      <dbdriver>postgresql</dbdriver>
      <dbserver>jdbc:postgresql://localhost:5432/server1</dbserver>
      <dbuser>waarp</dbuser>
      <dbpasswd>waarp</dbpasswd>
      <dbcheck>false</dbcheck>
    </db>
  </config>

authent.xml
***********

.. code-block:: xml

  <authent>
    <entry>
      <hostid>server1</hostid>
      <address>127.0.0.1</address>
      <port>6666</port>
      <isssl>false</isssl>
      <key>password</key>
    </entry>
    <entry>
      <hostid>server2</hostid>
      <address>127.0.0.4</address>
      <port>6668</port>
      <isssl>false</isssl>
      <key>password</key>
    </entry>
  </authent>

rule.xml
********

.. code-block:: xml

  <rules>
    <rule>
      <idrule>defaut</idrule>
      <comment>The default transfer rule</comment>
      <hostids>
        <hostid>server1</hostid>
        <hostid>server2</hostid>
      </hostids>
      <mode>1</mode>
      <rpretasks>
        <tasks></tasks>
      </rpretasks>
      <rposttasks>
        <tasks></tasks>
      </rposttasks>
      <rerrortasks>
        <tasks></tasks>
      </rerrortasks>
      <spretasks>
        <tasks></tasks>
      </spretasks>
      <sposttasks>
        <tasks>
          <task>
            <type>DELETE</type>
            <path></path>
            <delay>0</delay>
          </task>
        </tasks>
      </sposttasks>
      <serrortasks>
        <tasks></tasks>
      </serrortasks>
    </rule>
  </rules>

