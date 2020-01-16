#####################
Waarp R66 FileWatcher
#####################


Introduction
============

Le filewatcher est un mode particulier du client Waarp R66 : une fois démarré,
celui-ci surveille un ou plusieurs répertoires et envoit tous les fichiers qui y
sont déposés selon les données paramétrées (notamment le destinataire et la
règle de transfert).

Un même filewatcher peut surveiller plusieurs répertoires ou arborescences et
envoyer les fichiers à un ou plusieurs destinataires.

Paramétrage
===========

Principe
--------

Le filewatcher étant un client, il prend sa configuration dans le fichier de
configuration du client :file:`client.xml` de l'instance concernée. Selon la
méthode d'installation et le système d'exploitation cible, celui-ci peut se
trouver aux emplacements suivants :

- :file:`etc/conf.d/{HOSTID}/client.xml`
- :file:`/etc/conf.d/{HOSTID}/client.xml`
- :file:`etc\\conf.d\\{HOSTID}\\client.xml`


Configuration minimale
----------------------

Pour ajouter un filewatcher à un client Waarp R66, il suffit d'ajouter un bloc
XML ``<spooleddaemon>`` à son fichier de configuration à la fin du fichier,
juste avant la balise ``</config>`` :

.. code-block:: xml

   <config>

     <!-- ... -->

     <spooleddaemon>
       <stopfile>data/HOSTID/log/filewatcher.stop</stopfile>
     </spooleddaemon>
   </config>

Le fichier indiqué dans la balise ``<stopfile>`` permet d'arrêter le
filewatcher. Une fois lancé, et tant que ce fichier n'existe pas, le
filewatcher fonctionne. dès que le fichier est créé, le filewatcher s'arrête.

La configuration individuelle des dossiers à surveiller ainsi que les paramètres
d'envoi des fichiers qui y sont déposés sont définis dans des blocs XML
``<spooled>`` ajoutés à ``<spooleddaemon>``.

A minima, un bloc ``<spooled>`` doit contenir :

- un identifiant défini par l'administrateur dans une balise ``<name>``
- un serveur r66 destinataire dans une balise ``<to>``
- la règle de transfert à utiliser dans une balise ``<rule>``
- un dossier à suirveiller dans une balise ``<directory>``
- un fichier de statut dans une balise ``<statusfile>``. Ce fichier est utilisé
  pour enregistrer les informations sur les fichiers déposés dans le dossier
  surveillé.

Par exemple :

.. code-block:: xml

   <config>

     <!-- ... -->

     <spooleddaemon>
       <stopfile>data/HOSTID/log/filewatcher.stop</stopfile>
       <spooled>
         <name>identifiant</name>
         <to>host1</to>
         <rule>rulespooled</rule>
         <statusfile>data/HOSTID/log/status_identifiant.json</statusfile>
         <directory>data/HOSTID/spooled/out</directory>
       </spooled>
     </spooleddaemon>
   </config>


Configuration avancée
---------------------

Surveiller plusieurs dossiers
"""""""""""""""""""""""""""""

Pour surveiller plusieurs dossiers avec des paramètres d'envoi (destinataires
et/ou règle de transfert) différents, il est possible de définir plusieurs blocs
``<spooled>``. Par exemple :

.. code-block:: xml

   <config>

     <!-- ... -->

     <spooleddaemon>
       <stopfile>data/HOSTID/log/filewatcher.stop</stopfile>
       <spooled>
         <name>identifiant</name>
         <to>host1</to>
         <rule>rulespooled</rule>
         <statusfile>data/HOSTID/log/status_identifiant.json</statusfile>
         <directory>data/HOSTID/spooled/out</directory>
       </spooled>

       <spooled>
         <name>identifiant2</name>
         <to>host2</to>
         <rule>rulespooled2</rule>
         <statusfile>data/HOSTID/log/status_identifiant2json</statusfile>
         <directory>data/HOSTID/spooled/out2/directory>
       </spooled>
     </spooleddaemon>
   </config>

Il est également possible, pour les mêmes paramètres d'envois, de surveiller
plusieurs dossiers en spécifiant plusieurs balises ``<directory>`` :

.. code-block:: xml

   <config>

     <!-- ... -->

     <spooleddaemon>
       <stopfile>data/HOSTID/log/filewatcher.stop</stopfile>
       <spooled>
         <name>identifiant</name>
         <to>host1</to>
         <rule>rulespooled</rule>
         <statusfile>data/HOSTID/log/status_identifiant.json</statusfile>
         <directory>data/HOSTID/spooled/out</directory>
         <directory>data/HOSTID/spooled/out2/directory>
       </spooled>
     </spooleddaemon>
   </config>


Envoi des fichiers à plusieurs destinataires
""""""""""""""""""""""""""""""""""""""""""""

Pour envoyer les fichiers déposés dans un dossier surveillé à plusieurs
destinataires, il est possible de spécifier plusieurs balises ``<to>`` :

.. code-block:: xml

   <config>

     <!-- ... -->

     <spooleddaemon>
       <stopfile>data/HOSTID/log/filewatcher.stop</stopfile>
       <spooled>
         <name>identifiant</name>
         <to>host1</to>
         <to>host2</to>
         <rule>rulespooled</rule>
         <statusfile>data/HOSTID/log/status_identifiant.json</statusfile>
         <directory>data/HOSTID/spooled/out</directory>
       </spooled>
     </spooleddaemon>
   </config>

Autres directives de configuration
""""""""""""""""""""""""""""""""""

Dans les blocs XML ``<spooled>``, il est également possible d'utiliser les
balises suivantes :

``<info>``
   Métadonnées envoyées avec le fichier durant le transfert (corresponfd à
   l'argument ``-info`` de la commande d'envoi.

``<regex>``
   Une espression régulière de filtrage des fichiers à prendre
   en compte (permet d'exclure des fichiers des transferts).

   .. versionchanged:: 3.1.0

      l'expresion regulière permet de filtrer le chemin complet du fichier et
      non plus le nom du fichier seulement

``<recursive>``
   Si récursif, les sous dossiers aussi sont surveillés.

``<elapse>``
   L'intervalle entre 2 scan du dossier (en ms).

``<submit>``
   Si submit est True, les transferts sont asynchrones. sinon, ils sont directs.

``<parallel>``
   Si submit est false, c'est-à-dire si les transferts sont gérés directement
   par le file watcher, les transferts peuvent être faits en parallèle ou
   séquentiellement.

``<limitParallel>``
   Si les transferts doivent être faits en parallèle, le nombre maximal de
   transferts simultanés.

``<waarp>``
   L'historique de transfert peut être envoyé au serveur Waarp R66 désigné
   ci-dessous.  ceci permet la consultation de l'historique du filewatcher dans
   l'interface HTTP de monitoring de ce serveur.

   Ceci est facultatif (et inutile si submit vaut true -- les transferts sont
   déjà effectués par un serveur -- ou si les interfaces de monitoring sont
   désactivées).

``<elapseWaarp>``
   Si l'historique doit être envoyé à un serveur Waarp R66, intervalle en ms entre
   deux envois.


``<ignoreAlreadyUsed>``

   Si positionné à vrai, tout fichier déjà traité et non effacé, même s'il est
   modifié, sera ignoré et ne sera donc pas renvoyé pour éviter tout risque de
   collisions quant au contenu transféré. Normalement, cette option devrait être
   activée car la modification d'un fichier non transféré est une erreur
   d'exploitation.

   Cependant, si cette option n'est pas activée ou absente, alors, même si le
   fichier a été pris en compte pour un transfert mais toujours non effectué
   (partenaire injoignable par exemple), alors le nouveau contenu prendra le
   dessus sur le précédent et relancera la procédure de trasfert.

   Par défaut cette option est désactivée car elle ne gène pas l'usage normal.

Exemple complet
---------------

.. code-block:: xml

   <spooleddaemon>
       <stopfile>data/HOSTID/log/filewatcher.stop</stopfile>
       <spooled>
           <name>identifiant</name>
           <to>host1</to>
           <to>host2</to>
           <rule>rulespooled</rule>
           <statusfile>data/HOSTID/log/status_identifiant.json</statusfile>
           <directory>data/HOSTID/spooled/out</directory>
           <directory>data/HOSTID/spooled/out2</directory>
           <regex>.*\.?ar$</regex>
           <recursive>True</recursive>
           <elapse>1000</elapse>
           <submit>False</submit>
           <parallel>True</parallel>
           <limitParallel>0</limitParallel>
           <info>spooled transfer</info>
           <waarp>hostas</waarp>
           <elapseWaarp>5000</elapseWaarp>
           <ignoreAlreadyUsed>False</ignoreAlreadyUsed>
       </spooled>
   </spooleddaemon>


Lancement
=========

Linux
-----

Avec les archives, le filewatcher peut être démarré avec la commande suivante :

.. code-block:: bash

   ./bin/waarp-r66client HOSTID watcher start

Cette commande démarre le filelwatcher au premier plan. On peut l'arrêter en
tapant :kbd:`Control-C`.

Pour le démarrer en tant tâche de fond (service), il faut définir la variable
:envvar:`WAARP_SERVICE` avant :

.. code-block:: bash

   export WAARP_SERVICE=1
   ./bin/waarp-r66client.sh HOSTID watcher start

Le service peut alors être arrêté ou redémarré avec les commandes suivantes :

.. code-block:: bash

   ./bin/waarp-r66client.sh HOSTID watcher stop
   ./bin/waarp-r66client.sh HOSTID watcher restart



Windows
-------

Sous Windows, le filewatcher peut être démarré avec la commande suivante :

.. code-block:: bat

   bin\waarp-r66client.bat HOSTID watcher start

Cette commande démarre le filelwatcher au premier plan. On peut l'arrêter en
tapant :kbd:`Control-C`.

Il est également possible de l'installé en tant que service système avec la
commande :

.. code-block:: bat

   bin\waarp-r66client.bat HOSTID install

On peut ensuite le démarrer, l'arrêter ou le redémarrer avec le gestionnaire de
services système.
