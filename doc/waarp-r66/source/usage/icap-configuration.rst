.. _setup-icap:

Configuration de la tâche ICAP
##############################

.. seealso::

  La documentation de la tâche ``ICAP`` est disponible
  :any:`ici <task-icap>`


Mode opératoire préconisé pour l'installation et la configuration
-----------------------------------------------------------------

La commande ``IcapScanFile`` est disponible
(``java -classpath WaarpR66-3.4.0-jar-with-dependencies.jar org.waarp.icap.IcapScanFile``)
en mode ligne de commande afin de valider le bon fonctionnement avec le serveur ICAP cible.

Prérequis :
"""""""""""

- L'adresse du serveur ICAP et son port (noté hosticap et porticap) ; par défaut le port est 1344 et peut
  être omis
- Connaître le nom du service de scan associé à ce serveur (usuellement un parmi : ``avscan``,
  ``virus_scan``, ``srv-clamav``). S'il est connu, il est possible d'utiliser un modèle pré-enregistré
  (``DEFAULT_MODEL`` ou ``ICAP_AVSCAN`` pour ``avscan``, ``ICAP_CLAMAV`` pour ``srv-clamav``,
  ``ICAP_VIRUS_SCAN`` pour ``virus_scan``).
- D'autres options spécifiques peuvent être nécessaires et seront établies selon les tests à réaliser.

Etape 1 : Permettre la connexion et l'envoi d'un fichier sain (nommé fichier)
"""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""

La commande sera de la forme :

::

   java -classpath WaarpR66-3.4.0-jar-with-dependencies.jar \
    org.waarp.icap.IcapScanFile -file fichier \
    -to hosticap [-port porticap] \
    (-service nom | -model nom) -logger DEBUG

Cette commande va réaliser une requête OPTIONS puis RESPMOD avec le serveur pour le service nommé (ou selon
le modèle choisi) et afficher toutes les étapes en mode DEBUG.

Si le retour est 0 (sans erreur), il n'y a a priori pas d'autres options à positionner pour un cas sain.

Si le retour est en erreur, il faut analyser le code erreur et le log produit.

- code 1: Bad arguments ; Veuillez vérifier vos paramètres car il s'agit d'une erreur dans la ligne de
  commande
- code 2: ICAP protocol error ; Le serveur a mal répondu aux requêtes (le protocole ICAP est sans doute a
  adapter côté client, si possible)
- code 3: Network error ; Il s'agit d'un problème purement réseau (un parefeu peut bloquer le flux par
  exemple)
- code 4: Scan KO ; Soit le serveur ou le service est mal configuré, soit il faut adapter le client au code
  retourné par le serveur
- code 5: Scan KO but post action required in error ; En l'état, sauf si vous avez spécifié une des options
  concernées (``-errorMove path`` | ``-errorDelete`` | ``-sendOnError``), cette erreur ne devrait jamais
  apparaître à cette étape du test

Dans les cas 2 et 4, il faut analyser ce que reçoit le client du serveur ICAP et le cas échéant adapter les
modalités de traitement du client en fonction de ces retours.

Cela peut concerner les tailles limites :

* [``-previewSize size``, défaut aucun] spécifie la taille de Preview à utiliser (défaut négociée)
* [``-blockSize size``, défaut 8192] spécifie la taille en émission à utiliser (défaut 8192)
* [``-receiveSize size``, défaut 65536] spécifie la taille en réception à utiliser (défaut
  65536)
* [``-maxSize size``, défaut MAX_INTEGER] spécifie la taille Max d'un fichier à utiliser (défaut
  MAX_INTEGER)

Cela peut concerner les valeurs de statut et les retours associés :

* [``-keyPreview key -stringPreview string``, défaut aucun] spécifie la clef et la chaîne associée pour
  Options à valider (défaut aucun)
* [``-key204 key -string204 string``, défaut aucun] spécifie la clef et la chaîne associée pour 204 ICAP
  à valider (défaut aucun)
* [``-key200 key -string200 string``, défaut aucun] spécifie la clef et la chaîne associée pour 200 ICAP
  à valider (défaut aucun)
* [``-stringHttp string``, défaut aucun] spécifie la chaîne pour HTTP 200 ICAP à valider
  (défaut aucun)

Pour rappel, la norme ICAP indique :

- OPTIONS doit retourner un code ``200`` et ``Preview`` dans les balises :

  - si le Preview est indisponible, vous pouvez forcer sa taille avec l'option
    ``-previewSize`` ce qui évitera les requêtes OPTIONS pour la suite
  - si le code est ``404``, le service n'est pas connu. Il vous faut récupérer
    ce nom de service auprès de vos services informatiques (ICAP ne permet pas
    de lister les services disponibles).

- RESPMOD doit retourner :

  - un code ``204`` si le fichier est validé
  - un code ``200`` si le fichier est invalide

    - Si le fichier est sain mais qu'un code 200 est retourné, il est possible
      d'analyser les clefs/valeurs ICAP (``-key200``/``-string200``) ou le
      contenu de la partie HTTP de la réponse (``-stringHttp``)


Etape 2 : Permettre la connexion et l'envoi d'un fichier malsain de test
""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""

Si vous ne disposez pas d'un tel fichier, vous pouvez spécifier un test interne basé sur EICAR en précisant
l'option ``-file EICARTEST``.

::

   java -classpath \
    WaarpR66-3.4.0-jar-with-dependencies.jar \
    org.waarp.icap.IcapScanFile -file EICARTEST \
    -to hosticap [-port porticap] \
    (-service nom | -model nom) -logger DEBUG

Veillez à conserver les options que vous avez introduites dans l'étape 1, y compris celles correctives.

Normalement, le retour devrait être de la valeur 4 (Scan KO).

Si tel n'est pas le cas, il faut à nouveau analyser les logs et le retour.

Par exemple, si le retour est 204, il est possible qu'une clef ICAP dise autrement
(``-key204``/``-string204``).

Si le code était déjà 200 pour un fichier sain, il est possible qu'il faille modifier les options
précédemment mises en place pour fonctionner tant avec un fichier sain qu'un fichier malsain
(``-key200``/``-string200`` et/ou ``-stringHttp``).


Etape 3 : Configurer une règle pour utiliser ces paramètres
"""""""""""""""""""""""""""""""""""""""""""""""""""""""""""

Il s'agit maintenant de créer ou modifier une règle pour y ajouter une tâche ICAP avec les paramètres
valides pour votre configuration.

Par exemple :


.. code-block:: xml

  <task>
    <type>ICAP</type>
    <path>-file #TRUEFULLPATH# -to hostname -model ICAP_AVSCAN
    -sendOnError -ignoreNetworkError -- -file #TRUEFULLPATH# -to
    requestedHost -rule rule -copyinfo -info FILE INFECTED</path>
    <delay>10000</delay>
  </task>

Puis de tester, en mode DEBUG, l'exécution de cette règle suite à un transfert l'utilisant.

.. seealso::

  * Documentation de la :any:`tâche ICAP <task-icap>`

Options spécifiques
"""""""""""""""""""

Ces options sont plus spécifiques au traitement comme tâche dans R66. Elles permettent de gérer les cas
d'erreurs, en assurant ce que devient le fichier (déplacé, effacer ou renvoyer vers un autre serveur) ou en
ignorant des comportements réseaux instables (sur une erreur réseau) ou en ignorant les trop gros fichiers.

* [``-errorMove path`` | ``-errorDelete`` | ``-sendOnError``] spécifie l'action
  en cas de scan erronné : un répertoire de quarantaine, l'effacement du
  fichier, la retransmission (R66) vers un autre partenaire (mutuellement
  exclusif) (défaut aucun)
* [``-ignoreNetworkError``] spécifie que sur une erreur réseau, le fichier sera
  considéré comme OK
* [``-ignoreTooBigFileError``] spécifie que sur une erreur de fichier trop
  grand, le fichier sera considéré comme OK

