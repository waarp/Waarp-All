Spooled Directory
#################

* `client.xml`: Optionel

.. option:: stopfile

   *pathType*

   Nom du fichier d'arrêt du spooled service. Quand le fichier est détecté le service s'arrête.

.. option:: logWarn
   
   *boolean* (True)

   Si vrai log les fichier envoyé et supprimé au niveau WARN, si faux log au niveau INFO

.. option:: spooled
   
   *This* will allow to precise SpooledDirectoryTransfer options directly in the configuration file(from version 2.4.22).

.. option:: name

   *nonEmptyString*

   Le nom du Spooled Directory Daemon. Si non spécifié le nom de l'hôte et le nom des dossiers

.. option:: to

   *nonEmptyString*

   Les hostids des moniteurs recepteurs

.. option:: rule
   
   *nonEmptyString*

   La règle utilisée pour envoyer les fichiers reçus sur les dossiers observés

.. option:: statusfile
   
   *pathType*

   L'unique fichier de statut
   
.. option:: directory
   
   *directoryType*

   Les dossiers observés

.. option:: regex
   
   *nonEmptyString*

   Expression régulière pour filtrer les fichiers, .*\.zip$ pour les fichiers zip
   
.. option:: recursive

   *booleanType* (True)

   Utilisation d'un scan de dossier récursif

.. option:: elapse

   *nonNulInteger* (1000)

   Le délai (ms) entre 2 scans des dossiers
   
.. option:: submit

   *booleanType* (True)

   Vrai le Daemon ne fait que soumettre les transferts. Faux le Daemon transfert directement
   les fichiers.

.. option:: parallel

   *booleanType* (True)

   Vrai authorise le parallélisme des transferts. Faux les transferts sont
   effectués séquentiellement.

.. option:: limitParallel

   *nonNegInteger* (0)

   Limitation du nombres de transferts directs concurents. 0 ne donne aucune limite.

.. option:: info

   *nonEmptyString*

   Les informations envoyées à chaque fichier trouvé

.. option:: md5

   *booleanType* (False)

   Utilisation du mode digeste pour le transfert des packets

.. option:: block

   *nonNulInteger* (65536)

   La taille des blocks utilisé dans le transfert

.. option:: nolog

   *booleanType* (False)

   Vrai ne trace pas les transferts du côté client (activable seulement en transfert direct)

.. option:: waarp

   *nonEmptyString*

   L'hostname sous lequel les informations du Daemon sont envoyées aux serveurs. 
   Le client doit etre autorisé à utiliser BusinessRequest dans les hôtes ciblés

.. option:: elapseWaarp

   *nonNegInteger* (5000)

   Delai (ms) entre 2 envois des informations du Daemon aux serveurs. 0, les informations
   sont envoyées après chaque transfert.

.. option:: minimalSize

   *nonNegInteger* (1)
   
   La taille minimal des fichier à envoyer
   The minimal size of each file that will be transferred (default: 1 byte)
