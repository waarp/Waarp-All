##################
``snmpconfig.xml``
##################


.. index:: snmpconfig.xml

.. _snmp-xml:

Le fichier ``server.xml`` contient la configuration des serveurs
snmpconfig avec lesquels une instance peut communiquer.

.. note::

   Les changements dans ce fichier sont pris en compte au redémarrage du serveur.

Les directives de configuration sont réparties en 3 sections :

- :ref:`config <snmp-xml-config>`: paramétrage du système SNMP de
  l'instance Waarp
- :ref:`targets <snmp-xml-targets>`: liste des serveurs SNMP à utiliser
- :ref:`securities <snmp-xml-securities>`: données d'authentification
  SNMP

.. _snmp-xml-config:

Section ``config``
--------------------

=============== ======= ==== ====== =============
Balise          Type    Obl. Défaut Signification
=============== ======= ==== ====== =============
localaddress    string  O           Adresse IP sur laquelle le service SNMP doit écouter au format ``udp:address/port`` ou ``tcp:address/port`` (peut être renseigné plusieurs fois)
nbthread        string  N    4      Nombre de threads à utiliser pour le serveur SNMP
filtered        boolean N    False  Active le filtrage des connexions SNMPv1 or SNMPv2c entrantes sur l'adresse IP du client.
usetrap         string  N    True   Utilise des messages "traps" (``True``) ou "inform" (``False``) Lors de l'envoi d'événement à un serveur SNMP
trapinformlevel integer N    0      Défini le niveau de criticité des messages à envoyer : 0: Aucun ; 1: Démarrage/Arrêt du serveur ; 2: messages de niveau critique ; 3: messages de niveau erreur ; 4: messages de niveau warning ; 5: tous les événements
=============== ======= ==== ====== =============



.. _snmp-xml-targets:

Section ``targets``
--------------------

La section ``targets`` regroupe la liste des serveurs SNMP auxquels
envoyer des événements.

Chaque serveur est défini dans un bloc XML ``target`` acceptant les
balises suivantes (voir :ref:`snmp-xml-example`) :

=============== ======= ==== ======= =============
Balise          Type    Obl. Défaut  Signification
=============== ======= ==== ======= =============
name            string  O            Nom à utiliser pour le serveur
domain          string  N    UdpIpv4 Domaine à utiliser pour le serveur. Doit être une des valeurs suivantes : ``UdpIpV4``, ``UdpIpv6``, ``UdpIpV4e``, ``UdpIpv6z``, ``TcpIpv4``, ``TcpIpv6``, ``TcpIpv4z``, ``TcpIpv6z``
address         string  O            Adresse du serveur au format ``adress/port``
timeout         integer N    200     Délais maximum d'attente de réponse pour les messages "inform" (en ms)
retries         integer N    1       Nombre de tentative d'envoi des messages "inform" non acquittés par le serveur
isv2            boolean N    True    Défini si les serveur utilise le protocole SNMPv2c (``True``) ou SNMPv3 (``False``)
=============== ======= ==== ======= =============


.. _snmp-xml-securities:

Section ``securities``
----------------------


La section ``securities`` défini les paramètres de sécurité pour
SNMPv3. Plusieurs profils peuvent être définis.

Chaque profil est défini dans un bloc XML ``security`` acceptant les
balises suivantes (voir :ref:`snmp-xml-example`) :


==================== ======= ==== ======= =============
Balise               Type    Obl. Défaut  Signification
==================== ======= ==== ======= =============
securityname         string  O            Nom à utiliser
securityauthprotocol string  N            Protocole à utiliser pour l'authentification. Doit être une des valeurs suivantes : ``MD5``, ``SHA``
securityauthpass     string  N            Mot de passe pour l'authentification (peut être vide)
securityprivprotocol string  N            Protocole à utiliser pour le chiffrement. Doit être une des valeurs suivantes : ``P3DES``, ``PAES128``, ``PAES192``, ``PAES256``, ``PDES``
securityprivpass     string  N            Mot de passe pour le chiffrement (peut être vide)
==================== ======= ==== ======= =============



.. _snmp-xml-example:

Exemple complet
---------------

.. code-block:: xml

   <?xml version="1.0" encoding="UTF-8"?>
   <snmpconfig xmlns:x0="http://www.w3.org/2001/XMLSchema">
      <config>
         <localaddress>udp:0.0.0.0/2001</localaddress>
         <localaddress>tcp:0.0.0.0/2002</localaddress>
         <nbthread>4</nbthread>
         <filtered>False</filtered>
         <usetrap>True</usetrap>
         <trapinformlevel>4</trapinformlevel>
      </config>
      <targets>
         <target>
            <name>notificationV2c</name>
            <domain>UdpIpv4</domain>
            <address>127.0.0.1/162</address>
            <timeout>200</timeout>
            <retries>1</retries>
            <isv2>True</isv2>
         </target>
         <target>
            <name>notificationV3</name>
            <domain>UdpIpv4</domain>
            <address>127.0.0.1/162</address>
            <timeout>200</timeout>
            <retries>1</retries>
            <isv2>False</isv2>
         </target>
      </targets>
      <securities>
         <security>
            <securityname>SHADES</securityname>
            <securityauthprotocol>SHA</securityauthprotocol>
            <securityauthpass>SHADESAuthPassword</securityauthpass>
            <securityprivprotocol>PDES</securityprivprotocol>
            <securityprivpass>SHADESPrivPassword</securityprivpass>
         </security>
      </securities>
   </snmpconfig>
