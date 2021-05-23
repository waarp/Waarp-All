Mise à jour
###########

Pré-requis
==========

Les prérequis pour WaarpR66 sont les suivants :

* Un OS supporté : Linux (toutes distributions) et Windows 32 ou 64 bits
* Java 1.6 minimum (java 1.8 recommandé)
* Base de données PostgreSQL (version 9.4 minimum) est recommandée, MySQL,
  MariaDB, Oracle SQL server et H2 sont supportés.
* Les interfaces web nécessitent un navigateur récent (Chrome, Firefox, Internet
  Explorer 10+).
* Mémoire vive :

  * Pour un client : 128Mo de RAM minimum ; 512Mo recommandés
  * Pour un serveur : 512Mo de RAM minimum ; au moins 1Go recommandés.
    L'utilisation dépend de la charge du service. Par exemple, un serveur Waarp
    R66 a besoininstallation.rst d'au moins 2Go pour traiter 5 000 transferts simultanés.

  Par ailleurs, ces valeurs ne prennent en compte que les besoins des services
  R66. Si des transferts lancent des processus externes dans les chaînes de
  traitement (tâches `EXEC`), les besoins en RAM de ces processus viennent en
  sus.

Le chemin vers le dossier contenant Java peut être renseigné dans la variable
d'environnement :envvar:`JAVA_HOME` (ex: ``export JAVA_HOME=/usr/lib/jvm/java8``
ou ``set JAVA_HOME="C:\Java"``).

.. important:: 
   
   La variable d'environnement :envvar:`JAVA_HOME` doit être définie pour les
   systèmes Windows.


Avec les packages
=================

Pour une installation faite à partir des packages, utiliser une des commandes
suivantes (selon la distribution) :

.. code-block:: bash

   # Avec les dépôts
   yum install waarp-r66

   # avec le package rpm
   yum install path/to/waarp-r66.rpm

Pour la mise à jour de la base, il est utile de lancer la commande suivante :

.. code-block:: bash

    waarp-r66server {hostid} initdb -upgradeDb

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

6. Mettre à jour le schéma de la base (si elle est utilisée) :

.. code-block:: bash

    waarp-r66server {hostid} initdb -upgradeDb

Enfin, redémarrer les services.

Avec les jars
=============

Il est recommandé d'utiliser les jars ``with-dependencies``.

Vous pouvez remplacer le jar en cours d'usage par un nouveau jar téléchargé.

Il est ensuite fortement recommande de procéder à la mise à jour de la base.
Aucune perte de données, seul les schémas des tables et les index seront modifiés
pour s'adapter à la dernière version, selon des mises à jours progressives,
en fonction de la version installée (depuis la version ``2.4.23``).

.. code-block:: bash

    waarp-r66server {hostid} initdb -upgradeDb

.. _page de téléchargement: https://dl.waarp.org
