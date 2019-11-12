``authent.xml``
###############

.. index:: authent.xml

.. _authent-xml:

Le fichier ``authent.xml`` contient les directives de configurations
des partenaires du serveur.

.. note::

   Les changements dans ce fichier sont pris en compte par import dans la base via la commande
   ``waarp-r66server loadconf``.

Les directives de configuration sont réparties en 1 section :

- :ref:`identity <authent-xml-entry>`: données concernant l'identité
  d'un partenaire

.. _authent-xml-entry:

Section ``entry``
--------------------

=========== ======= ==== ====== =============
Balise      Type    Obl. Défaut Signification
=========== ======= ==== ====== =============
hostid      string  O           Nom de l'hôte
address     string  O           Adresse IP ou nom DNS de l'hôte (si 0.0.0.0, alors c'est un client)
port        integer O           Port associé à l'adresse pour ce nom
isssl       boolean N    False  Cette association désigne un hôte usant des connexions chiffrées
admin       boolean N    False  Cette association désigne un hôte avec droit d'administration via le protocole R66 ; A partir de la version 2.4.9 cela peut être remplacé si par la configuration fichier des rôles
isclient    boolean N    False  Cette association désigne un hôte de type Client
isactive    boolean N    True   Vrai si l'entrée est activée/autorisée
isproxified boolean N    False  Vrai si l'entrée est accessible via un proxy (désactive la vérification de l'IP)
keyfile     string  N           Fichier contenant la clef publique du partenaire au format GGP
key         string  N           Clef publique du partenaire
=========== ======= ==== ====== =============

``keyfile`` ou ``key`` doivent être spécifiés (l'un des deux uniquement).
