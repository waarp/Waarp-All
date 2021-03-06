Configuration Avancée
#####################

client.xml
**********

============================ ==================== ==============
Balise                       Type (Default)       Description
============================ ==================== ==============
comment                      String

**identity**
hostid                       String               Identifiant pour connection non SSL
sslhostid                    String               Identifiant pour connection SSL
cryptokey                    Des-File             Fichier de la clef DES pour le chiffrement des mots de passe
authentfile                  XML-File             Fichier d'authentification aux partenaires

**ssl** (Optional)
keypath                      JKS-File             JKS KeyStore pour les accès R66 via SSL (Contient le certificat serveur)
keystorepass                 String               Mot de passe du JSK <keypath>
keypass                      String               Mot de passe du Certificat du jks <keypath>
trustkeypath                 JKS-File             JKS TrustStore pour les accès R66 via SSL en utilisant l'authentification client (contient les certificats client)
trustkeystorepass            String               Mot de passe du JSK <trustkeypath>
trustuseclientauthenticate   Boolean              Si vrai, R66 n'acceptera que les clients authorisés via SSL

**directory**
serverhome                   Directory            Dossier racine du moniteur R66 (Les chemins sont calculés depuis ce dossier)
in                           String (IN)          Dossier par défaut de reception
out                          String (OUT)         Dossier par défaut d'envoie
arch                         String (ARCH)        Dossier par défaut d'archive
work                         String (WORK)        Dossier par défaut de travail
conf                         String (CONF)        Dossier par défaut de configuration

**db**
dbdriver                     String               Driver JDBC à utiliser pour se connecter à la base de données (mysql, postgresql, h2)
dbserver                     String               URI JDBC de connection à la base de données (jdbc:type://[host:port]....). Veuillez vous référer à la documentation de votre base de donnée pour la syntaxe correcte
dbuser                       String               Utilisateur à utiliser pour se connecter à la base de données
dbpasswd                     String               Mot de passe de l’utilisateur
============================ ==================== ==============

server.xml
**********

============================ ==================== ==============
Balise                       Type (Default)       Description
============================ ==================== ==============
comment                      String 

**identity**
hostid                       String               Identifiant pour connection non SSL
sslhostid                    String               Identifiant pour connection SSL
cryptokey                    Des-File             Fichier de la clef DES pour le chiffrement des mots de passe
authentfile                  XML-File             Fichier d'authentification aux partenaires

**server**
serveradmin                  String               Login pour l'accès administrateur
serverpasswd                 String               Mot de passe pour l'accès administrateur chiffré par <cryptokey>
serverpasswdfile             String               Fichier PGP pour l'accès administrateur chiffré par <cryptokey> (Choisir <serverpasswd> ou <serverpasswdfile>)
usenossl                     Boolean (True)       Autorise les connections non SSL
usessl                       Boolean (False)      Autorise les connections SSL (voir configuration SSL plus bas)
usehttpcomp                  Boolean (False)      Authorise la compression HTTP pour l'interface d'administration (HTTPS)
uselocalexec                 Boolean (False)      Si vrai utilise R66 LocalExec Daemon au lieu de System.exec()
lexecaddr                    Address (127.0.0.1)  Addresse du Daemon LocalExec
lexecport                    Integer (9999)       Port du Daemon LocalExec
httpadmin                    Directory            Dossier racine de l'interface d'administration (HTTPS)
admkeypath                   JKS-File             JKS KeyStore pour les accès administrateur en HTTPS (Contient les certificats serveur)
admkeystorepass              String               Mot de passe du <admkeypath> KeyStore
admkeypass                   String               Mot de passe du certificat serveur dans <admkeypath>
checkaddress                 Boolean (False)      R66 vérifiera l'addresse IP distance pendant l'acceptation de la connection
checkclientaddress           Boolean (False)      R66 vérifiera l'addresse IP distance du client distant
multiplemonitors             Integer (1)          Défini le nombre de serveur du même groupe considéré comme une unique instance R66
businessfactorynetwork       String               Nom de classe complet de la Business Factory. (org.waarp.openr66.context.R66DefaultBusinessFactory)

**network**
serverport                   Integer (6666)       Port utilisé pour les connections en clair
serversslport                Integer (6667)       Port utilisé pour les connections chiffrées
serverhttpport               Integer (8066)       Port de l'interface HTTP de monitoring, 0 désactive cette interface
serverhttpsport              Integer (8067)       Port de l'interface HTTPS d'administration, 0 désactive cette interface
serverrestport               Integer (-1)         Port de l'API REST HTTP(S), -1 désactive cette interface

**ssl** (Optional)
keypath                      JKS-File             JKS KeyStore pour les accès R66 via SSL (Contient le certificat serveur)
keystorepass                 Strin                Mot de passe du JSK <keypath>
keypass                      String               Mot de passe du Certificat du jks <keypath>
trustkeypath                 JKS-File             JKS TrustStore pour les accès R66 via SSL en utilisant l'authentification client (contient les certificats client)
trustkeystorepass            String               Mot de passe du JSK <trustkeypath>
trustuseclientauthenticate   Boolean              Si vrai, R66 n'acceptera que les clients authorisés via SSL

**directory**
serverhome                   Directory            Dossier racine du moniteur R66 (Les chemins sont calculés depuis ce dossier)
in                           String (IN)          Dossier par défaut de reception
out                          String (OUT)         Dossier par défaut d'envoie
arch                         String (ARCH)        Dossier par défaut d'archive
work                         String (WORK)        Dossier par défaut de travail
conf                         String (CONF)        Dossier par défaut de configuration

**limit**
serverthread                 Integer (n*2 + 1)    Nombre de threads serveur (n=Nombre de coeur)
clientthread                 Integer (10)         Nombre de threads client (=10x<serverthread>)
memorylimit                  Integer (1000000000) Limite mémoire des services HTTP et REST
sessionlimit                 Integer (1GB)        Limitation de bande passante par session (1GB)
globallimit                  Integer (100GB)      Limitation de bande passante globale (100GB)
delaylimit                   Integer (10000)      Interval entre 2 vérification de bande passante
runlimit                     Integer (1000)       Limite du nombre de transfers actifs (1000)
delaycommand                 Integer (5000)       Interval entre 2 execution du Commander (5s)
delayretry                   Integer (30000)      Interval avant une nouvelle tentative de transfert en cas d'erreur (30s)
timeoutcon                   Integer (30000)      Interval avant l'envoie d'un Time Out (30s)
blocksize                    Integer (65536)      Taille des blocs (64Ko). Une valeur entre 8 ko et 16 Mo est recommandé
gaprestart                   Integer (30)         Nombre de blocs doublonnés en cas d'arrêt puis reprise d'un transfert
usenio                       Boolean (False)      Support NIO des fichiers. Paramètre obsolète
usecpulimit                  Boolean (False)      Limitation du CPU via la gestion de la bande passante
usejdkcpulimit               Boolean (False)      Limitation CPU basé sur le JDSK natif, sinon Java Sysmon library est utilisé
cpulimit                     Decimal (0.0)        % de CPU, 1.0 ne produit aucune limite
connlimit                    Integer (0)          Limitation du nombre de connection
digest                       Integer (2)          Utilisation d'un Digest autre que MD5 (7 pour SHA-512 recommandé)
usefastmd5                   Boolean (False)      Utilisation de la bibliothèque FastMD5 (paramètre obsolète)
fastmd5                      SODLL                Path vers la JNI. Si vide, la version core de Java sera utilisée
checkversion                 Boolean (True)       Utilisation du protocole etendu (>= 2.3), accès à plus de retour d'information en fin de transfert
globaldigest                 Boolean (True)       Utilisation d'un digest global (MD5, SHA1, ...) par transfert de fichier
localdigest                  Boolean (True)       Utilisation d'un digest local (MD5, SHA1, ...) en fin de transfert (optionnel)

**db**
dbdriver                     address              Driver JDBC à utiliser pour se connecter à la base de données (mysql, postgresql, h2)
dbserver                     String               URI JDBC de connection à la base de données (jdbc:type://[host:port]....). Veuillez vous référer à la documentation de votre base de donnée pour la syntaxe correcte
dbuser                       String               Utilisateur de la base de données
dbpasswd                     String               Mot de passe de la base de données
dbcheck                      Boolean (True)       Vérification de la base de données au démarage
taskrunnernodb               Boolean (False)      WaarpR66 serveur sans base, utilise les fichiers comme information permanente sur les tâches de transfert

**rest**
restssl                      Boolean (False)      Utilisation de SSL par l'interface REST
restdelete                   Boolean (False)      Authorisation de DELETE par l'interface REST
restauthenticated            Boolean (False)      Utilisation de l'authentification par l'interface REST
resttimelimit                Long (-1)            Time out de l'interface REST
restauthkey                  Path                 Clef d'authentification SHA 256 de l'interface REST

**business**
businessid                   String               L'hostid (1 by 1) authorisé à utiliser des Business Request

**roles**
role                         Array                Remplace le rôle de l'ĥôte en base de données
roleid                       String               L'hostid (1 à 1) concerné par le remplacement
roleset                      StringArray          Les nouveaux rôle attribués

**aliases**
alias                        Array                Permets d'utiliser des alias au lieu des hostid
realid                       String               Hostid aliassé (l'alias est local)
aliasid                      StringArray          L'ensemble des alias de l'hostid
============================ ==================== ==============

Les balises <roles> et <aliases> contiennent des listes d'option. Exemple:

.. code-block:: xml

  ...
  <roles>
    <role>
      <roleid>DummyHost1</roleid>
      <roleset>RoleA</roleset>
    </role>
    <role>
      <roleid>DummyHost2</roleid>
      <roleset>RoleA RoleC</roleset>
    </role>
    <role>
      <roleid>DummyHost3</roleid>
      <roleset>RoleC RoleD RoleE</roleset>
    </role>
  </roles>
  <aliases>
    <alias>
      <realid>DummyHost1</realid>
      <aliasid>AliasC</aliasid>
    </alias>
    <alias>
      <realid>DummyHost4</realid>
      <aliasid>AliasA AliasB</aliasid>
    </alias>
  </aliases>
  ...

Optimisation
************

Il peut être nécessaire de paramétrer finement dans certains cas.

**Limitation de la mémoire**

Il est possible de limiter l'usage de la mémoire en usant des paramètres suivants :

*Limitation des services*

 * Services R66 : un des protocoles au moins doit être activé (TLS ou no TLS) ; si l'un des deux n'est pas
   utile, vous pouvez le désactiver (`usenossl` ou `usessl` à `False`)
 * `uselocalexec` : à `False` si aucun usage (exécution dans un processus externes des commandes EXECxxx)
   (valeur par défaut)
 * `serverhttpport` : si le monitoring HTTP est sans usage, vous pouvez le désactiver (`0`)
 * `serverhttpsport` : si le moteur d'administration HTTPS est sans usage, vous pouvez le désactiver (`0`)
   (non recomandé)
 * `serverrestport` : si le moteur REST est sans usage, vous pouvez le désactiver (`-1`, valeur par défaut)
 * `usethrift` : si le moteur THRIFT est sans usage,  vous pouvez le désactiver (`0`, valeur par défaut)

*Limitation des ressources*

 * `serverthread`: Possibilité de limiter le nombre de Threads dédiées à la partie serveur (y compris 1)
 * `clientthread`: Possibilité de limiter le nombre de Threads dédiées à la partie protocolaire (il est avisé
   de ne par mettre moins de 10)
 * `memorylimit`: Possibilité de limiter la taille mémoire maximale allouable pour décoder/encoder les pages
   HTTP et les réponses REST (minimum conseillé 100 Mo)
 * `runlimit`: Possibilité de limiter le nombre de transferts simultanés (il est avisé de ne pas mettre moins
   de 2)
 * de limiter l'impact processeur via une gestion adaptative de la bande passante globale :
   * `usecpulimit` à `True` : ceci active la fonctionnalité
   * `usejdkcpulimit` de préférence, laisser à `False` ou *ignoré* (permet de choisir l'implémentation
     sous-jacente analysant les ressources CPU)
   * `cpulimit` avec une valeur maximale autorisée pour la charge globale CPU, tous coeurs confondus (minimum
     conseillé `0.2`, en pratique `0.5` comme minimum) ; cette valeur détermine le seuil à partir duquel la
     bande passante globale sera progressivement diminuée afin de réduire l'activité CPU, puis remontée
   * `connlimit` en laissant à `0` ou *ignoré* (permet de limiter le nombre maximum de connexion mais
      souvent trop restrictif)

**Performances**

 * Usage de règles dans un mode sans empreinte par paquet de données (`SENDMODE`=1, `RECVMODE`=2) au lieu des
   modes avec empreinte par paquet de données (`SENDMD5MODE`=3, `RECVMD5MODE`=4) (environ 15% de gains)
 * `blocksize` : Possibilité d'augmenter la taille par défaut de 64KB à par exemple 256KB (en pratique,
   inutile d'aller au-delà), permettant de diminuer le nombre de paquets de données ainsi émis (uniquement
   valable sur de gros transferts)
 * `gaprestart` : Possibilité de diminuer la valeur par défaut (`30`) à `10`, permettant ainsi de
   restreindre la réémission des paquets à la reprise du transfert (au lieu de `30 x blocksize`, ce sera par
   exemple `10 x blocksize`)
 * `digest`: Possibilité de choisir des algorithmes plus performants (`CRC32`=0, `MD5`=2) ou avec moins de
   risques de collisions (`SHA-XXX` tel que `SHA-512`=7) (`SHA-512` est conseillé car très efficace)
   * `CRC32` est le plus performant (95% avec 6ms JDK11, 10ms JDK8) mais avec le plus de collisions,
   * `MD5` performant (55% avec 88ms JDK11, 105ms JDK8) mais avec encore des collisions
   * `SHA-512` est le plus performant des SHA (au moins 25% avec 70ms JDK11, 153ms JDK8) et aux collisions
     infimes
   * *chiffres comparés à `SHA-256` (159ms JDK11, 192ms JDK8)*
 * `globaldigest` : Possibilité de le désactiver mais recommandé à `True` (environ 25% de gains)
 * `localdigest` : Possibilité de le désactiver (`False`) (environ 20% de gains)

La performance d'autres éléments peuvent jouer :

 * La vitesse du processeur et de la mémoire
   * Il est conseillé de disposer d'au moins 2 coeurs et au moins 2 Go de mémoire disponible totalement
     pour Waarp, une valeur optimale étant 4 coeurs et 8 Go de mémoire
 * La vitesse du stockage sur lequel sont écrits les fichiers (limite naturelle du transfert)
   * Il est conseillé de disposer de disques très rapides (SSD ou FC). La vitesse en lecture (émission) ou
     en écriture (réception) peuvent en être impactées. Ceci concerne a minima le répertoire `WORK` et `IN`
     et dans une moindre mesure (lecture) `OUT`.
 * La vitesse et la latence du réseau sur lequel transite les données (limite naturelle du transfert)

*Mini-Benchmark*

Sur un Core I7 génération 5, 16 Go de mémoire, un disque rapide SSD de portable, un réseau local (`lo`),
en condition complète de vérification de cohérences (`digest` à `SHA-512` (7),
`globaldigest` et `localdigest` à `True`, et règle avec empreinte par paquet),
les transferts ont pu atteindre 65 MB/s (520 Mbits/s).

En réduisant les vérifications de cohérence (`digest` `globaldigest` maintenus mais `localdigest` à `False`
et règle sans empreinte par paquet), les performances sont montées à 80 MB/s (640 Mbits/s).

En supprimant toutes les vérifications de cohérence sauf celles des empreintes par paquet, le
débit atteint était de 110 MB/s (880 Mbits/s) (*ceci correspond au maximum du débit disque en écriture*).

Il est fortement déconseillé de désactiver totalement toutes les vérifications de cohérence, car il ne
pourra alors pas être assuré que le fichier transmis le sera sans défaut lors du transport (même si le
protocole s'appuie sur TCP/IP, il est possible d'avoir une corruption sur le réseau).

