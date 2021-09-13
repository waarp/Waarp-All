.. _raw-commands:

#####################
Commandes Java brutes
#####################


Les commandes Waarp R66 peuvent être appélé directement sans passer par les
scripts :ref:`waarp-r66client` et :ref:`waarp-r66server`.

Tous les appels sont de la forme :

.. code-block:: sh

   java -cp "path/to/waarp/jars" [options jvm] [classname] [class arguments]

Le détail des classes pouvant être exécutées et de leurs arguments est décrit
ci-dessous.


Options additionnelles pour Waarp via la JVM
============================================

.. index:: -Dopenr66.locale

``-Dopenr66.locale=en|fr``
   (défaut = en)

   Permet choisir entre l'anglais ou le français pour le langage de l'interface


.. index:: -Dopenr66.ishostproxyfied

``-Dopenr66.ishostproxyfied=1|0``
   (défaut = 0)

   Indique que ce serveur est derrière un proxy (comme R66Proxy ou un matériel
   équivalent) afin d'empêcher le contrôle des adresses IP de s'appliquer
   (puisque celle-ci sera celle du Proxy)


.. index:: -Dopenr66.startup.warning

``-Dopenr66.startup.warning=1|0``
   (défaut = 1)

   Indique si les messages de démarrage doivent être inscrits dans les logs avec
   le niveau warning (``1``) ou info (``0``).


.. index:: -Dopenr66.startup.checkdb

``-Dopenr66.startup.checkdb=1|0``
   .. deprecated:: 3.1.0

      Utiliser ``-Dopenr66.autoUpgrade`` à la place


.. index:: -Dopenr66.startup.autoUpgrade

``-Dopenr66.startup.autoUpgrade=0|1``
   (défaut = 0)

   .. versionadded:: 3.1.0

   Active la mise-à-jour automatique de la base de données au lancement du
   programme


.. index:: -Dopenr66.chroot.checked

``-Dopenr66.chroot.checked=1|0``
   (défaut = 1)

   Active le mode chroot pour les connexions clientes.

   Par exemple, tenter de récupérer un fichier (RECV) depuis un partenaire
   distant en spécifiant un chemin complet peut être autorisé, même si il est en
   dehors du répertoire "OUT", sauf si checked=1. Si checked=1, alors tous les
   fichiers reçus doivent spécifier un chemin inclu dans "OUT", sans remonter
   au-delà.


.. index:: -Dopenr66.blacklist.badauthent

``-Dopenr66.blacklist.badauthent=1|0``
   (défaut = 1)

   Active le banissement temporaire des partenaires distants en cas d'erreur
   d'authentification. Cette option permet de prévenir les attaques de type
   DDOS.

   Si ``-Dopenr66.ishostproxyfied=1``, alors il est obligatoirement faux. En
   effet, dans ce cas, si un des partenaires a un problème d'authentification,
   alors tous les partenaires proxifiés via le même proxy seront bannis puisque
   visibles depuis la même adresse IP.


.. index:: -Dopenr66.filename.maxlength

``-Dopenr66.filename.maxlength=n``
   (défaut = 255)

   Définit la longueur maximale autorisée pour les nom de fichiers reçus (nom
   temporaire et nom final). Ceci n'empêche pas de changer le nom du fichier
   après et #ORIGINALFILENAME# contient toujours le nom complet d'origine du
   fichier, non tronqué.


.. index:: -Dopenr66.trace.stats

``-Dopenr66.trace.stats=n``
   (défaut = 0)

   pour mettre en debug certaines traces spécifiques sur des données toutes les
   ``n`` secondes. 0 signifie absence de trace.


.. index:: -Dopenr66.cache.limit

.. index:: -Dopenr66.cache.timelimit

``-Dopenr66.cache.limit=n`` et ``-Dopenr66.cache.timelimit=m``
   (défaut n = 5000, m=180000)

   Pour mettre en cache les informations de transfert avec

   - ``n`` est le nombre maximum de tâches à conserver dans un cache LRU (Last
     Recent Used). La valeur minimale est 100
   - ``m`` est le temps maximum en millisecondes avant qu'un élément créé,
     utilisé ou modifié soit évincé du cache. La valeur minimale est 1000 ms
     (1s). Une valeur trop grande peut provoquer des consommations mémoires trop
     importante.


.. index:: -Dopenr66.usespaceseparator

``-Dopenr66.usespaceseparator=0|1``
   (défaut = 0)

   Autorise Waarp à utiliser l'espace comme séparateur mais induit des risques
   de bugs. **USAGE NON RECOMMANDE**


.. index:: -Dopenr66.executebeforetransferred

``-Dopenr66.executebeforetransferred=0|1``
   (défaut = 1)

   Autorise Waarp à exécuter les Error-Tasks si une erreur intervient pendant
   les "pré-task", avant le transfert effectif


.. index:: -Dopenr66.transfer.guid

``-Dopenr66.transfer.guid=0|1``
   (défaut = 1)

   Waarp utilisera les GUID en lieu et place d'une séquence SQL pour déterminer
   l'identifiant unique d'un transfert (l'identifiant unique étant la
   conjonction de cet Id au format Long, et des identifiants du requeteur
   et du requeté. Si désactivé (``0``), la séquence SQL sera utilisé
   (comme avant la version 3.6.0).

   .. versionchanged:: 3.6.1
      Cette option a été activée par défaut (valeur 1) pour optimiser
      et diminuer les soucis de réinstallation avec effacement de la base
      en évitant de devoir repositionner la séquence à une valeur plus élevée
      que la dernière utilisée, mais peut être désactivée (``0``) si
      nécessaire.


.. index:: -Dopenr66.authent.noreuse

``-Dopenr66.authent.noreuse=0|1``
   (défaut = 0)

   Autorise Waarp à ne pas réauthentifier un partenaire qui est déjà connecté
   sur un même lien réseau (channel), par défaut. Si activé, l'authentification
   sera obligatoire pour chaque commande (comme avant la version 3.6.0).

   .. versionchanged:: 3.6.1
      Cette option a été activée par défaut (valeur ``0``) pour optimiser
      mais peut être désactivée (``1``) si nécessaire.

.. index:: -Dwaarp.database.connection.max

``-Dwaarp.database.connection.max=n``
   (défaut = 10)

   Permet d'augmenter le nombre maximum de connection simultanées à la base
   de données, avec un minimum de 2 et un maximum lié à la base elle-même.


.. index:: -Dfile.encoding

``-Dfile.encoding=UTF-8``
   (défaut = UTF-8)

   Configuration du mode des fichiers par défaut (UTF-8 recommandé).


.. index:: -Dio.netty.allocator.type

``-Dio.netty.allocator.type=pooled``
   (défaut = pooled)

   Configuration de la mémoire pour Netty, par défaut utilise le mode "pooled".

.. index:: -Dio.netty.noPreferDirect

``-Dio.netty.noPreferDirect=true``
   (défaut = pooled)

   Configuration de la mémoire pour Netty, par défaut utilise le mode amoindrie
   en mémoire Direct.

.. index:: -Dio.netty.maxDirectMemory

``-Dio.netty.maxDirectMemory=0|-1``
   (défaut = 0)

   Configuration de la mémoire pour Netty, par défaut utilise le mode optimisé
   par Netty (0) en se limitant au maximum que la JVM autorise.
   -1 est possible (contrôle total à Netty) mais aucun nettoyage de la mémoire
   n'étant réalisé, cette valeur tend à augmenter la consommation mémoire de
   manière trop importante.



``org.waarp.client.Message``
============================

.. program:: org.waarp.client.Message

Permet d'échanger un message simple avec un partenaire pour s'assurer de la
connectivité et de l'authentification respective entre les partenaires.

Cette commande accepte les arguments suivants :

.. option:: clientConfigurationFile.xml

  *obligatoire*

  Fichier de confguration client Waarp R66, en mode synchrone

.. option:: -to PARTNER

   *obligatoire*

   Serveur R66 de destination

.. option:: -msg MESSAGE

   *obligatoire pour indiquer le message à transmettre*

   Contenu du message à transmettre. Celui-ci apparaîtra dans les logs respectifs
   des deux serveurs (émetteur et récepteur).


Codes de retour :

====== =============
Code   Signification
====== =============
``0``  Succès
``1``  Les arguments sont incorrects ou le fichier de configuration contient une erreur
``2``  Une erreur s'est produite lors de la tentative de connexion ou d'authentification
====== =============


``org.waarp.client.BusinessRequest``
====================================

.. program:: org.waarp.client.BusinessRequest

Permet de déclencher une action à distance avec un partenaire si le partenaire
demandeur est autorisé (cf. BUSINESS ROLE)

Cette commande accepte les arguments suivants :

.. option:: clientConfigurationFile.xml

  *obligatoire*

  Fichier de confguration client Waarp R66, en mode synchrone

.. option:: -to PARTNER

   *obligatoire*

   Serveur R66 de destination

.. option:: -class FULL.CLASS.NAME

   *obligatoire pour indiquer la classe cible à exécuter de type ExecBusinessTask*

   Nom de la classe à exécuter

.. option:: -arg ARGUMENT

   Argument à appliquer à la classe

.. option:: -nolog

   Désactive les logs pour ce transfert


Codes de retour :

====== =============
Code   Signification
====== =============
``0``  Succès
``2``  Les arguments sont incorrects ou le fichier de configuration contient une erreur
``N``  Une erreur s'est produite lors de la tentative d'exécution
====== =============
