Installation
############

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

Linux
=====

Avec les packages systèmes
--------------------------

Des packages RPM sont fournis pour RHEL 6 et 7 (et Centos/Scientific Linux).

Prérequis :
- le package java-1.7.0-openjdk doit être installé au préalable

Télécharger la dernière version du fichier RPM correspondant à la version de
votre système d’exploitation depuis la `page de téléchargement`_.

Installer les RPM avec les commandes :

.. code-block:: bash

  rpm -i waarp-r66-server-[version].rpm

Vous pouvez ensuite passer à la configuration.

Avec les dépôts Waarp
---------------------

Pour faciliter l’installation et les mises à jour de WaarpR66, nous fournissons
des dépôts pour RHEL 6 (et Centos/Scientific Linux).

Pour ajouter les dépôts Waarp à votre système, suivez la procédure indiquée sur
notre `page de téléchargement`_.

Après avoir suivi cette procédure, vous pouvez installer WaarpR66 avec la
commande :

.. code-block:: bash

  yum install waarp-r66-server

Vous pouvez ensuite passer à la configuration.

Avec les archives autonomes
---------------------------

L’archive est autonome et fonctionne sur toutes les distributions linux.
Il suffit de la décompresser et de suivre la procédure de configuration pour
pouvoir utiliser WaarpR66.

Télécharger la dernière version tar.gz pour linux depuis la
`page de téléchargement`_.

Décompressez ensuite l’archive :

.. code-block:: bash

  tar -xf waarp-r66-[version]_linux.tar.bz2

Vous pouvez ensuite passer à la configuration.


Windows
=======

Avec les archives autonomes
---------------------------

L’archive est autonome et fonctionne sur toutes les versions de Windows.
Il suffit de la décompresser et de suivre la procédure de configuration
pour pouvoir utiliser WaarpR66.

Télécharger la dernière version zip pour Windows depuis la
`page de téléchargement`_.
Décompressez l’archive `waarp-r66-[version]_windows.zip` et passez à la
configuration.


La section suivante détaille le contexte multi-instance de WaarpR66 ainsi que
la création d'une de ces instances.


.. _page de téléchargement: https://dl.waarp.org
