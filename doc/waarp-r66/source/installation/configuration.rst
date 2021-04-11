Configuration Simple
####################

Principes Généraux
******************

Il existe 4 fichiers de configurations:

 * client.xml, détail le fonctionnement du moniteur en mode client
 * logback-client.xml, configuration des logs client
 * server.xml, détail le fonctionnement du moniteur en mode serveur
 * logback-server.xml, configuration des logs serveur

En plus de ces 4 fichiers 2 autres fichiers sont consommés par l'instance pour alimenter 
sa configuration en base de données (si applicable)
 
 * authent.xml, détails d'authentification des moniteurs authorisés
 * rules.xml, règles de transfert utilisable par le mooniteur

client.xml
**********

Le tableau ci-dessous détail les groupes utilisés dans la configuration d'un client WaarpR66.
La description de ces groupes est détaillée plus bas.

========== ==============
Balise     Status
========== ==============
identity   Obligatoire
ssl        Si utilisation de SSL
directory  Recommandé
db         Si utilisation d'une base de données
========== ==============

server.xml
**********

Le tableau ci-dessous détail les groupes utilisés dans la configuration d'un serveur WaarpR66.
La description de ces groupes est détaillée plus bas.

========== ==============
Balise     Description
========== ==============
identity   Obligatoire
ssl        Si utilisation de SSL
server     Obligatoire
network    Recommandé
directory  Recommandé
db         Obligatoire
========== ==============

.. note::

  Pour les balises indiquées ci-dessus, les fichiers valeurs renseignées dans les fichiers 
  server.xml et client.xml doivent être identiques. À défaut, deux instances distinctes
  seront configurées.

Détails
*******

Identity
========

Le groupe `<identity>` des configurations client et serveur permet de définir l'identité 
du moniteur (hostids et mot de passe)

============================ ==============
Balise                       Description
============================ ==============
**identity**
hostid                       Identifiant de l’instance utilisé pour les connections en clair
sslhostid                    Identifiant de l’instance utilisé pour les connexions chiffrées
cryptokey                    Chemin d’accès à la clef DES utilisée pour chiffrer les mots de passe
============================ ==============

Server
======

Le groupe `<server>` de la configuration serveur permet de préciser les informations 
nécéssaire au fonctionement du serveur.

============================ ==============
Balise                       Description
============================ ==============
**server**
serveradmin                  Login pour l'accès administrateur
serverpasswd                 Mot de passe pour l'accès administrateur chiffré par <cryptokey>
usenossl                     Le serveur accepte les connections non SSL
usessl                       Le serveur accepte les connections ssl
usehttpcomp                  Utilisation de la compréssion HTTP pour l'interface d'administration
uselocalexec                 Utilisation du LocalExec R66 au lieu du 
httpadmin                    Chemin d’accès au serveur
admkeypath                   Chemin d’accès au keystore pour l’authentification https
admkeystorepass              Mot de passe d’accès au keystore
admkeypass                   Mot de passe d’accès à la clef
============================ ==============

Network
=======

Par defaut un serveur WaarpR66 utilise les ports suivants:

 * 6668: Communications R66 en clair
 * 6669: Communications R66 chiffrées
 * 8066: Interface web de suivi (désactivée par défaut)
 * 8067: Interface web d’administration (désactivée par défaut)
 * 8088: Interface REST des serveurs WaarpR66 autonomes (désactivée par défaut)

Cependant ces ports sont configurables via le groupe `network`.

============================ ==============
Balise                       Description
============================ ==============
**network**
serverport                   Communications R66 en clair
serveraddresses              Adresses utilisées pour le protocole R66 (séparées par des virgules)
serversslport                Communications R66 chiffrées
serverssladdresses           Adresses utilisées pour le protocole R66 en SSL (séparées par des virgules)
serverhttpport               Interface web de suivi (désactivée par défaut)
serverhttpaddresses          Adresses utilisées pour l'interface web de supervision (séparées par des virgules)
serverhttpsport              Interface web d’administration (désactivée par défaut)
serverhttpsaddresses         Adresses utilisées pour l'interface web HTTPS d'administration (séparées par des virgules)
serverrestport               Interface REST des serveurs WaarpR66 autonomes (désactivée par défaut)
============================ ==============

SSL
===

Afin d'utilisé des connexions chiffrées le groupe `<ssl>` doit etre configuré avec les 
catalogues de certificats authorisés et leurs mots de passe d'accès.

============================ ==============
Balise                       Description
============================ ==============
**ssl**
keypath                      Chemin d’accès au keystore d’authentification de l’instance.
keystorepass                 Mot de passe d’accès au keystore
keypass                      Mot de passe d’accès à la clef
trustkeypath                 Chemin d’accès au keystore des certificats de confiance de l’instance
trustkeystorepass            Mot de passe d’accès au trustkeystore
trustuseclientauthenticate   Si vrai, R66 n'acceptera que les clients authorisés via SSL
============================ ==============

Directory
=========

Le groupe `<directory>` permet de définir les dossiers utilisés par les moniteurs WaarpR66
pour l'émission et la réception de fichiers.

============================ ==============
Balise                       Description
============================ ==============
**directory**
serverhome                   Dossier racine de WaarpR66. Les autres dossiers paramétrables sont définis relativement à celui-ci
in                           Dossier de dépôt des fichiers reçus
out                          Dossier où sont cherchés les fichiers à transférer
work                         Dossier tampon où sont stockés les fichiers en cours de réception
arch                         Dossier d’export XML de l’historique des transferts
conf                         Dossier d’export XML de la configuration de l’instance
extendedtaskfactories        Liste (séparée par des virgules) des TaskFactory en tant qu'extension pour ajouter des tâches à WaarpR66
============================ ==============

DB
==

WaarpR66 utilise une base de données pour stocker les informations nécessaires aux transferts 
(Moniteurs authorisés et règles de transferts). Le groupe `<db>` permet de configurer les 
accès à la base de données utilisé par le moniteur.

============================ ==============
Balise                       Description
============================ ==============
**db**
dbdriver                     Driver JDBC à utiliser pour se connecter à la base de données (postgresql)
dbserver                     URI JDBC de connection à la base de données (ex : jdbc:postgresql://localhost:5433/waarp)
dbuser                       L’utilisateur à utiliser pour se connecter à la base de données
dbpasswd                     Le mot de passe de l’utilisateur
============================ ==============

.. note::
  Il est possible de faire fonctionner les moniteurs sans base de données. 
  Les fichiers `authent.xml` et `rules.xml` seront utilisés comme source de configuration.


logback-{client,server}.xml
***************************

Les fichiers `logback*.xml` permettent de paramétrer les écritures de log.
Veuillez vous référer au manuel en ligne de Logback pour configurer la façon dont les logs sont générés et
écrits dans un fichier et/ou vers `syslog`.

Il est à noter qu'il est conseillé d'avoir les éléments suivants dans le fichier de configuration de Logback.

.. code-block:: xml

  <configuration>
    <statusListener class="org.waarp.common.logging.PrintOnlyWarningLogbackStatusListener" />

    <appender name=... class=...><!-- Voir la documentation Logback -->
    </appender>

    <root level="warn">
      <appender-ref ref=... /><!-- Voir la documentation Logback -->
    </root>

    <logger name="ch.qos.logback" level="WARN"/>
    <logger name="org.apache.http" level="WARN"/>
    <logger name="io.netty" level="WARN"/>
    <logger name="io.netty.util.internal.PlatformDependent" level="DEBUG"/>
  </configuration>

authent.xml
***********

Le fichier d'authent permet de renseigner les paramètres de connections des instances WaarpR66.
Ce fichier est consommé par la commande `loadauth` ou `loadconf` (voir utilisation).
Une fois consommé ce fichier n'est plus utilisé (vous pouvez le mettre à jour pour le recharger plus tard).

Le fichier liste un moniteurs dans une balise `<entry>` détaillée ci-dessous. 
Ces balises sont regroupées au sein d'une balise <authent>. 

========== ==============
Balise     Description
========== ==============
**entry**
hostid     L'hostid du moniteur
address    Addresse ou entrée DNS du moniteur
port       Si le moniteur est un serveur, le port de destination
isssl      Le moniteur utilise SSL
admin      Le moniteur authorise les accès Admin via R66
isclient   Le moniteur n'est pas un serveur
key        Mot de passe du moniteur
========== ==============

Au minimum le fichier doit renseigner le moniteur qui l'utilise.

rules.xml
*********

Les fichiers de règles permettent de détailler les règles utilisées par le moniteur ainsi que 
leur contenu.
Ce fichier est consommé par la commande `loadauth` ou `loadrules` (voir utilisation).
Une fois consommé ce fichier n'est plus utilisé (vous pouvez le mettre à jour pour le recharger plus tard).

Le fichier décrit une règle dans une balise `<rule>` détaillée ci-dessous.
Ces balises sont regroupées au sein d'une balise <rules>. 

============ ==============
Balise       Description
============ ==============
**rule**
idrule       Nom de la règle
comment      Commentaire
hostids      Liste des moniteurs authorisés à utiliser la règle
mode         Le mode de la règle
rpretasks    Tâches executées par le receveur avant le transfert
rposttasks   Tâches executées par le receveur après le transfert
rerrortasks  Tâches executées par le receveur en cas d'erreur du transfert
spretasks    Tâches executées par l'envoyeur avant le transfert
spoststasks  Tâches executées par l'envoyeur après le transfert
serrortasks  Tâches executées par l'envoyeur en cas d'erreur du transfert
============ ==============

Les hostids de la balises hostids sont présentés comme suit:

.. code-block:: xml

  <hostids>
    <hostid>hostid1</hostid>
    <hostid>hostid2</hostid>
  </hostids>

Le mode de la règle peut etre un des suivant

 * 1: SEND, Envoie le fichier client -> serveur
 * 2: RECV, Demande le fichier serveur -> client
 * 3: SEND+MD5
 * 4: RECV+MD5
 * 5: SENDTHROUGHMODE
 * 6: RECVTHROUGHMODE
 * 7: SENDMD5THROUGHMODE
 * 8: RECVMD5THROUGHMODE


Les listes de tâches (rpretasks, rposttasks, rerrortasks, spretasks, sposttasks, serrortasks).
sont présentées comme suit:

.. code-block:: xml

  <rpretasks>
    <tasks>
      <task></task>
      <task></task>
      <task></task>
      ...
    </tasks>
  </rpretasks>

Le contenue d'une balise <task> est détaillé ci-dessous:

============ ==============
Balise       Description
============ ==============
**task**
type         Le type de tâche
path         Les options de cette tâche
delay        Temps (ms) accordé avant l'envoie d'un Time Out
============ ==============

Cryptographie
*************

cryptokey
=========

Cette clef DES est utilisée par les instances WaarpR66 pour chiffrer les mots de passe pour s’identifier sur les autres instances.
Pour générer une nouvelle cryptokey:

.. code-block:: bash

  $ cat /dev/urandom | head -c 8 > cryptokey.des

Pour régénérer le mot de passe {pwd} dans le fichier {output} avec la clé {key}:

.. code-block:: bash

  ./bin/waarp-password.sh -ki {key} -pwd {pwd} -po {output}

keystore
========

Le keystore contient la clef privée d’identification de l’instance WaarpR66 pour les communication SSL. Il s’agit d’un Java KeyStore de type keystore.

truststore
==========

Le truststore contient les certificats des instances autorisés à communiquer via SSL avec l’instance WaarpR66. Il s’agit d’un Java KeyStore de type truststore.

adminstore
==========

Le keystore contient la clef privée pour accéder à l’interface d’administration de l’instance WaarpR66 en https. Il s’agit d’un Java KeyStore de type keystore.
Pour générer une nouveau keystore:

.. code-block:: bash

  $ keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -validity 360 -keysize 2048

Pour générer un nouveau truststore depuis un keystore existant

.. code-block:: bash

  $ keytool -export -keystore keystore.jks -alias selfsigned -file cert.crt
  $ keytool -import -alias selfsigned -file cert.crt -keystore truststore.jks

restsignkey
===========

La clef REST est utilisée par Waarp Manager pour communiquer avec les serveurs Waarp afin de récupérer l’historiques des transferts.
Pour générer une nouvelle clef de signature REST

.. code-block:: bash

  $ cat /dev/urandom | head -c 64 > restsignkey.key

Attention: Dans le cadre d’une utiilisation de Waarp Manager, les clefs cryptokey et restsignkey doivent être partagé par toute les instances serveur WaarpR66 du parc et connu de Waarp Manager.


Les sections suivantes présentent:
 
 #. Un exemple de fichier des configurations
 #. Le détail complet des fichiers de configuration


La section d'après détails le lancement d'un serveur WaarpR66.
