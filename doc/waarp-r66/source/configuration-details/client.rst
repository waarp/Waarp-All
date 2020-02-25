##############
``client.xml``
##############


.. index:: client.xml

Le fichier ``client.xml`` contient les directives de configurations
de l'instance cliente.

Les directives de configuration sont réparties en 7 sections :

- :ref:`identity <client-xml-identity>`: données concernant l'identité
  de l'instance
- :ref:`ssl <client-xml-ssl>`: paramétrage des certificats SSL
- :ref:`directory <client-xml-directory>`: dossiers utilisés par le
  service
- :ref:`limit <client-xml-limit>`: paramétrage de l'utilisation des
  ressources et du comportement interne du serveur
- :ref:`db <client-xml-db>`: paramétrage de la base de données

.. _client-xml-identity:

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


.. _client-xml-client:

Section ``client``
------------------

====================== ======== ==== ====== =============
Balise                 Type     Obl. Défaut Signification
====================== ======== ==== ====== =============
taskrunnernodb         boolean  N    False  Indique que le client n'utilise pas de base de données
businessfactorynetwork string   N    null   Indique la classe Factory pour les comportements "métiers" à associer à Waarp (Embedded)
====================== ======== ==== ====== =============


.. _client-xml-ssl:

Section ``ssl``
---------------

Cette section est optionelle et peut être omise si le mode SSL n'est
pas utilisé.

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


.. _client-xml-directory:

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


.. _client-xml-limit:

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


.. _client-xml-db:

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
autoUpgrade       boolean N    True       Vérifie que le modèle de données est à jour au démarrage, et effectue la mise à jour le cas échéant
dbcheck           boolean N    True       *(déprécié)* Utiliser ``autoUpgrade`` à la place
================= ======= ==== ========== =============



.. _client-xml-example:

Exemple complet
---------------

.. code-block:: xml

   <?xml version="1.0" encoding="UTF-8"?>
   <config xmlns:x0="http://www.w3.org/2001/XMLSchema">
     <comment>Client configuration template</comment>
     <identity>
        <hostid>monserveur</hostid>
        <sslhostid>monserveur-ssl</sslhostid>
        <cryptokey>/etc/waarp/cryptokey.des</cryptokey>
     </identity>
     <client/>
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
           <autoUpgrade>false</autoUpgrade>
     </db>
   </config>
