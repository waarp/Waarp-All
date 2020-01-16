.. _raw-commands:

#####################
Commandes Java brutes
#####################


Les commandes Waarp R66 peuvent être appélé directement sans passer par les
scripts :ref:`waarp-r66client` et :ref:`waarp-r66server`.

Tous les appels sont de la forme :

.. code-block:: sh

   java -cp "path/to/waarp/jars" [classname] [class arguments]

Le détail des classes pouvant être exécutées et de leurs arguments est décrit
ci-dessous.


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
