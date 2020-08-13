#####################
Liste des changements
#####################

La procédure de mise à jour est disponible ici: :any:`upgrade`

Non publié
==========

Waarp R66 3.4.1 (2020-08-18)
============================

Nouvelles fonctionnalités
-------------------------

- [`#74 <https://github.com/waarp/Waarp-All/pull/74>`__]
  Les interfaces réseaux sont spécifiables en plus du port à utiliser.
  Plusieurs interfaces sont possibles (séparées par une virgule).

Correctifs
----------

- [`#72 <https://github.com/waarp/Waarp-All/pull/72>`__]
  Le commander pouvait être bloqué dans certains cas.
  (issue [`#65 <https://github.com/waarp/Waarp-All/issues/65>`__])
- [`#71 <https://github.com/waarp/Waarp-All/pull/71>`__]
  XMLRuleDAO ne prenait pas en compte les règles de transferts (cas d'un
  client sans base comme le FileMonitor)
  (issue [`#64 <https://github.com/waarp/Waarp-All/issues/64>`__])
- [`#69 <https://github.com/waarp/Waarp-All/pull/69>`__]
  Des actions dans le menu Système de l'interface d'administration
  étaient manquantes.
  (issue [`#63 <https://github.com/waarp/Waarp-All/issues/63>`__])
- [`#70 <https://github.com/waarp/Waarp-All/pull/70>`__]
  Un transfert d'un client vers lui-même (self-transfert) provoquait
  un effacement du transfert.
  (issue [`#62 <https://github.com/waarp/Waarp-All/issues/62>`__])
- [`#68 <https://github.com/waarp/Waarp-All/pull/68>`__]
  La page Web admin était cassée avec les map dans le champ Information
  de transfert.
  (issue [`#61 <https://github.com/waarp/Waarp-All/issues/61>`__])
- [`#67 <https://github.com/waarp/Waarp-All/pull/67>`__]
  Les options de sorties (csv, xml, json, property) sont rétablies.
  (issue [`#60 <https://github.com/waarp/Waarp-All/issues/60>`__])
- [`#66 <https://github.com/waarp/Waarp-All/pull/66>`__]
  EXECOUTPUT provoquait une erreur de mappage de classe
  (issue [`#59 <https://github.com/waarp/Waarp-All/issues/59>`__])


Waarp R66 3.4.0 (2020-07-17)
============================

Nouvelles fonctionnalités
-------------------------

- [`#49 <https://github.com/waarp/Waarp-All/pull/49>`__]
  Pour les transferts, une nouvelle fonctionnalité permet de gérer le suivi
  fin des retransferts (rebonds entre plusieurs serveurs R66). Cette option
  positionne un champ dans la partie ``information de transfert`` de la forme
  suivante : ``{"follow": numeroUnique}`` pour le premier transfert et les
  transferts suivants récupèreront ainsi cette information nativement.

  Pour les anciennes versions, il est possible de simuler cette option manuellement
  en spécifiant pour le premier transfert dans le champ ``-info`` (``information de transfert``)
  un Json de type ``{"follow": numeroUnique}`` en attribuant un numéro unique
  (comme un timestamp).

  Cette option est active par défaut. Pour la désactiver, il faut préciser l'option
  ``-nofolow``.

- L'interface REST V2 intègre l'option de recherche par ``followId``
  (``GET /v2/transfers/?followId=number``). ``number`` étant possiblement un entier
  long, il est conseillé de le manipuler en chaîne de caractères.

  Pour les anciennes versions, il faut requêter tous les transferts et filtrer ensuite
  sur le champ ``transferInformation`` selon la présence d'un champ ``follow`` suivi
  d'un numéro au format Json.
- [`#48 <https://github.com/waarp/Waarp-All/pull/48>`__]
  Une nouvelle tâche nommée ``ICAP`` est créée afin de permettre  l'échange avec
  un serveur répondant à la norme RFC 3507 dite ``ICAP``.
  Elle permet de transférer le contenu du fichier vers un service ICAP via une
  commande ``RESPMOD`` et d'obtenir la validation de ce fichier par le service
  (statut ``204``).
- Packaging : ajout de la commande ``icaptest`` aux scripts ``waarp-r66client``
  pour tester les paramètres ICAP

Évolutions
----------

- [`#51 <https://github.com/waarp/Waarp-All/pull/51>`__] Les valeurs par défaut
  des limitations de bande passante ont changées : La limitation globale par
  défaut est maintenant de 100Gbps, et celle par connexion est de 1Gbps (ces
  valeurs peuvent être ajustées dans les fichiers de configuration).
- [`#51 <https://github.com/waarp/Waarp-All/pull/51>`__] La valeur par défaut
  de la RAM maximale utilisée par les services WEB et REST a été abaissée à 1Go
  (au lieu de 4Go) (cette valeur peut être ajustée dans les fichiers de
  configuration).

Correctifs
----------

- [`#50 <https://github.com/waarp/Waarp-All/pull/50>`__]
  Le log géré par LogBack génère parfois des logs au démarrage d'information
  ou de debug qui peuvent être évités (en conservant les Warnings et les Erreurs)
  via l'ajout dans le fichier de configuration ``logback.xml`` les paramètres
  suivants en tête des options :

.. code-block:: xml

  <statusListener
    class="org.waarp.common.logging.PrintOnlyWarningLogbackStatusListener" />

- Packaging : les modèles de configuration intègrent le nouveau
  ``StatusListener`` dans la configuration des logs
- [`#51 <https://github.com/waarp/Waarp-All/pull/51>`__]
  Diminution de l'empreinte mémoire pour le cas des clients simples et diminution
  de la mémoire côté serveur pour les parties Web et REST.
  (issue [`#52 <https://github.com/waarp/Waarp-All/issues/52>`__])
- [`#51 <https://github.com/waarp/Waarp-All/pull/51>`__] Si aucun argument
  ``-Xms`` n'est passé à la JVM lors du démarrage, la valeur par défaut de la
  JVM s'applique (en général 4Go).
- [`#54 <https://github.com/waarp/Waarp-All/pull/54>`__] Prise en charge
  correcte du filtrage par expression régulière dans le *file watcher* (il
  était impossible de filtrer juste sur le nom d'un fichier situé dans un
  sous-dossier).
- [`#57 <https://github.com/waarp/Waarp-All/pull/57>`__] Certaines commandes
  ne fonctionnaient plus suite à un bug sur les logs.
  (issue [`#56 <https://github.com/waarp/Waarp-All/issues/56>`__])
- Mise à jour des dépendances
- Packaging : les scripts ``waarp-r66server`` utilisaient la configuration
  client pour certaines sous-commandes
- Packaging : Arrêt des serveurs avec le signal ``HUP`` plutôt que ``INT``


Waarp R66 3.3.4 (2020-06-02)
============================

Correctifs
----------

- [`#31 <https://github.com/waarp/Waarp-All/pull/31>`__]
  Corrige la régression sur la sélection d'un transfert à partir de son ID
  où le nom du serveur local ne prenait pas en compte si le serveur
  distant était en mode SSL ou pas (régression en 3.0).
- Corrige la documentation (maven site) pour WaarpHttp
- Corrige les dépendences dans les shading jars et les pom
- Corrige l'interface DbHostConfiguration dans le Web Admin
- Corrige la classe HttpWriteCacheEnable
- [`#35 <https://github.com/waarp/Waarp-All/issues/35>`__] Corrige le Web Admin
  sur les écrans Listing et CancelRestart pour le tri selon le specialId et pour
  le boutton "Clear"
- [`#37 <https://github.com/waarp/Waarp-All/issues/37>`__] Corrige l'interface
  RESTV2 pour les accès avec droits non pris en compte
- Nettoyage du code
- Corrige l'intégration de SonarQube avec Maven
- [`#38 <https://github.com/waarp/Waarp-All/pull/38>`__] Corrige l'exemple de
  la documentation sur l'authentification HMAC
- [`#42 <https://github.com/waarp/Waarp-All/pull/42>`__] Correction de la
  signature des requêtes dans l'API REST v2
- [`#43 <https://github.com/waarp/Waarp-All/pull/43>`__] Correction de
  l'authentification HMAC de l'API REST v2
- [`#45 <https://github.com/waarp/Waarp-All/pull/45>`__] Correction d'un bug
  sur la taille des paquets

Waarp R66 3.3.3 (2020-05-07)
============================

Correctifs
----------

- [`#20 <https://github.com/waarp/Waarp-All/pull/20>`__] Corrige l'affichage
  d'un transfert dont la règle n'existe plus dans l'interface
  d'administration Web Waarp OpenR66 et empêche l'effacement d'une règle
  tant qu'il existe au moins un transfert qui l'utilise dans sa définition.
  (issue [`#19 <https://github.com/waarp/Waarp-All/issues/19>`__])
- [`#23 <https://github.com/waarp/Waarp-All/pull/23>`__] Corrige la prise
  en compte d'un chemin sous Windows avec \ qui se double en \\
  (issue [`#22 <https://github.com/waarp/Waarp-All/issues/22>`__])
- [`#25 <https://github.com/waarp/Waarp-All/pull/25>`__] Corrige l'arrêt
  immédiat du serveur Waarp GW FTP après son démarrage (introduit en 3.1)
  (issue [`#24 <https://github.com/waarp/Waarp-All/issues/24>`__])
- [`#27 <https://github.com/waarp/Waarp-All/pull/27>`__] Corrige l'absence
  de connections à la base de données pour l'interface d'administration
  en mode Responsive
  (issue [`#26 <https://github.com/waarp/Waarp-All/issues/26>`__])
- [`#30 <https://github.com/waarp/Waarp-All/pull/30>`__]
  Corrige la régression sur la répétition à l'infini des tentatives
  de connexion depuis la version 3.1. Le principe de 3 tentatives avant échec
  est rétabli.
- Corrige les dépendances externes (et le style)

Waarp R66 3.3.2 (2020-04-21)
============================

Correctifs
----------

- Corrige les tests Rest V1
- Corrige des méthodes manquantes dans le module WaarpHttp
- Mise à jour des dépendances externes (compatibles Java 6)
- Correction de l'API Rest V2 /v2/hostconfig/ qui retourne versionR66
  (version du protocole) et versionBin (version du code)

   - La version retournée par l'API V1 n'est plus conforme suite la mise à jour
    automatique du schéma de la base de données.

- Corrige une fuite mémoire API Rest
- Corrige le cas du blocage d'un client lorsqu'il n'est pas reconnu par un
  serveur distant


Waarp R66 3.3.1 (2020-02-17)
============================

Correctifs
----------

- [`#13 <https://github.com/waarp/Waarp-All/pull/13>`__] Corrige l'oubli du
  module WaarpPassword dans les autres modules dans les packages
  `jar-with-dependencies` et en crée un pour WaarpPassword ;
  Met à jour les dépendances pour SonarQube (usage interne)
- [`#9 <https://github.com/waarp/Waarp-All/pull/9>`__] Corrige une régression
  sur l'API REST v1 introduite dans la version 3.2.0
- [`#10 <https://github.com/waarp/Waarp-All/pull/10>`__] Corrige une régression
  qui empêche les ports négatifs pour les partenaires introduite dans la version
  3.2.0


Waarp R66 3.3.0 (2020-01-18)
============================

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

Correctifs
----------

- Corrige des problèmes de perte de connexions à la base de données



Waarp R66 3.0.11-1 (2019-02-20)
===============================

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

