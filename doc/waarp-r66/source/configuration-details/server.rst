``server.xml``
##############

.. index:: server.xml

.. _server-xml:

Le fichier ``server.xml`` contient les directives de configurations
de l'instance serveur.

.. note::

   Les changements dans ce fichier sont pris en compte au redémarrage du serveur.

Les directives de configuration sont réparties en 11 sections :

- :ref:`identity <server-xml-identity>`: données concernant l'identité
  de l'instance
- :ref:`server <server-xml-server>`: données spécifiques au service
- :ref:`network <server-xml-network>`: données concernant les paramètres
  réseaux
- :ref:`ssl <server-xml-ssl>`: paramétrage des certificats SSL
- :ref:`directory <server-xml-directory>`: dossiers utilisés par le
  service
- :ref:`limit <server-xml-limit>`: paramétrage de l'utilisation des
  ressources et du comportement interne du serveur
- :ref:`db <server-xml-db>`: paramétrage de la base de données
- :ref:`rest <server-xml-rest>`: paramétrage de l'interface REST
- :ref:`business <server-xml-business>`: paramétrage des composantes métiers (Mode ``Embedded``)
- :ref:`roles <server-xml-roles>`: paramétrage des rôles autorisés des partenaires
- :ref:`aliases <server-xml-aliases>`: paramétrage d'alias (nom de remplacement) pour des partenaires

Il existe également des options étendues à la JVM : :ref:`ExtraOptions <Extra-Waarp-options>`

.. _server-xml-identity:

Section ``identity``
--------------------

=========== ====== ==== ====== =============
Balise      Type   Obl. Défaut Signification
=========== ====== ==== ====== =============
hostid      string O           Nom de l'hôte en mode non-SSL
sslhostid   string N           Nom de l'hôte en mode SSL
cryptokey   string O           Fichier contenant la clef de cryptage des mots de passe stockés, elle-même encryptée en DES
authentfile string N           Fichier XML contenant l'authentification des partenaires Waarp R66
=========== ====== ==== ====== =============

.. _server-xml-server:

Section ``server``
------------------

====================== ======= ==== ========= =============
Balise                 Type    Obl. Défaut    Signification
====================== ======= ==== ========= =============
serveradmin            string  O              Nom d'utilisateur de l'administrateur utilisé pour accéder à l'interface web d'administration
serverpasswd           string  O              Mot de passe de l'administrateur encryptée avec la clef « cryptokey » utilisé pour accéder à l'interface web d'administration
usenossl               boolean N    True      Active le mode non-SSL
usessl                 boolean N    False     Active le mode SSL
usehttpcomp            boolean N    False     Si le mode SSL  est activé, active la compression SSL
uselocalexec           boolean N    False     Par défaut, Waarp R66 utilise System.exec() pour exécuter les processus externes. Cela peut poser des problèmes de performance (limitations de la JDK). L'utilisation de GoldenGate LocalExec Daemon peut permettre d'obtenir de meilleures performance par délégation d'exécution.
lexecadd               string  N    127.0.0.1 Adresse sur laquelle écoute le daemon LocalExec
lexecport              integer N    9999      Port sur lequel écoute le daemon LocalExec
httpadmin              string  O              Chemin vers le dossier où sont stockées les sources de l'interface d'administration web
admkeypath             string  O              Chemin vers le fichier JKS contenant le certificat HTTPS pour l'interface web d'administration
admkeystorepass        string  O              Mot de passe du fichier JKS contenant le certificat HTTPS pour l'interface web d'administration
admkeypass             string  O              Mot de passe certificat HTTPS pour l'interface web d'administration contenu dans le fichier JKS.
checkaddress           boolean N    False     Si « True », le serveur R66 vérifie l'adresse IP de l'hôte distant qui demande une connexion
checkclientaddress     boolean N    False     Si « True », le serveur R66 vérifie l'adresse IP des clients qui demandent une connexion
multiplemonitors       integer O    1         Nombre de serveurs qui agissent dans le même groupe comme une seule instance R66
pastlimit              integer N    86400000  Profondeur maximale affichées dans l'interface HTTP de monitoring en ms
minimaldelay           integer N    5000      Intervalle de rafraîchissement automatique de l'interface HTTP de monitoring en ms
snmpconfig             string  N              Chemin vers le fichier de configuration de l'agent SNMP (voir :ref:`référence <snmp-xml>`)
multiplemonitors       integer N    1         Nombre d'instances dans un cluster de serveurs Waarp R66
businessfactorynetwork string   N    null     Indique la classe Factory pour les comportements "métiers" à associer à Waarp (Embedded)
====================== ======= ==== ========= =============


.. _server-xml-network:

Section ``network``
-------------------

================== ======= ==== ========= =============
Balise             Type    Obl. Défaut    Signification
================== ======= ==== ========= =============
serverport         integer N    6666      Port utilisé pour le protocole R66
serversslport      integer N    6667      Port utilisé pour le protocole R66 en SSL
serverhttpport     integer N    8066      Port utilisé pour l'interface web de supervision (la valeur ``0`` désactive l'interface)
serverhttpsport    integer N    8067      Port utilisé pour l'interface web HTTPS d'administration (la valeur ``0`` désactive l'interface)
================== ======= ==== ========= =============


.. _server-xml-ssl:

Section ``ssl``
---------------

Cette section est optionnelle et peut être omise si le mode SSL est
désactivé (``server/usessl`` est ``false``)

========================== ======= ==== ========= =============
Balise                     Type    Obl. Défaut    Signification
========================== ======= ==== ========= =============
keypath                    String  O              Chemin vers le fichier JKS qui contient  la clef privée du serveur
keystorepass               String  O              Mot de passe du fichier JKS qui contient  la clef privée du serveur
keypass                    String  O              Mot de passe de la clef privée du serveur
trustkeypath               String  O              Chemin vers le fichier JKS qui contient  la clef publics des hôtes autorisés à se connecter à ce serveur
trustkeystorepass          String  O              Mot de passe du fichier JKS qui contient  la clef publics des hôtes autorisés à se connecter à ce serveur
trustuseclientauthenticate boolean N    False     Force la connexion des clients en SSL
========================== ======= ==== ========= =============


.. _server-xml-directory:

Section ``directory``
---------------------

.. note::

   Les dossiers par défaut indiqués sont relatifs au dossier
   ``serverhome``.

========================== ======= ==== ========= =============
Balise                     Type    Obl. Défaut    Signification
========================== ======= ==== ========= =============
serverhome                 String  O              Chemin vers le répertoire de base du serveur Waarp R66
in                         String  N    IN        Chemin du dossier par défaut dans lequel sont déposés les fichiers reçus par défaut (chemin relatif à « serverhome »)
out                        String  N    OUT       Chemin du dossier par défaut dans lequel sont pris les fichiers envoyés (chemin relatif à « serverhome »)
arch                       String  N    ARCH      Chemin du dossier utilisé pour les archives (chemin relatif à « serverhome »)
work                       String  N    WORK      Chemin du dossier utilisé par défaut pour stocker les fichiers en cours de réception (chemin relatif à « serverhome »)
conf                       String  N    CONF      Chemin vers le dossier contenant la configuration du serveur
========================== ======= ==== ========= =============


.. _server-xml-limit:

Section ``limit``
-----------------

================= ======= ==== ========== =============
Balise            Type    Obl. Défaut     Signification
================= ======= ==== ========== =============
serverthread      Integer N    8          Nombre de threads utilisés par les serveur Waarp R66 (valeur recommandée: nombre de cœurs du processeur)
clientthread      Integer N    80         Nombre de threads utilisés par le client Waarp R66 (valeur recommandée: serverthread*10)
memorylimit       Integer N    4000000000 Quantité maximale de mémoire utilisée par le processus Java du serveur Waarp R66 (en octets)
sessionlimit      Integer N    8388608    Bande passante maximale utilisée pour une session (en octets)
globallimit       Integer N    67108864   Bande passante globale maximale utilisée (en octets)
delaylimit        Integer N    10000      Délais entre deux vérifications de bande passante. Plus cette valeur est faible, plus le contrôle de la bande passante sera précis. Attention toutefois à ne pas donner de valeur trop faible (en ms)
runlimit          Integer N    10000      Nombre maximal de transferts actifs simultanés
delaycommand      Integer N    5000       Délais entre deux exécutions du Commander (en ms)
delayretry        Integer N    30000      Délais entre deux tentatives de transfert en cas d'erreur (en ms)
timeoutcon        Integer N    30000      Délais de timeout d'une connexion (en ms)
blocksize         Integer N    65536      Taille de bloc utilisée par le serveur Waarp R66. Une valeur entre 8KB et 16MB est recommandée (en octets)
gaprestart        Integer N    30         Nombre de blocs écartés lors de la reprise d'un transfert.
usenio            boolean N    False      Activation du support de NIO pour les fichiers. Selon le JDK, cela peut améliorer les performances.
usecpulimit       boolean N    False      Utilisation de la limitation de l'utilisation du CPU au démarrage d'une requête
usejdkcpulimit    boolean N    False      Utilisation du support natif du JDK pour contrôler l'utilisation du CPU.  Si « False », la librairie Java Sysmon est utilisée
cpulimit          Decimal N    0.0        Pourcentage maximal d'utilisation du CPU au-delà duquel une demande de transfert est refusée. Les valeurs 0 et 1 désactivent la limite.
connlimit         Integer N    0          Nombre maximal de connexions. La valeur 0 désactive la limite.
lowcpulimit       decimal N    0.0        Seuil minimal de consommation de CPU (en pourcentage)
highcpulimit      decimal N    0.0        Seuil maximal de consommation de CPU (en pourcentage). La valeur 0 désactive le contrôle.
percentdecrease   decimal N    0.01       Valeur de diminution de la bande passante quand le seuil maximal de consommation CPU est atteint (en pourcentage)
delaythrottle     integer N    1000       Intervalle de contrôle de la consommation de ressources (en ms)
limitlowbandwidth integer N    1000000    Seuil minimal de consommation de bande passante (en octets)
digest            Integer N    2          Algorithme de hashage utilisé par défaut. CRC32=0, ADLER32=1, MD5=2, MD2=3, SHA1=4, SHA256=5, SHA384=6, SHA512=7 (SHA256=5 est recommandé)
usefastmd5        boolean N    True       Utilisation de la librairie FastMD5
usethrift         integer N    0          Active le serveur RPC Apache Thrift (0 désactive le serveur RPC, une valeur supérieure à 0 indique le port sur lequel écouter)
checkversion      boolean N    True       Vérifie la version de ses partenaires pour s'assurer de la compatibilité du protocole
globaldigest      boolean N    True       Active ou non le contrôle d'intégrité de bout en bout
================= ======= ==== ========== =============


.. _server-xml-db:

Section ``db``
--------------

.. note::

   Si ``taskrunnernodb`` est à ``True``, les autres balises *peuvent*
   être omises.

   Si ``taskrunnernodb`` est à ``False``, où si la balise est absente,
   toutes les autres balises **doivent** être renseignées.



================= ======= ==== ========== =============
Balise            Type    Obl. Défaut     Signification
================= ======= ==== ========== =============
taskrunnernodb    boolean N    False      Indique si le serveur utilise une base de données ou non
dbdriver          String  N               Type de base de données utilisé. Sont supportés : oracle, mysql, postgresql, h2
dbserver          String  N               Chaîne de connexion JDBC à la base de données. Consulter le manuel du pilote JDBC utilisé pour la syntaxe exacte.
dbuser            String  N               Utilisateur de la base de données
dbpasswd          String  N               Mot de passe de l'utilisateur de la base de données.
dbcheck           boolean N    True       Vérifie que le modèle de données est à jour au démarrage, et effectue la mise à jour le cas échéant
================= ======= ==== ========== =============


.. _server-xml-rest:

Section ``rest``
----------------


================= ======= ==== ========== =============
Balise            Type    Obl. Défaut     Signification
================= ======= ==== ========== =============
restaddress       string  N               Adresse IP sur laquelle le serveur écoute pour servir l'API REST
serverrestport    integer N    8068       Port sur lequel le serveur écoute pour servir l'API REST
restssl           boolean N    False      Active le mode HTTPS pour l'interface REST
restauthenticated boolean N    False      Active l'authentification des requêtes vers l'API REST
resttimelimit     integer N    -1         Active la limitation de validité dans le temps des requêtes (en ms). ``-1`` désactive cette limitation.
restsignature     boolean N    True       Active la signature des requêtes REST
restsigkey        string  N               Chemin vers le fichier contenant la clef de signature des requêtes REST (cf. :ref:`certifs-rest`)
restmethod                O               Voir ci-dessous.
================= ======= ==== ========== =============

Les balises ``restmethod`` peuvent être renseignées plusieurs fois.
Elles permettent d'activer chaque fonctionnalités de l'API REST
individuellement.

Chaque ocurrence de ``restmethod`` doit contenir deux balises :

- ``restname``: le nom de la fonctionnalité à paramétrer (plusieurs
  fonctionnalités peuvent être renseignées, séparées par des espaces)
- ``restcrud``: les actions actives pour la (les) fonctionnalités en question.

Par exemple :

.. code-block:: xml

   <restmethod>
        <restname>ALL</restname>
        <restcrud>R</restcrud>
   </restmethod>
   <restmethod>
      <restname>DbHostAuth DbRule</restname>
      <restcrud>CRU</restcrud>
   </restmethod>
   <restmethod>
      <restname>Bandwidth</restname>
      <restcrud>RU</restcrud>
   </restmethod>


Les fonctionnalités sont les suivantes :

=================== ============
Fonctionnalité      Description
=================== ============
All                 Alias regroupant toutes les fonctionnalités ci-dessous
DbTaskRunner        Actions sur les transferts
DbHostAuth          Actions sur la liste des partenaires
DbRule              Actions sur les règles de transfert
DbHostConfiguration Actions sur la configuration des hôtes
DbConfiguration     Actions sur les limitations de bandes passantes
Bandwidth           Actions sur les limitations de bandes passantes
Business            Actions sur l'intégration métier
Config              Import/export de la configuration
Information         Récupère des informations sur les transferts
Log                 Actions sur les logs
Server              Actions sur le serveur
Control             Actions sur les transferts
=================== ============


Pour chaque fonctionnalités, les actions à activer sont indiquées par
une combinaison des lettres ``C``, ``R``, ``U`` et ``D`` (``C`` pour
*création*, ``R`` pour *lecture*, ``U`` pour *mise-à-jour* et ``D`` pour
*suppression*) ou seules les actions voulues doivent être indiquées.


.. _server-xml-business:

Section ``business``
--------------------


================= ======= ==== ========== =============
Balise            Type    Obl. Défaut     Signification
================= ======= ==== ========== =============
businessid        string  N               Id d'un partenaire autorisé à déclencher des opérations Business
================= ======= ==== ========== =============

.. _server-xml-roles:

Section ``roles``
--------------------

Il s'agit d'une liste de ``role``, contenant chacun:

================= ======= ==== ========== =============
Balise            Type    Obl. Défaut     Signification
================= ======= ==== ========== =============
roleid            string  O               Id d'un partenaire
roleset           string  O               liste de rôles autorisés, séparés par un "blanc" ou un "|", parmi:  NOACCESS,READONLY,TRANSFER,RULE,HOST,LIMIT,SYSTEM,LOGCONTROL,PARTNER(READONLY,TRANSFER),CONFIGADMIN(PARTNER,RULE,HOST),FULLADMIN(CONFIGADMIN,LIMIT,SYSTEM,LOGCONTROL)
================= ======= ==== ========== =============

.. _server-xml-alias:

Section ``aliases``
--------------------

Il s'agit d'une liste de ``alias``, contenant chacun:

================= ======= ==== ========== =============
Balise            Type    Obl. Défaut     Signification
================= ======= ==== ========== =============
realid            string  O               Id d'un partenaire
aliasid           string  O               liste de noms alias équiavelents, séparés par un "blanc" ou un "|"
================= ======= ==== ========== =============


.. _Extra-Waarp-options:

Section ``ExtraOptions``
------------------------

Extra XML Option:

- Mise à jour automatique de la base en fonction de la version de Waarp

Par défaut, le champ ``<root><version>version</version></root>`` du fichier de configuration XML
est géré par Waarp pour vérifier la configuration de la base de données et sa version par rapport à celle
du programme, afin de permettre une mise à jour automatique.

Cette mise à jour automatique peut être empêchée par l'option ``<db>dbcheck>False</dbcheck>...</db>`` ou
grâce à la propriété Java ``-Dopenr66.startup.dbcheck=0``.

- Partage d'une même base entre plusieurs moniteurs Waarp

Dans le cas où une base est partagée entre plusieurs moniteurs R66, afin d'être capable de voir tous les
transferts dans la console web d'administration, vous pouvez indiquer une option spéciale dans "Autres
informations" avec l'identifiant qui sera utilisé pour se connecter à cette interface Web.
    ``<root>...<seeallid>id1,id2,...,idn</seeallid></root>``

- Options additionnelles pour Waarp via la JVM :

  - ``-Dopenr66.locale=en|fr`` pour choisir entre l'anglais ou le français pour le langage de l'interface (
  (défaut = en)
  - ``-Dopenr66.ishostproxyfied=1|0``  pour indiquer que ce serveur est derrière un proxy (comme R66Proxy
  ou un matériel équivalent) afin d'empêcher le contrôle des adresses IP de s'appliquer (puisque celle-ci
  sera celle du Proxy) (défaut = 0)
  - ``-Dopenr66.startup.warning=1|0`` pour décider si les Warning du démarrage seront affichées ou non
  (défaut = 1)
  - ``-Dopenr66.startup.checkdb=1|0`` pour choisir s'il y a un test entre le niveau de la configuration de la
  base de données
  et la version du serveur lors du démarrage (défaut = 0)
   - ``-Dopenr66.startup.autoUpgrade=0|1`` (défaut 0) Si les versions de la base et du moniteur diffèrent,
   autorise la mise à jour automatique de la base de données.
  - ``-Dopenr66.chroot.checked=1|0`` pour choisir si toutes les règles doivent respecter le contrat "choroot"
  Par exemple, tenter de récupérer un fichier (RECV) depuis un partenaire distant en spécifiant un chemin
  complet peut être autorisé, même si il est en dehors du répertoire "OUT", sauf si checked=1. Si
  checked=1, alors tous les fichiers reçus doivent spécifier un chemin incluant "OUT", sans remonter
  au-delà.  (défaut = 1)
  - ``-Dopenr66.blacklist.badauthent=1|0`` pour choisir si un serveur qui a une mauvaise identification
  doit être immédiatement "black listé" (il n'est plus autorisé pour un temps) pour prévenir des attaques
  DDOS. Si ``-Dopenr66.ishostproxyfied=1``, alors il est obligatoirement faux. En effet, dans ce cas, si un
   des partenaires a un problème d'authentification, alors tous les partenaires proxifiés via le même
   proxy seront bannis puisque visibles depuis la même adresse IP. (défaut = 1)
  - ``-Dopenr66.filename.maxlength=n`` pour choisir la longueur maximum autorisée pour le nom du fichier
   (défaut = 255) quand on reçoit un fichier (nom temporaire et nom final). Ceci n'empêche pas de changer
   le nom du fichier après et #ORIGINALFILENAME# contient toujours le nom complet d'origine du fichier, non
    tronqué.
  - ``-Dopenr66.trace.stats=n`` pour mettre en debug certaines traces spécifiques sur des données toutes
  les n secondes. 0 signifie absence de trace. (défaut n=0)
  - ``-Dopenr66.cache.limit=n`` et ``-Dopenr66.cache.timelimit=m`` pour mettre en cache les informations de
   transfert avec
      - n est le nombre maximum de tâches à conserver dans un cache LRU (Last Recent Used). La valeur
      minimale est 100
      - m est le temps maximum en millisecondes avant qu'un élément créé, utilisé ou modifié soit évincé du
       cache. La valeur minimale est 1000 ms (1s). Une valeur trop grande peut provoquer des consommations
       mémoires trop importante.
    (défaut n=20000, m=180000 - 180s -)
    **A noter: Cette option n'est plus valide à compter de la version 3.2.0 qui ne tient plus un cache LRU
    actif**
   - ``-Dopenr66.usespaceseparator=0|1`` (défaut 0) Autorise Waarp à utiliser l'espace comme séparateur
   mais induit des risques de bugs.
   - ``-Dopenr66.executebeforetransferred=0|1`` (défaut 1) Autorise Waarp à exécuter les Error-Tasks si une
    erreur intervient pendant les "pré-task", avant le transfert effectif


.. _server-xml-example:

Exemple complet
---------------

.. code-block:: xml

   <?xml version="1.0" encoding="UTF-8"?>
   <config xmlns:x0="http://www.w3.org/2001/XMLSchema">
       <comment>Configuration file for a server with a Postgresql database</comment>
       <identity>
           <hostid>monserveur</hostid>
           <sslhostid>monserveur-ssl</sslhostid>
           <cryptokey>/etc/waarp/cryptokey.des</cryptokey>
       </identity>
       <server>
           <serveradmin>admin</serveradmin>
           <serverpasswd>5a4b7c6a66065cbb622acefec8c3a302</serverpasswd>
           <usenossl>True</usenossl>
           <usessl>True</usessl>
           <usehttpcomp>False</usehttpcomp>
           <uselocalexec>False</uselocalexec>
           <httpadmin>/etc/waarp/admin</httpadmin>
           <admkeypath>/etc/waarp/adminkey.jks</admkeypath>
           <admkeystorepass>password</admkeystorepass>
           <admkeypass>password</admkeypass>
           <checkaddress>False</checkaddress>
           <checkclientaddress>False</checkclientaddress>
           <pastlimit>86400000</pastlimit>
           <minimaldelay>5000</minimaldelay>
           <multiplemonitors>1</multiplemonitors>
           <snmpconfig>/etc/waarp/snmpconfig.xml</snmpconfig>
       </server>
       <network>
           <serverport>6666</serverport>
           <serversslport>6667</serversslport>
           <serverhttpport>8066</serverhttpport>
           <serverhttpsport>8067</serverhttpsport>
       </network>
       <ssl>
           <keypath>/etc/waarp/key.jks</keypath>
           <keystorepass>password</keystorepass>
           <keypass>password</keypass>
           <trustkeypath>/etc/waarp/trustkey.jks</trustkeypath>
           <trustkeystorepass>password</trustkeystorepass>
           <trustuseclientauthenticate>True</trustuseclientauthenticate>
       </ssl>
       <directory>
           <serverhome>/var/lib/waarp</serverhome>
           <in>in</in>
           <out>out</out>
           <arch>arch</arch>
           <work>work</work>
           <conf>conf</conf>
       </directory>
       <rest>
           <restaddress>0.0.0.0</restaddress>
           <restport>8088</restport>
           <restssl>true</restssl>
           <restauthenticated>true</restauthenticated>
           <resttimelimit>3000</resttimelimit>
           <restsignature>true</restsignature>
           <restsigkey>/etc/waarp/restsigning.key</restsigkey>
           <restmethod>
               <restname>ALL</restname>
               <restcrud>CRUD</restcrud>
           </restmethod>
           <restmethod>
              <restname>Bandwidth</restname>
              <restcrud>CRUD</restcrud>
           </restmethod>
           <restmethod>
              <restname>Information</restname>
              <restcrud>CRUD</restcrud>
           </restmethod>
           <restmethod>
              <restname>Server</restname>
              <restcrud>CRUD</restcrud>
           </restmethod>
           <restmethod>
              <restname>Control</restname>
              <restcrud>CRUD</restcrud>
           </restmethod>
       </rest>
       <limit>
           <serverthread>8</serverthread>
           <clientthread>80</clientthread>
           <usefastmd5>False</usefastmd5>
           <timeoutcon>10000</timeoutcon>
           <delayretry>10000</delayretry>
       </limit>
       <db>
           <dbdriver>postgresql</dbdriver>
           <dbserver>jdbc:postgresql://localhost:5432/waarp_r66</dbserver>
           <dbuser>username</dbuser>
           <dbpasswd>password</dbpasswd>
           <dbcheck>false</dbcheck>
       </db>
   </config>
