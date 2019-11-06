Initialisation
##############

Initialiser l'instance
======================

Une fois l'instance {hostid} créée et configurée les commandes suivantes permettent d'initialiser
et de charger sa configuration dans la base de données (fichier authent.xml et rules.xml).

.. code-block:: bash

  #Créer le schéma de la base de données spécifiée dans server.xml
  waarp-r66server {hostid} initdb
  #Charge les données des fichiers authent.xml et rules.xml dans la base de donnés
  waarp-r66server {hostid} loadconf

Lancer l'instance serveur
=========================

Une fois les étapes précédents effectuées vous pouvez démarer un serveur WaarpR66

.. code-block:: bash

   waarp-r66server {hostid} start


Le chapitre suivant aborde les diférentes commandes des moniteurs WaarpR66 avec plus de détails.
