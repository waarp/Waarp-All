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
uselocalexec*                Boolean (False)      Si vrai utilise R66 LocalExec Daemon au lieu de System.exec()
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
memorylimit                  Integer (4000000000) Limite mémoire du processus Java R66 Server
sessionlimit                 Integer (8388608)    Limitation de bande passante par session (64Mb)
globallimit                  Integer (67108864)   Limitation de bande passante globale (512Mb)
delaylimit                   Integer (10000)      Interval entre 2 vérification de bande passante
runlimit                     Integer (10000)      Limite du nombre de transfers actifs (10000)
delaycommand                 Integer (5000)       Interval entre 2 execution du Commander (5s)
delayretry                   Integer (30000)      Interval avant une nouvelle tentative de transfert en cas d'erreur (30s)
timeoutcon                   Integer (30000)      Interval avant l'envoie d'un Time Out (30s)
blocksize                    Integer (65536)      Taille des blocs (64Ko). Une valeur entre 8 ko et 16 Mo est recommandé
gaprestart                   Integer (30)         Nombre de blocs doublonnés en cas d'arrêt puis reprise d'un transfert
usenio                       Boolean (False)      Support NIO des fichiers. Améliore les performance
usecpulimit                  Boolean (False)      Limitation du CPU au démarage de nouvelles requêtes
usejdkcpulimit               Boolean (False)      Limitation CPU basé sur le JDSK natif, sinon Java Sysmon library est utilisé
cpulimit                     Decimal (0.0)        % de CPU, 1.0 ne produit aucune limite
connlimit                    Integer (0)          Limitation du nombre de connection
digest                       Integer (0)          Utilisation d'un Digest autre que MD5
usefastmd5                   Boolean (True)       Utilisation de la bibliothèque FastMD5
fastmd5                      SODLL                Path vers la JNI. Si vide, la version core de Javasera utilisée
checkversion                 Boolean (False)      Utilisation du protocole etendu (>= 2.3), accès à plus de retour d'information en fin de transfert
globaldigest                 Boolean (True)       Utilisation d'un digest global (MD5, SHA1, ...) par transfert de fichier

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

