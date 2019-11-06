Waarp R66 Server
################

.. todo:: faire la distinction packages/archives
.. todo:: ajouter les commandes manquantes

.. contents::

Gestion du service
******************

Le serveur WaarpR66 peut être démarré et arrêté avec le script ``waarp-r66server``.

Les commandes suivantes sont disponibles :


Commande ``waarp-r66server start``
==================================

.. program:: waarp-r66server start


Démarrage du serveur.

Codes de retour:

====== =============
Code   Signification
====== =============
``0``  Succès
``1``  Erreur au démarrage ou le serveur est déjà démarré
====== =============


Commande ``waarp-r66server stop``
=================================

.. program:: waarp-r66server stop

Arrêt du serveur

Codes de retour:

====== =============
Code   Signification
====== =============
``0``  Succès
``1``  Le serveur est déjà arrêté ou le signal d'arrêt n'a pas pu être envoyé au serveur
====== =============


Commande ``waarp-r66server status``
===================================

.. program:: waarp-r66server status

Statut du serveur (démarré ou arrêté)

Codes de retour:

====== =============
Code   Signification
====== =============
``0``  Le serveur est démarré
``1``  le serveur est arrêté
====== =============


Commande ``waarp-r66server restart``
====================================

.. program:: waarp-r66server restart

Redémarrage du serveur

Codes de retour:

====== =============
Code   Signification
====== =============
``0``  Succès
``1``  Erreur au démarrage ou le serveur est déjà démarré
====== =============

Autres commandes
----------------

D'autres commandes de maintenance sont disponibles pour le même exécutable
``waarp-r66server``.


Commande ``waarp-r66server initdb``
===================================

.. program:: waarp-r66server initdb

Initialise la base de données.

Syntaxe d'appel :

::

   waarp-r66server initdb [OPTIONS]

Cette commande accepte les arguments suivants :

.. option:: -initdb

   Initialise la base de données

.. option:: -upgradeDb

   Met à jour le modèle de la base de données

.. option:: -dir DOSSIER

   Charge les règles de transferts en base depuis le dossier DOSSIER

.. option:: -auth FICHIER

   Charge les données d'authentification en base depuis le fichier FICHIER

.. option:: -limit FICHIER

   Charge les limitation de bande passante en base depuis le fichier FICHIER

.. option:: -loadAlias FICHIER

   Charge les alias du serveur en base en base depuis le fichier FICHIER

.. option:: -loadRoles FICHIER

   Charge les rôles du serveur en base depuis le fichier FICHIER

.. option:: -loadBusiness FICHIER

   Charge les données business en base depuis le fichier FICHIER


Codes de retour :

===== =============
Code  Signification
===== =============
``0`` Succès
``1`` Les arguments sont incorrects ou le fichier de configuration contient une erreur
``2`` Une erreur SQL s'est produite durant l'initialisation de la base
===== =============


Commande ``waarp-r66server loadauth``
=====================================

.. program:: waarp-r66server loadauth

Charge les données d'authentification depuis un fichier XML donné en
argument dans la base de données de WaarpR66 Server.

Syntaxe d'appel :

::

   waarp-r66server loadauth /path/to/authent.xml

Codes de retour :

===== =============
Code  Signification
===== =============
``0`` Succès
``1`` WaarpR66 a retourné une erreur durant le chargement des données en base.
===== =============



Commande ``waarp-r66server loadrule``
=====================================

Charge les règles de transfert depuis un dossier donné en argument dans
la base de données de WaarpR66 Server.

Syntaxe d'appel :

::

   waarp-r66server loadrule /path/to/rules_dir

Codes de retour :

===== =============
Code  Signification
===== =============
``0`` Succès
``1`` WaarpR66 a retourné une erreur durant le chargement des données en base.
===== =============



Commande ``waarp-r66server loadconf``
=====================================

Charge la configuration (authentification et règles de transfert) depuis
des fichiers XML dans la base de données de WaarpR66 Server.
Il s'agit d'un raccourci vers les deux commandes ``loadauth`` et
``loadrule``.

Les fichiers attendus par la commande sont les suivants :

- ``/etc/waarp/{hostid}/authent-server.xml`` : fichier contenant les données d'authentification
- ``/etc/waarp/{hostid}/`` : dossier contenant les définitions de règles

Une fois les données chargées en base de données, les fichiers peuvent
être supprimés sans risque.

Codes de retour :

===== =============
Code  Signification
===== =============
``0`` Succès
``1`` WaarpR66 a retourné une erreur durant le chargement des données en base.
===== =============
