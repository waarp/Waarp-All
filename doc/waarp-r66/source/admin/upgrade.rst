###########
Mise à jour
###########


Avec les packages
=================

Pour une installation faite à partir des packages, utiliser une des commandes
suivantes (selon la distribution) :

.. code-block:: bash

   # Avec les dépôts
   yum install waarp-r66

   # avec le package rpm
   yum install path/to/waarp-r66.rpm


Avec les archives autonomes
===========================

Pour une installation faite à partir les packages autonomes, la procédure est
la suivante :

1. Si le serveur R66 ou filewatcher a été installé en tant que service, arrêter
   celui-ci.
   Pour Windows seulement : désinstaller le service.
2. Extraire l'archive au même niveau que l'ancienne installation.
3. Copier le contenu du dossier ``etc`` de l'ancienne installation vers le
   dossier ``etc`` de la nouvelle version.
4. Procéder de même avec le dossier data de l'ancienne installation.
5. Si le serveur R66 ou filewatcher a été installé en tant que service :

   - Pour windows seulement : réinstaller les services depuis la nouvelle
     installation.
   - Pour linux : mettre à jour les chemins du service avec les nouveaux dossiers.

   Enfin, redémarrer les services.

