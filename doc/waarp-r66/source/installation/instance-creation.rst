Creation d'instance
###################

Principe généraux
=================

Waarp R66 est une solution multi-instance. 
Une unique installation peut servir à plusieurs moniteurs.

Pour cela le dossier de configuration répertorie les configurations des 
différentes instances dans un dossier du nom de leur ``HOSTID``.

Le dossier de configuration dépends du type d'installation de waarp:

* :file:`/etc/waarp/conf.d` pour une installation via les rpms
* :file:`etc/conf.d` à partir du dossier d'extraction pour les archives 
  autoportantes

Dans ce document ce dossier est nommé ``CONFDIR``.

Ainsi un serveur portant 3 moniteurs (server1, server2, server3) Waarp R66 
aurait une arborsence semblable à celle ci-dessous.

* :file:`{CONFDIR}/server1`
* :file:`{CONFDIR}/server2`
* :file:`{CONFDIR}/server3`



Création d'une instance Linux
=============================

Création de la configuration
----------------------------

Pour facililter l'initialisation d'une instance, des modèles de configuration
sont fournis. Ces modèles sont situés dans le dossiers ``{TEMPLATES}`` :

 * :file:`/usr/share/waarp/templates/` pour une installation via les rpms
 * :file:`share/templates` à partir du dossier d'extraction pour les archives 
   autoportantes

Pour créer une instance, copiez le dossier de modèle dans  le dossier 
correspondant à la configuration de l'instance :


.. code-block:: bash

  cp -r {TEMPLATES} {CONFDIR}/$HOSTID
  
Ces fichiers sont préconfigurés pour une utilisation standard. Une partie de la
configuration est dépendante de l'instance (identifiants, dossiers, etc.).
Dans les fichiers XML de modèle, la chaîne ``{{app_name}}`` doit être remplacée
par l'identifiant de l'instance :

.. code-block:: sh

   for f in {CONFDIR}/$HOSTID/*.xml; do   
      sed -i -r "s|{{app_name}}|$HOSTID|g" $f
   done


Configuration de la base de données
-----------------------------------

Par défaut, la nouvelle instance est configurée pour utiliser la base de données
embarquée H2. Pour utiliser une autre base de données, il faut la configurer
dans les fichier :file:`{CONFDIR}/$HOSTID/server.xml` et 
:file:`{CONFDIR}/$HOSTID/client.xml`.

La configuration de la base de données se trouve dans le bloc XML 
``<db>...</db>`` :

.. code-block:: xml

   <db>
      <dbdriver>postgresql</dbdriver>
      <dbserver>jdbc:postgresql://localhost/waarp_r66</dbserver>
      <dbuser>waarp</dbuser>
      <dbpasswd>waarp</dbpasswd>
      <dbcheck>false</dbcheck>
   </db>


Initialisation de la base de données
------------------------------------

Pour initialiser la base de données, exécuter la commande suivante :

.. code-block:: sh

   # Avec les packages :  
   waarp-r66client $HOSTID initdb

   # Avec les archives :
   ./bin/waarp-r66client.sh $HOSTID initdb

Démarrage du serveur
--------------------

.. todo:: à ce stade-là, il manque les données d'authentification

Si l'instance configurée est un serveur, vous pouvez mintenant le démarrer. 

.. code-block:: sh

   # Avec les packages :  
   waarp-r66server $HOSTID start

   # Avec les archives :
   ./bin/waarp-r66server.sh $HOSTID start




Création d'une instance Windows
===============================

Création de la configuration
----------------------------

Pour facililter l'initialisation d'une instance, des modèles de configuration
sont fournis. Ces modèles sont situés dans le dossier 
:file:`share\\templates` à partir du dossier d'extraction.

Pour créer une instance, copiez le dossier de modèle dans le dossier 
correspondant à la configuration de l'instance :


.. code-block:: bat

  xcopy /S share\templates {CONFDIR}\%$HOSTID%
  
Ces fichiers sont préconfigurés pour une utilisation standard. Une partie de la
configuration est dépendante de l'instance (identifiants, dossiers, etc.).
Dans les fichiers XML de modèle, la chaîne ``{{app_name}}`` doit être remplacée
par l'identifiant de l'instance %$HOSTID%.


Configuration de la base de données
-----------------------------------

Par défaut, la nouvelle instance est configurée pour utiliser la base de données
embarquée H2. Pour utiliser une autre base de données, il faut la configurer
dans les fichier :file:`{CONFDIR}\\HOSTID\\server.xml` et 
:file:`{CONFDIR}\\HOSTID\\client.xml`.

La configuration de la base de données se trouve dans le bloc XML 
``<db>...</db>`` :

.. code-block:: xml

   <db>
      <dbdriver>postgresql</dbdriver>
      <dbserver>jdbc:postgresql://localhost/waarp_r66</dbserver>
      <dbuser>waarp</dbuser>
      <dbpasswd>waarp</dbpasswd>
      <dbcheck>false</dbcheck>
   </db>


Initialisation de la base de données
------------------------------------

Pour initialiser la base de données, exécuter la commande suivante :

.. code-block:: bat

   bin\waarp-r66server.bat %HOSTID% initdb

Démarrage du serveur
--------------------

Si l'instance configurée est un serveur, vous pouves mintenant le démarrer. 

Pour une installation avec les archives, la commande est :

.. code-block:: bat

   bin\waarp-r66server.bat %HOSTID% start
