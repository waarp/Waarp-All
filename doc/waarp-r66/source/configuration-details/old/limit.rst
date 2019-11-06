Limit
#####

 * `server.xml`: Optionel
 * `client.xml`: Optionel

.. option:: serverthread

   *Integer* (8)

   Nombre de Threads côté serveur (=Nombre de Coeur)

.. option:: clientthread

   *Integer* (80)

   Nombre de Threads côté client (=10xServeur)

.. option:: memorylimit

   *Integer* (4000000000)

   Limite mémoire pour le processus Java du Serveur R66

.. option:: sessionlimit

   *Integer* (8388608)

   Limite de la bande passante par session (64Mb)

.. option:: globallimit

   *Integer* (67108864)

   Limite de la bande passante globale (512Mb)

.. option:: delaylimit

   *Integer* (10000)

   Delai (ms) entre 2 verification de la bande passante. Plus cette valeur est petite plus
   la limitation est respectée. Cependant une valeur trop faible peut être contre productive.

.. option:: runlimit

   *Integer* (10000)

   Limite bar batch de transferts actifs

.. option:: delaycommand

   *Integer* (5000)

   Délai (ms) entre 2 execution du Commander.

.. option:: delayretry

   *Integer* (30000)

   Délai (ms) entre 2 tentatives d'envoi en cas d'erreur

.. option:: timeoutcon

   *Integer* (30000)

   Délai (ms) avant l'envoi d'un Time Out

.. option:: blocksize

   *Integer* (65536)

   Taille des block. Une valuer entre 9kb et 16Mb est recommandée

.. option:: gaprestart

   *Integer* (30)

   Nombre de blocks renvoyés en cas de nouvelle tentative de transferts

.. option:: usenio

   *Boolean* (False)

   Usage of NIO support for the files. According to the JDK, it can enhance the performances

.. option:: usecpulimit

   *Boolean* (False)

   Limite le CPU au lancement d'un nouveau transfert

.. option:: usejdkcpulimit

   *Boolean* (False)

   Limitation du CPU basé sur le JDK natif ou (si faux) sur la Java Sysmon

.. option:: cpulimit

   *Decimal* (1.0)

   Pourcentage de limitation du CPU (entre 0 et 1), 1 n'impose aucune limitation

.. option:: connlimit

   *Integer* (0)

   Limite de connections concurantes, 0 n'impose aucune limitation

.. option:: lowcpulimit

   *Decimal* (0.0)

   Limite basse de l'utilisation du CPU (entre 0 et 1)

.. option:: highcpulimit

   *Decimal* (0.0)

   Limite haute de l'utilisation du CPU (entre 0 et 1)

.. option:: percentdecrease

   *Decimal* (0.01)

   Modification de la bande passante utilisée quand une limite est atteinte (entre 0 et 1)

.. option:: delaythrottle

   *Integer* (1000)

   Delai (ms) entre 2 modification de l'utilisation de la bande passante 
   (minimum 500 valeur recomandée autour de 1000)

.. option:: limitlowbandwidth

   *Integer* (1000000)

   Limite basse de l'utilisation de la bande passante

.. option:: digest

   *Integer* (2)

   Digest a utilisé, litteral ou nombre: CRC32=0, ADLER32=1, MD5=2, MD2=3, SHA1=4, SHA256=5, SHA384=6, SHA512=7

.. option:: usefastmd5

   *Boolean* (True)

   Utilisation de la biblioteque FastMD5. Améliore les performances des calculs MD5

.. option:: usethrift

   *Integer* (0)

   Une valeur inferieur ou égale à 0 désactive le support de Thrift. Si supérieur à 0 
   (de préference supérieur a 1024) active le support de Thrift sur le port TCP spécifié.

.. option:: checkversion

   *Boolean* (True)

   Vérifie la version du protocole utilisé pour permettre la rétro compatibilité.

.. option:: globaldigest

   *Boolean* (True)

   Active le Digest général par transfert
