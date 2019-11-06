Server
######

* `server.xml`: Obligatoire

.. option:: serveradmin

   *String* Obligatoire

   Login pour l'accès à l'interface d'administration

.. option:: serverpasswd

   *String* Obligatoire si <serverpasswdfile> n'est pas renseigné

   Mot de passe de l'interface d'administration administration chiffré avec <cryptokey> de la partie `idenity`

.. option:: serverpasswdfile

   *String* Obligatoire si <serverpasswd> n'est pas renseigné

   Fichier du mot de passe de l'interface d'administration administration chiffré avec <cryptokey> de la partie `idenity`

.. option:: usenossl

   *Boolean* (True)

   Authorise les communication non chiffrées

.. option:: usessl

   *Boolean* (False)

   Authorise les communication chiffrées

.. option:: usehttpcomp

   *Boolean* (False)

   Authorise la compression HTTP pour l'interface d'administration

.. option:: uselocalexec

   *Boolean* (False)

   By default, use the System.exec() but can lead to limitation in performances (JDK limitation). The usage of the GoldenGate LocalExec Daemon tends to reach better performances through execution delegation

.. option:: lexecaddr

   *Address* (127.0.0.1)

   Addresse du LocalExec Daemon

.. option:: lexecport

   *Integer* (9999)

   Port du LocalExec Daemon

.. option:: httpadmin

   *Directory*

   Dossier racine des de l'interface d'adminitration HTTPS

.. option:: admkeypath

   *JKS-File* Obligatoire

   JKS KeyStore pour l'accès à l'interface d'adminsitration. (Contient le certificat serveur)

.. option:: admkeystorepass

   *String* Obligatoire

   Mot de passe du keystore <admkeypath>

.. option:: admkeypass

   *String* Obligatoire

   Mot de passe pour le certificat serveur du keystore <admkeypath>

.. option:: checkaddress

   *Boolean* (False)

   Force R66 à verifier l'adresse IP distance lors de la validation de la connection

.. option:: checkclientaddress

   *Boolean* (False)

   Force R66 à verifier aussi l'addresse IP distante du client

.. option:: pastlimit

   *Integer* (86400000)

   Monitoring: delai (ms) avant de revenir au monitoring

.. option:: minimaldelay

   *Integer* (5000)

   Monitoring: interval minimal (ms) avant the le retour au vrai monitoring

.. option:: snmpconfig

   *XML-File* Optionel

   Fichier XML de configuration du service SNMP

.. option:: multiplemonitors

   *Integer* (1)

   Defini le nombre de moniteur agissant comme une unique instance WaarpR66 en tant que cluster 

.. option:: businessfactory

   *String* Optionel

   Spécifie la classe Business Factory utilisée. La classe `org.waarp.openr66.context.R66DefaultBusinessFactory` est utilisée par défaut. Elle ecrits les logs uniquement en mode DEBUG

