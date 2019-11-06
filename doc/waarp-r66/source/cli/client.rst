Waarp R66 Client
################

.. todo:: faire la distinction entre archives et packages

.. todo:: ajouter les commandes filewatcher

.. todo:: ajouter les commandes manquantes

.. contents::

Commandes de gestion des transferts
***********************************

Commande ``waarp-r66client send``
=================================

.. program:: waarp-r66client send

Démarre un transfert synchrone (attend le résultat du transfert avant
de rendre la main).

Cette commande accepte les arguments suivants :


.. option:: -to PARTNER

   *obligatoire*

   Serveur R66 de destination

.. option:: -file FILENAME

   *obligatoire pour démarrer un nouveau transfert*

   Fichier à envoyer

.. option:: -rule RULE

   *obligatoire pour démarrer un nouveau transfert*

   Règle de transfert à utiliser

.. option:: -id

   *obligatoire pour relancer un transfert*

   Identifiant du transfert à relancer

.. option:: -info INFO

   Info complémentaires sur le transfert

.. option:: -block

   Fixe la taille de blocs pour le transfert

.. option:: -md5

   Force le contrôle d'intégrité par paquet (déconseillé)

.. option:: -nolog

   Désactive les logs pour ce transfert

.. option:: -logWarn

   Loggue les messages INFO avec un niveau WARN

.. option:: -notlogWarn

   Loggue les messages INFO avec un niveau INFO


Codes de retour :

====== =============
Code   Signification
====== =============
``0``  Succès
``2``  Les arguments sont incorrects ou le fichier de configuration contient une erreur
``66`` Une erreur inattendue s'est produite
``N``  Les autres codes de sortie correspondent à une erreur de transfert. Il s'agit de la valeur numérique du :ref:`code d'erreur <error-codes>`
====== =============



Commande ``waarp-r66client asend``
==================================

.. program:: waarp-r66client asend


Démarre un transfert asynchrone (enregistre le démarrage du transfert
et de rendre la main).

Cette commande accepte les arguments suivants :

.. option:: -to PARTNER

   *obligatoire*

   Serveur R66 de destination

.. option:: -file FILENAME

   *obligatoire pour démarrer un nouveau transfert*

   Fichier à envoyer

.. option:: -rule RULE

   *obligatoire pour démarrer un nouveau transfert*

   Règle de transfert à utiliser

.. option:: -id

   *obligatoire pour relancer un transfert*

   Identifiant du transfert à relancer


.. option:: -info INFO

   Info complémentaires sur le transfert

.. option:: -block

   Fixe la taille de blocs pour le transfert

.. option:: -md5

   Force le contrôle d'intégrité par paquet (déconseillé)

.. option:: -nolog

   Désactive les logs pour ce transfert

.. option:: -logWarn

   Loggue les messages INFO avec un niveau WARN

.. option:: -notlogWarn

   Loggue les messages INFO avec un niveau INFO

.. option:: -start yyyyMMddHHmmss

   Date à laquelle le transfert doit démarrer

.. option:: -delay timestamp|+NNN

   Si un timestamp est fourni, date à laquelle le transfert doit
   démarrer (sous la forme d'un timestamp UNIX en ms).

   Si une valeur de la forme +NNN est fournie, délais en seconde à
   partir de l'exécution de la commande après lequel le transfert doit
   démarrer

Codes de retour :

====== =============
Code   Signification
====== =============
``0``  Succès
``1``  Les arguments sont incorrects ou le fichier de configuration contient une erreur
``N``  Les autres codes de sortie correspondent à une erreur de transfert. Il s'agit de la valeur numérique du :ref:`code d'erreur <error-codes>`
====== =============



Commande ``waarp-r66client msend``
==================================

.. program:: waarp-r66client msend


Démarre plusieurs transferts synchrones (attend le résultat du transfert avant
de rendre la main).

Cette commande fonctionne sensiblement comme la commande ``send``, mais
permet de lister plusieurs fichiers et plusieurs hôtes de destination :

- En séparant les valeurs dans les arguments ``-to`` et ``-file`` par
  des virgules (``,``)
- En utilisant des "jokers" dans l'argument ``file`` (``*`` pour
  remplacer plusieurs caractères ou ``?`` pour remplacer un caractère
  unique.)

Cette commande accepte les arguments suivants :

.. option:: -to PARTNER

   *obligatoire*

   Serveur R66 de destination

.. option:: -file FILENAME

   *obligatoire pour démarrer un nouveau transfert*

   Fichier à envoyer

.. option:: -rule RULE

   *obligatoire pour démarrer un nouveau transfert*

   Règle de transfert à utiliser

.. option:: -id

   *obligatoire pour relancer un transfert*

   Identifiant du transfert à relancer

.. option:: -info INFO

   Info complémentaires sur le transfert

.. option:: -block

   Fixe la taille de blocs pour le transfert

.. option:: -md5

   Force le contrôle d'intégrité par paquet (déconseillé)

.. option:: -nolog

   Désactive les logs pour ce transfert

.. option:: -logWarn

   Loggue les messages INFO avec un niveau WARN

.. option:: -notlogWarn

   Loggue les messages INFO avec un niveau INFO


Codes de retour :

====== =============
Code   Signification
====== =============
``0``  Succès
``2``  Les arguments sont incorrects ou le fichier de configuration contient une erreur
``22`` Erreur inconnue
``N``  Nombre de transferts en erreur
====== =============



Commande ``waarp-r66client masend``
===================================

.. program:: waarp-r66client masend

Démarre plusieurs transferts asynchrones (enregistre le démarrage du transfert
et de rendre la main).

Cette commande fonctionne sensiblement comme la commande ``asend``, mais
permet de lister plusieurs fichiers et plusieurs hôtes de destination :

- En séparant les valeurs deans les arguments ``-to`` et ``-file`` par
  des virgules (',')
- En utilisant des "jokers" dans l'argument ``file`` (``*`` pour
  remplacer plusieurs caractères ou ``?`` pour remplacer un caractère
  unique.)


Cette commande accepte les arguments suivants :

.. option:: -to PARTNER

   *obligatoire*

   Serveur R66 de destination

.. option:: -file FILENAME

   *obligatoire pour démarrer un nouveau transfert*

   Fichier à envoyer

.. option:: -rule RULE

   *obligatoire pour démarrer un nouveau transfert*

   Règle de transfert à utiliser

.. option:: -id

   *obligatoire pour relancer un transfert*

   Identifiant du transfert à relancer

.. option:: -client

   Doit être ajouté pour si la règle est en mode réception

.. option:: -info INFO

   Info complémentaires sur le transfert

.. option:: -block

   Fixe la taille de blocs pour le transfert

.. option:: -md5

   Force le contrôle d'intégrité par paquet (déconseillé)

.. option:: -nolog

   Désactive les logs pour ce transfert

.. option:: -logWarn

   Loggue les messages INFO avec un niveau WARN

.. option:: -notlogWarn

   Loggue les messages INFO avec un niveau INFO

.. option:: -start yyyyMMddHHmmss

   Date à laquelle le transfert doit démarrer

.. option:: -delay timestamp|+NNN

   Si un timestamp est fourni, date à laquelle le transfert doit
   démarrer (sous la forme d'un timestamp UNIX en ms).

   Si une valeur de la forme +NNN est fournie, délais en seconde à
   partir de l'exécution de la commande après lequel le transfert doit
   démarrer

Codes de retour :

====== =============
Code   Signification
====== =============
``0``  Succès
``1``  Les arguments sont incorrects ou le fichier de configuration contient une erreur
``2``  Erreur de connexion à la base de données ou absence de l'argument -client
``N``  Nombre de transferts dont la programmation est en erreur
====== =============


Commande ``waarp-r66client transfer``
=====================================

.. program:: waarp-r66client transfer

Cette commande permet d'obtenir des informations sur un transfert en
cours ou terminé, et d'agir sur ces transferts

Elle accepte les arguments suivants :

.. option:: -id

   *obligatoire*

   Identifiant du transfert

.. option:: -to

   *Les options -to et -from sont exclusives, et l'une des deux doit
   être fournie*

   Partenaire de destination

.. option:: -from

   *Les options -to et -from sont exclusives, et l'une des deux doit
   être fournie*

   Partenaire de d'origine

.. option:: -cancel

   *Les options -cancel, -stop et -restart sont exclusives*

   Annule le transfert en cours (les fichiers temporaires sont
   supprimés sur le récepteur)

.. option:: -stop

   *Les options -cancel, -stop et -restart sont exclusives*

   Arrête un transfert en cours

.. option:: -restart

   *Les options -cancel, -stop et -restart sont exclusives*

   Redémarre un transfert en erreur

.. option:: -start yyyyMMddHHmmss

   *Ne peut être utilisé qu'avec l'action -restart*

   Date à laquelle le transfert doit démarrer

.. option:: -delay timestamp|+NNN

   Si un timestamp est fourni, date à laquelle le transfert doit
   démarrer (sous la forme d'un timestamp UNIX en ms).

   Si une valeur de la forme +NNN est fournie, délais en seconde à
   partir de l'exécution de la commande après lequel le transfert doit
   démarrer

Codes de retour communs :

====== =============
Code   Signification
====== =============
``0``  Succès
``1``  Les arguments sont incorrects ou le fichier de configuration contient une erreur
``99`` Une erreur inattendue s'est produite
====== =============


Codes de retour pour l'action :option:`-cancel` :

====== =============
Code   Signification
====== =============
``3``  Le transfert est déjà terminé
``4``  L'action demandée n'a pas pu être effectuée
====== =============


Codes de retour pour l'action :option:`-stop` :

====== =============
Code   Signification
====== =============
``3``  L'action demandée n'a pas pu être effectuée
====== =============


Codes de retour pour l'action :option:`-restart` :

====== =============
Code   Signification
====== =============
``3``  L'action demandée n'a pas pu être effectuée
``4``  Le transfert est déjà terminé
``5``  Le partenaire distant a renvoyé une erreur
====== =============



Commande ``waarp-r66client getinfo``
====================================

.. program:: waarp-r66client getinfo


Cette commande permet d'obtenir sur les fichiers disponibles sur un
partenaire distant.

Elle accepte les arguments suivants :

::

.. option:: -to PARTNER

   *Obligatoire*

   Serveur R66 de destination

.. option:: -file FILENAME

   *Obligatoire*

   Fichier à envoyer (peut contenir des caractères de subtitution "*")

.. option:: -rule RULE

   Règle de transfert à utiliser

.. option:: -exist

   Vérifie si le fichier donné exist

.. option:: -detail

   Récupère des informations sur le fichie

.. option:: -list

   Liste les fichiers correspondant au motif donn

.. option:: -mlsx

   Liste les fichiers et récupère leurs détails


Codes de retour communs :

====== =============
Code   Signification
====== =============
``0``  Succès
``1``  Les arguments sont incorrects ou le fichier de configuration contient une erreur
``2``  Une erreur s'est produite durant l'interrogation du partenaire
``3``  Une erreur inattendue s'est produite
====== =============


Commande ``waarp-r66client gui``
================================

Ouvre un client graphique pour démarrer un transfert.

.. warning::

   Ne fonctionne que dans un environnement graphique




Autres commandes
****************

Commande ``waarp-r66client initdb``
===================================

.. program:: waarp-r66client initdb

Initialise la base de données du client.

Cette commande accepte les arguments suivants :

.. option:: -initdb

   Initialise la base de données

.. option:: -upgradeDb

   Met à jour le modèle de la base de données

.. option:: -dir DOSSIER

   Charge les règles de transferts en base depuis  le dossier DOSSIER

.. option:: -auth FICHIER

   Charge les données d'authentification en base depuis le fichier
   FICHIER

.. option:: -limit FICHIER

   Charge les limitation de bande passante en base depuis le fichier
   FICHIER

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


Commande ``waarp-r66client loadconf``
=====================================

.. program:: waarp-r66client loadconf


Charge la configuration (authentification et règles de transfert) depuis
des fichiers XML dans la base de données de WaarpR66 Server.
Il s'agit d'un raccourci vers les deux commandes ``loadauth`` et
``loadrule``.

Les fichiers attendus par la commande sont les suivants :

- ``/comp/waarp/wrs/etc/authent-server.xml`` : fichier contenant les
  données d'authentification
- ``/comp/waarp/wrs/etc/`` : dossier contenant les définitions de règles

Une fois les données chargées en base de données, les fichiers peuvent
être supprimés sans risque.

Codes de retour :

===== =============
Code  Signification
===== =============
``0`` Succès
``1`` WaarpR66 a retourné une erreur durant le chargement des données en base.
===== =============


.. _r66client-log-export:

Commande ``waarp-r66client log-export``
=======================================

.. program:: waarp-r66client log-export

Cette commande permet d'exporter l'historique de transfert du serveur
WaarpR66 associé au client, et le cas échéant de purger l'historique.

Les fichiers XML produit sont déposés dans le dossier ``arch``
définitions dans la configuration du serveur.

.. warning::

   Cette commande ne fonctionne que pour les clients associés à un
   serveur WaarpR66.

   Elle sera déplacée dans le script waarp-r66server.sh dans une version
   future

Cette commande accepte les arguments suivants :

.. option:: -clean

   Corrige le statut des transferts terminés erronés

.. option:: -purge

   Supprime l'historique exporté de la base de données

.. option:: -start DATE

   Exporte seulement l'historique postérieur à cette date

.. option:: -stop DATE

   Exporte seulement l'historique antérieur à cette date

.. option:: -startid ID

   Valeur minimale d'identifiants de transfert à exporter

.. option:: -stopid ID

   Valeur maximale d'identifiants de transfert à exporter

.. option:: -rule RULE

   Limite l'export à une règle spécifique

.. option:: -request HOST

   Limite l'export à un partenaire spécifique

.. option:: -pending

   Limite l'export aux transferts en attente

.. option:: -transfer

   Limite l'export aux transferts en cours

.. option:: -done

   Limite l'export aux transferts terminés

.. option:: -error

   Limite l'export aux transferts en erreur


Les valeurs ``DATE``  doivent avoir le format ``yyyyMMddHHmmssSSS``. La
date peut omettre les derniers éléments (ex: ``20150815``).

Codes de retour :

====== =============
Code   Signification
====== =============
``0``  Succès
``1``  Les arguments sont incorrects ou le fichier de configuration contient une erreur
``10`` Le serveur WaarpR66 associé au client n'est pas trouvé
``20`` Warning
``N``  Les autres codes de sortie correspondent à une erreur de transfert. Il s'agit de la valeur numérique du :ref:`code d'erreur <error-codes>`
====== =============


Commande ``waarp-r66client config-export``
==========================================

.. program:: waarp-r66client config-export

Cette commande permet d'exporter la configuration enregistrée en base de
données du serveur WaarpR66 associé au client.

Les fichiers XML produit sont déposés dans le dossier ``arch``
définitions dans la configuration du serveur.

.. warning::

   Cette commande ne fonctionne que pour les clients associés à un
   serveur WaarpR66.

   Elle sera déplacée dans le script waarp-r66server.sh dans une version
   future

Cette commande accepte les arguments suivants :

.. option:: -hosts

   Exporte les données d'authentification

.. option:: -rules

   Exporte les règles de transfert

.. option:: -business

   Exporte les données business

.. option:: -alias

   Exporte les alias du serveur

.. option:: -role

   Exporte les rôles du serveur

.. option:: -host HOST

   Envoi la demande d'export au serveur HOST


Codes de retour :

====== =============
Code   Signification
====== =============
``0``  Succès
``1``  Les arguments sont incorrects ou le fichier de configuration contient une erreur
``10`` Le serveur WaarpR66 associé au client n'est pas trouvé
``20`` Warning
``N``  Les autres codes de sortie correspondent à une erreur de transfert. Il s'agit de la valeur numérique du :ref:`code d'erreur <error-codes>`
====== =============
