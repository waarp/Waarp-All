#####################
Liste des changements
#####################


Waarp R66 3.3.0 (2020-01-18)
============================

Mise à jour
-----------

Pour une installation faite à partir des packages, utiliser une des commandes
suivantes (selon la distribution) :

.. code-block:: bash

   # Avec les dépôts
   yum install waarp-r66

   # avec le package rpm
   yum install path/to/waarp-r66-3.2.1-1.el6.noarch.rpm


Pour une installation faite à partir les packages autonomes, la procédure est
la suivante :

1. Si le serveur R66 ou filewatcher a été installé en tant que service, arrêter
   celui-ci.
   Pour Windows seulement : désinstaller le service.
2. Extraire l'archive au même niveau que l'ancienne installation.
3. Copier le contenu du dossier ``etc/conf.d`` de l'ancienne installation vers le
   dossier ``etc/conf.d`` de la nouvelle version.
4. Procéder de même avec le dossier data de l'ancienne installation.
5. Si le serveur R66 ou filewatcher a été installé en tant que service :

   - Pour windows seulement : réinstaller les services depuis la nouvelle
     installation.
   - Pour linux : mettre à jour les chemins du service avec les nouveaux dossiers.

   Enfin, redémarrer les services.

Améliorations
-------------

- Ajout des propriétés suivantes à la sortie des commandes ``*send`` :
  ``specialid``, ``finalPath``, ``originalPath``, ``statusCode``, ``ruleid``,
  ``requested``, ``requester``, ``fileInformation``, ``originalSize``
- Amélioration de la prise en compte d'un transfert échoué sur connexion
  impossible pour rejeu
- Amélioration de la détection au plus tôt de l'absence d'un fichier lors d'une
  demande d'émission
- Amélioration de la prise en compte d'un fichier déjà pris en compte par
  FileWatcher mais modifié après, sans être effacé (ce qui n'est pas une bonne
  pratique) : le fichier sera reprogrammé pour un nouveau transfert. Cette
  amélioration est désactivable avec l'option ``-ignoreAlreadyUsed=true``
- Mise à jour des dépendances externes


Waarp R66 3.2.0 (2019-10-25)
============================

Mise à jour
-----------

Pour une installation faite à partir des packages, utiliser une des commandes
suivantes (selon la distribution) :

.. code-block:: bash

   # Avec les dépôts
   yum install waarp-r66

   # avec le package rpm
   yum install path/to/waarp-r66-3.2.0-1.el6.noarch.rpm


Pour une installation faite à partir les packages autonomes, la procédure est
la suivante :

1. Si le serveur R66 ou filewatcher a été installé en tant que service, arrêter
   celui-ci.
   Pour Windows seulement : désinstaller le service.
2. Extraire l'archive au même niveau que l'ancienne installation.
3. Copier le contenu du dossier ``etc/conf.d`` de l'ancienne installation vers le
   dossier ``etc/conf.d`` de la nouvelle version.
4. Procéder de même avec le dossier data de l'ancienne installation.
5. Si le serveur R66 ou filewatcher a été installé en tant que service :

   - Pour windows seulement : réinstaller les services depuis la nouvelle
     installation.
   - Pour linux : mettre à jour les chemins du service avec les nouveaux dossiers.

   Enfin, redémarrer les services.


Sécurité
--------

- Support de TLS 1.2 pour toutes les versions de JRE

Nouveautés
----------

- Refonte Db

Améliorations
-------------

- Diminution du nombre de threads utilisés
- Optimisation de l'utilisation de ressources externes (RAM, CPU)
- Mise à jour des dépendances externes

Correctifs
----------

- Suppressions d'erreurs de type "deadlocks"



Waarp R66 3.1.0-1 (non publiée)
===============================

.. note:: 

   En raison de bugs bloquants, cette version n'a pas été publiée.

Sécurité
--------

- Corrige un bug permettant de contourner l'obligation d'un canal SSL

Nouveautés
----------

- Nouvelle version de l'API REST ([documentation](interface/restv2/index.html))


Améliorations
-------------

- Les regexes du filewatcher permettent de filtrer sur le chemin complet des
  fichiers et non juste le nom du fichier
- les scripts ``waarp-r66client`` et ``waarp-r66server`` permettent de mettre à jour
  la base de données.

Correctifs
----------

- Corrige les code retour d'initialisation de la base de données
- Corrige les messages d'erreur suite à un échec de connexion
- Renomme l'option ``dbcheck`` de la configuration de base données en ``autoupgrade``
- Corrige les messages d'erreur au chargement de la page "Cancel-Restart" de l'interface d'admin
- Les services sont arrêtés avec le signal ``interrupt`` plutôt qu'``usr1`` pour
  permettre un arrêt normal du service
- Mise à jour des dépendances externes
- Optimisation de l'utilisation de connexions à la base de données
- Les scripts linux ``waarp-r66client`` et ``waarp-r66server`` permettent de
  mettre à jour le modèle de données

Dépréciations
-------------

- L'option de configuration ``dbcheck`` est dépréciée


Waarp R66 3.0.12-1 (2019-05-10)
===============================

Mise à jour
-----------

Pour une installation faite à partir des packages, utiliser une des commandes
suivantes (selon la distribution) :

.. code-block:: bash

   # Avec les dépôts
   yum install waarp-r66

   # avec le package rpm
   yum install path/to/waarp-r66-3.0.12-1.el6.noarch.rpm


Pour une installation faite à partir les packages autonomes, la procédure est
la suivante :

1. Si le serveur R66 ou filewatcher a été installé en tant que service, arrêter
   celui-ci.
   Pour Windows seulement : désinstaller le service.
2. Extraire l'archive au même niveau que l'ancienne installation.
3. Copier le contenu du dossier ``etc/conf.d`` de l'ancienne installation vers le
   dossier ``etc/conf.d`` de la nouvelle version.
4. Procéder de même avec le dossier data de l'ancienne installation.
5. Si le serveur R66 ou filewatcher a été installé en tant que service :

   - Pour windows seulement : réinstaller les services depuis la nouvelle
     installation.
   - Pour linux : mettre à jour les chemins du service avec les nouveaux dossiers.

   Enfin, redémarrer les services.



Correctifs
----------

- Corrige des problèmes de perte de connexions à la base de données



Waarp R66 3.0.11-1 (2019-02-20)
===============================

Mise à jour
-----------

Pour une installation faite à partir des packages, utiliser une des commandes
suivantes (selon la distribution) :

.. code-block:: bash

   # Avec les dépôts
   yum install waarp-r66

   # avec le package rpm
   yum install path/to/waarp-r66-3.0.11-1.el6.noarch.rpm


Pour une installation daiteà partir les packages autonomes, la procédure est
la suivante :

1. Si le serveur R66 ou filewatcher a été installé en tant que service, arrêter
   celui-ci.
   Pour Windows seulement : désinstaller le service.
2. Extraire l'archive au même niveau que l'ancienne installation.
3. Copier le contenu du dossier ``etc/conf.d`` de l'ancienne installation vers le
   dossier ``etc/conf.d`` de la nouvelle version.
4. Procéder de même avec le dossier data de l'ancienne installation.
5. Si le serveur R66 ou filewatcher a été installé en tant que service :

   - Pour windows seulement : réinstaller les services depuis la nouvelle
     installation.
   - Pour linux : mettre à jour les chemins du service avec les nouveaux dossiers.

   Enfin, redémarrer les services.



Correctifs
----------

- Correction du support des espaces dans les tâches TRANSFER
- Correction d'un NullPointerException au lancement du filewatcher
- Correctif dans le lancement des transferts asynchrones
- Suppression de la valeur miminum pour l'option runlimit
- Arrête l'envoi de paquets quand le transfert est stoppé ou annulé
- Correction de la commande exécutée sous Windows dans les tâches EXEC* si des
  slashes ("/") sont utilisés dans le chemin de l'exécutable
- Ajout d'un délais de 5 minutes entre de tentatives de redémarrage du serveur
  R66 en cas d'échec de lancement dans les services systèmes (systemd et
  Windows).

Packaging
---------

- ``manager-send.sh`` génère un fichier ``get-files.list`` pour Waarp Gateway
  SFTP : ce fichier est consommé par le script ``waarp-get-sftp.sh`` (livré avec
  les packages de la passerelle) pour interroger périodiquement les serveurs
  distants.
- ``waarp-pull.sh`` ne démarre plus qu'un seul transfert pour le fichiers
  disponibles.

Waarp R66 3.0.10-1 (2018-10-08)
===============================

Correctifs
----------

- Support des espaces dans les tâches des chaînes de traitement
- Support des chemins UNC sous windows


Waarp R66 3.0.9-2 (2018-07-16)
==============================

Correctifs
----------

- Correction de la gestion de la configuration des filewatchers par Manager
- Correction du redémarrage des filewatchers sous windows


Waarp R66 3.0.9 (2018-01-08)
============================

Correctifs
----------

- Mise à jour des dépendances externes
- Correction de l'erreur de chargement des données dans l'interface d'administration
- Le serveur Waarp R66 ne démarre plus si les ports sont déjà utilisés
- Les chemins de destination des tâches RENAME, MOVE, MOVERENAME, COPY, COPYRENAME peuvent contenir des espaces
- Correction du blocage des transferts asynchone quand leur nombre est supérieur à clientthread+11
- Correction d'un interblocage quand le nombre de transferts simultanés approche la valeur de clientthread
- Correction d'une fuite de mémoire
- Le Filewatcher ne démarrait pas quand fileinfo n'était pas renseigné dans le fichier de configuration

