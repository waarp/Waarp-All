############
Type de Task
############

Format général d'une tâche
--------------------------

Une tâche est définie selon un format unifié XML :

.. code-block:: xml

  <tasks>
   <task>
    <type>NAME</type>
    <path>path</path>
    <delay>x</delay>
    <rank>n</rank>
   </task>
  </tasks>

- ``Type`` est l'identifiant du type de tâche à exécuter (les types sont présentés ci-après.

- ``Path`` est un argument fixé par la règle et dont des remplacements de mots clefs sont opérés :

  - ``#TRUEFULLPATH#`` : Chemin complet du fichier courant
  - ``#TRUEFILENAME#`` : Nom du fichier courant (hors chemin) (différent côté réception)
  - ``#ORIGINALFULLPATH#`` : Chemin complet du fichier d'origine (avant changement côté réception)
  - ``#ORIGINALFILENAME#`` : Nom du fichier d'origine (avant changement côté réception)
  - ``#FILESIZE#`` : Taille du fichier s'il existe
  - ``#INPATH#`` : Chemin du dossier de réception
  - ``#OUTPATH#`` : Chemin du dossier d'émission
  - ``#WORKPATH#`` : Chemin du dossier de travail
  - ``#ARCHPATH#`` : Chemin du dossier d'archive
  - ``#HOMEPATH#`` : Chemin du dossier du répertoire "racine" de Waarp
  - ``#RULE#`` : Règle utilisé pour le transfert
  - ``#DATE#`` : Date courante au format yyyyMMdd
  - ``#HOUR#`` : Heure courante au format HHmmss
  - ``#REMOTEHOST#`` : Nom DNS du partenaire
  - ``#REMOTEHOSTIP#`` : IP du partenaire
  - ``#LOCALHOST#`` : Nom DNS local du serveur Waarp
  - ``#LOCALHOSTIP#`` : IP du serveur Waarp
  - ``#TRANSFERID#`` : Identifiant de transfert
  - ``#REQUESTERHOST#`` : Nom du partenaire initiateur du transfert
  - ``#REQUESTEDHOST#`` : Nom du partenaire recevant la demande de transfert
  - ``#FULLTRANSFERID#`` : Identifiant complet du transfert comme ``TRANSFERID_REQUESTERHOST_REQUESTEDHOST``
  - ``#RANKTRANSFER#`` : Rang du bloc courant ou final du fichier transféré
  - ``#BLOCKSIZE#`` : Taille du bloc utilisé
  - ``#ERRORMSG#`` : Le message d'erreur courant ou "NoError" si aucune erreur n'a été levée jusqu'à cet
    appel
  - ``#ERRORCODE#`` : Le code erreur courant ou ``-`` (``Unknown``) si aucune erreur n'a été levée jusqu'à
    cet appel
  - ``#ERRORSTRCODE#`` : Le message lié au code d'erreur courant ou ``Unknown`` si aucune erreur n'a été
    levée jusqu'à cet appel
  - ``#NOWAIT#`` : Utilisé par la tâche EXEC pour spécifier que la commande est à exécuter en mode asynchrone,
    sans en attendre le résultat
  - ``#LOCALEXEC#`` : Utilisé par la tâche EXEC pour spécifier que la commande est à exécuter de manière
    distante (pas dans la JVM courante) mais au travers d'un démon ``LocalExec`` (spécifié dans la
    configuration globale

Par exemple, un ``Path`` défini comme :
``some #DATE# some2 #TRANSFERID# some3 #REMOTEHOST# some4``
donnera
``some 20130529 some2 123456789123 some3 remotehostid some4``

- ``Delay`` est généralement le délai (si précisé) maximum pour l'exécution d'une tâche avant qu'elle tombe
  en erreur pour dépassement de délai.
- ``Rank`` est optionnel et permet d'indiquer un rang d'exécution des tâches, laissant une souplesse sur
  l'ordre d'écriture en période de tests. Il n'est pas conseillé de l'utiliser par défaut hormis pour les
  tests et validation de plusieurs tâches.

De plus, une tâche utilisera les arguments de transfert eux-même (``Transfer Information``) pour déduire
les paramètres finaux en utilisant la fonction
``String.format(path règle, info transfert.éclaté en sous chaînes via le séparateur " ")``

L'argument de transfert (spécifié par ``-info`` dans les commandes de transferts) ou le ``path`` peuvent
contenir une MAP au format JSON qui intègre des éléments utiles aux tâches ou au fonctionnement de Waarp
(comme le ``DIGEST``, le ``RESCHDEDULE``, l'option ``follow``).

Ceci permet de rendre les arguments très adaptatifs.

Par exemple :

- si la règle définie ``Path`` comme ``some %s some2 %d some3 %s some4``
- et si les informations de transferts sont ``info1 1 info2``
- Le résultat sera pour cette tâche : ``some info1 some2 1 some3 info2 some4``



Tâches informatives
-------------------

LOG
"""

Cette tâche loggue ou écrit dans un fichier externe des informations :

- si ``delay`` est 0, aucune sortie ne sera effectuée
- si ``delay`` est 1, les informations seront envoyées vers un log
- si ``delay`` est 2, les informations seront envoyées dans un fichier (le dernier argument sera le chemin 
  complet du fichier de sortie)
- si ``delay`` est 3, les informations seront envoyées dans le log et dans un fichier (le dernier argument 
  sera le chemin complet du fichier de sortie)

Si le premier mot de ce log est un parmi ``debug``, ``info``, ``warn`` ou ``error``, ce sera le niveau du 
log utilisé. 

Exemple:

.. code-block:: xml

  <task>
    <type>LOG</type>
    <path>warn information /path/logfile</path>
    <delay>2</delay>
  </task>

Ceci logguera un log "WARN" dans le fichier ``/path/logfile`` sans trace dans les logs usuels.

SNMP
""""

Cette tâche émet un trap SNMP :

- si ``delay`` est 0, un trap SNMP warning/info est envoyé avec le champ info et le transfer ID
- si ``delay`` est 1, un trap SNMP/info avec toutes les informations de transfert sont envoyées

Si le premier mot de ce log est un parmi ``debug``, ``info``, ``warn`` ou ``error``, ce sera le niveau du
log utilisé.

Exemple:

.. code-block:: xml

  <task>
    <type>SNMP</type>
    <path>information</path>
    <delay>0</delay>
  </task>

Ceci enverra un trap SNMP/info contenant ``information`` et le TransferID.

Tâches agissant sur l'emplacement du fichier
--------------------------------------------

COPY
""""

Copie le fichier au chemin désigné comme argument sans renommer le fichier (même nom de base). Le chemin
obtenu sera un chemin absolu (et non un chemin relatif).

- ``Delay`` et Transfer Information sont ignorés.
- Le fichier n'est pas marqué comme déplacé.

Exemple:

.. code-block:: xml

  <task>
    <type>COPY</type>
    <path>/newpath/</path>
    <delay/>
  </task>

Cela copiera le fichier courant vers ``/newpath/`` en tant que ``/newpath/currentfilename``. Le fichier
courant reste le même (inchangé).

COPYRENAME
""""""""""

Copie le fichier au chemin désigné comme argument en renommant le fichier. Le chemin
obtenu sera un chemin absolu (et non un chemin relatif).

- Une fois le ``Path`` transformé selon les remplacements dynamiques, il est utilisé comme
  ``String Format`` avec le ``Transfer Information`` utilisé en entrée (``String.format(Path,Info)``).
  Le chemin obtenu doit être un chemin absolu.
- ``Delay`` est ignoré.
- Le fichier n'est pas marqué comme déplacé.

Exemple:

.. code-block:: xml

  <task>
    <type>COPYRENAME</type>
    <path>/newpath/newfilename_%s_#TRANSFERID#</path>
    <delay/>
  </task>

Si le ``Transfer Information`` est ``myinfoFromTransfer``, cela copiera le fichier dans un nouveau
fichier nommé ``/newpath/newfilename_myinfoFromTransfer_transferid`` où ``transferid`` sera remplacé par
un identifiant unique (comme 123456789). Le fichier courant reste le même (inchangé).

MOVE
""""

Déplace le fichier au chemin désigné comme argument sans renommer le fichier (même nom de base). Le chemin
obtenu sera un chemin absolu (et non un chemin relatif).

- Une fois le ``Path`` transformé selon les remplacements dynamiques, il est utilisé comme
  ``String Format`` avec le ``Transfer Information`` utilisé en entrée (``String.format(Path,Info)``).
  Le chemin obtenu doit être un chemin absolu.
- ``Delay`` est ignoré.
- Le fichier est marqué comme déplacé.

Exemple:

.. code-block:: xml

  <task>
    <type>MOVE</type>
    <path>/newpath/</path>
    <delay/>
  </task>

Le fichier sera déplacé (non copié) dans le répertoire ``/newpath/``. Le fichier courant est maintenant
celui déplacé.

MOVERENAME
""""""""""

Déplace le fichier au chemin désigné comme argument en renommant le fichier. Le chemin
obtenu sera un chemin absolu (et non un chemin relatif).

- Une fois le ``Path`` transformé selon les remplacements dynamiques, il est utilisé comme
  ``String Format`` avec le ``Transfer Information`` utilisé en entrée (``String.format(Path,Info)``).
  Le chemin obtenu doit être un chemin absolu.
- ``Delay`` est ignoré.
- Le fichier est marqué comme déplacé.

Exemple:

.. code-block:: xml

  <task>
    <type>MOVERENAME</type>
    <path>/newpath/newfilename</path>
    <delay/>
  </task>

Le fichier sera déplacé (non copié) dans le répertoire ``/newpath/`` avec comme nouveau nom
``/newpath/newfilename``. Le fichier courant est maintenant celui déplacé.

LINKRENAME
""""""""""

Crée un lien vers le fichier courant et pointe dessus.

- Le lien est d'abord tenté en mode "hard link", puis "soft link" et si ce n'est pas possible (non
  supporté par le système de fichiers), il crée une copie avec le nouveau nom.
- Une fois le ``Path`` transformé selon les remplacements dynamiques, il est utilisé comme
  ``String Format`` avec le ``Transfer Information`` utilisé en entrée (``String.format(Path,Info)``).
  Le chemin obtenu doit être un chemin absolu.
- ``Delay`` est ignoré.
- Le fichier est marqué comme déplacé.

Exemple:

.. code-block:: xml

  <task>
    <type>LINKRENAME</type>
    <path>/newpath/filenamelink</path>
    <delay/>
  </task>

Le fichier sera un lien dans le répertoire ``/newpath/`` avec pour nom ``filenamelink`` (ou une copie si ce
n'est pas possible).

RENAME
""""""

Renomme le fichier au chemin désigné comme argument. Le chemin
obtenu sera un chemin absolu (et non un chemin relatif).

- Une fois le ``Path`` transformé selon les remplacements dynamiques, il est utilisé comme
  ``String Format`` avec le ``Transfer Information`` utilisé en entrée (``String.format(Path,Info)``).
  Le chemin obtenu doit être un chemin absolu.
- ``Delay`` est ignoré.
- Le fichier est marqué comme déplacé.

Exemple:

.. code-block:: xml

  <task>
    <type>RENAME</type>
    <path>/newpath/newfilename</path>
    <delay/>
  </task>

Le fichier sera déplacé avec le nouveau nom spécifié. Le fichier est marqué comme déplacé.

DELETE
""""""

Cette tâche efface le fichier courant.

- Le fichier courant n'est plus valide.
- Aucun autre argument n'est pris en compte.

Exemple:

.. code-block:: xml

  <task>
    <type>DELETE</type>
    <path/>
    <delay/>
  </task>

Le fichier courant est effacé. En conséquence, plus aucune action ne peut être opérée sur le
fichier. Note : si le fichier ne peut pas être effacé, un Warning sera levé.

VALIDFILEPATH
"""""""""""""

Teste si le fichier courant est sous l'un des dossiers obtenus depuis le ``Path`` ou les ``Transfer
Information``.

- Une fois le ``Path`` transformé selon les remplacements dynamiques, il est utilisé comme
  ``String Format`` avec le ``Transfer Information`` utilisé en entrée (``String.format(Path,Info)``).
- Le résultat devra être : ``path1 path2 ...`` où chaque chemin est séparé par un "blanc".
- Si ``Delay`` n'est pas 0, un log sera produit.
- Le fichier n'est pas marqué comme déplacé.

Exemple:

.. code-block:: xml

  <task>
    <type>VALIDFILEPATH</type>
    <path>/path1/ /path2/</path>
    <delay>1</delay>
  </task>

Ceci vérifiera si le fichier courant est dans un des dossiers spécifiés, ici ``/path1`` ou ``/path2``.
Et il fera une sortie log pour enregistrer le résultat de cette vérification.

Tâches agissant sur le fichier
------------------------------

TAR
"""

Crée un TAR depuis les arguments comme source et destination ou un UNTAR des fichiers depuis une archive TAR.

- Une fois le ``Path`` transformé selon les remplacements dynamiques, il est utilisé comme
  ``String Format`` avec le ``Transfer Information`` utilisé en entrée (``String.format(Path,Info)``).
- Si ``delay`` est ``1``, l'archive tar indiquée en premier argument est
  **extraite** dans le dossier indiqué en second argument (le ``path``
  ``archiveFile destDir`` équivaut à la commande
  ``tar xf archiveFile -C destDir``)
- Si ``delay`` est ``2``, l'archive tar indiquée en premier argument est
  **crée** avec le contenu du dossier indiqué en second argument (le ``path``
  ``archiveFile sourceDir`` équivaut à la commande
  ``tar cf archiveFile sourceDir``)
- Si ``delay`` est ``3``, l'archive tar indiquée en premier argument est
  **crée** avec les fichiers indiqués dans les arguments suivants (le
  ``path`` ``archiveFile sourceFile1 sourceFile2`` équivaut à la commande
  ``tar cf archiveFile sourceFile1 sourceFile2``)
- Le fichier n'est pas marqué comme déplacé.

Exemple:

.. code-block:: xml

  <task>
    <type>TAR</type>
    <path>/path/sourcetarfile /path/targetdirectory/</path>
    <delay>1</delay>
  </task>

Ceci déclenchera un ``UNTAR`` depuis l'archive TAR ``/path/sourcetarfile``
vers le dossier ``/path/targetdirectory``. Le fichier n'est pas marqué comme
déplacé.

ZIP
"""

Crée un ZIP depuis les arguments comme source et destination ou un UNZIP des fichiers depuis une archive ZIP.

- Une fois le ``Path`` transformé selon les remplacements dynamiques, il est utilisé comme ``String
  Format`` avec le ``Transfer Information`` utilisé en entrée (``String.format(Path,Info)``).
- Si ``delay`` est ``1``, l'archive zip indiquée en premier argument est
  **extraite** dans le dossier indiqué en second argument (le ``path``
  ``archiveFile destDir`` équivaut à la commande
  ``unzip archiveFile -d destDir``)
- Si ``delay`` est ``2``, l'archive zip indiquée en premier argument est
  **crée** avec le contenu du dossier indiqué en second argument (le ``path``
  ``archiveFile sourceDir`` équivaut à la commande
  ``zip -r archiveFile sourceDir``)
- Si ``delay`` est ``3``, l'archive zip indiquée en premier argument est
  **crée** avec les fichiers indiqués dans les arguments suivants (le
  ``path`` ``archiveFile sourceFile1 sourceFile2`` équivaut à la commande
  ``zip archiveFile sourceFile1 sourceFile2``)
- Le fichier n'est pas marqué comme déplacé.

Exemple:

.. code-block:: xml

  <task>
    <type>ZIP</type>
    <path>/path/sourcetarfile /path/targetdirectory/</path>
    <delay>1</delay>
  </task>

Ceci déclenchera un ``UNZIP`` depuis l'archive ZIP ``/path/sourcetarfile``
vers le dossier ``/path/targetdirectory``. Le fichier n'est pas marqué comme
déplacé.

TRANSCODE
"""""""""

Permet de transcoder un fichier d'un ensemble de codage vers un autre.

Une fois le ``Path`` transformé selon les remplacements dynamiques, il est utilisé comme
``String Format`` avec le ``Transfer Information`` utilisé en entrée (``String.format(Path,Info)``).

- ``-from fromCharset``
- ``-to toCharset``
- ``-newfile filename`` argument optionnel : si non utilisé, ce sera le nom du fichier courant plus
  ``.extension`` (usuellement ``transcode``) ; si utilisé, aucune extension ne sera ajoutée
- ``-extension extension`` argument optionnel : si non utilisé, le fichier produit sera
  ``filename.transcode``
- ``-dos2unix`` or ``-unix2dos`` argument optionnel, mais si présent, ``-from`` et ``-to`` peuvent être
  ignorés ; ceci autorise des actions ``dos2unix``/``unix2dos`` à la fin du transcodage. Cette opération peut
  être réalisée même sans les options ``-from`` ou ``-to``, ce qui signifie que seule cette transformation
  sera appliquée, sans transcodage.

``fromCharset`` et ``toCharset`` sont des chaînes représentant les codages officiels disponibles en Java
dont.

Le fichier n'est pas marqué comme déplacé.

Exemple:

.. code-block:: xml

  <task>
    <type>TRANSCODE</type>
    <path>-from fromCharset -to toCharset -newfile /path/file</path>
    <delay/>
  </task>

Ceci transcodera le fichier courant depuis ``fromCharset`` vers ``toCharset`` et le résultat sera placé
dans le fichier ``/path/file``.
Le fichier n'est pas marqué comme déplacé.


Une méthode en ligne de commande (depuis Waarp Common) permet d'obtenir une liste en html (``-html``), csv
(``-csv``) ou au format texte (``-text``) de tous les codages supportés par votre JVM. Pour l'utiliser,
exécuter la commande suivante :

.. code-block::

  java -cp WaarpCommon-1.2.7.jar \
    org.waarp.common.transcode.CharsetsUtil \
    [-csv | -html | -text ]

Elle peut également être utilisé pour transcoder des fichiers en dehors de R66.

.. code-block::

  java -cp WaarpCommon-1.2.7.jar \
    org.waarp.common.transcode.CharsetsUtil \
    -from fromFilename fromCharset -to toFilename toCharset

Codages supportés
'''''''''''''''''

Parmi les ensembles de codages, les plus connus sont :

- France: IBM297 or IBM01147
- Italy: IBM280 or IBM01144
- UK: IBM285 or IBM01146
- International (Switzerland, Belgium): IBM500 or IBM01148
- Austria/Germany: IBM273 or IBM01141
- Spain and Latin America: IBM284 or IBM01145
- Portugal, Brazil, USA, Canada, Netherlands: IBM037 or IBM01140
- Central and Eastern Europe: IBM870
- Cyrillic: x-IBM1025 (x-IBM1381?)
- Turkey: IBM1026
- Cyrillic Ukraine: x-IBM1123
- Denmark, Norway: IBM277 or IBM01142
- Finland or Sweden: IBM278 or IBM01143
- Greece: x-IBM875 or x-IBM1124

.. seealso::

    * `référence IBM des code pages <http://publib.boulder.ibm.com/infocenter/pcomhelp/v5r9/topic/com.ibm.pcomm.doc/reference/html/hcp_reference.htm>`__


UNZEROED
""""""""

Cette tâche ajoute un octet à un fichier si celui-ci est vide (de taille 0).

Cette tâche sera en erreur si le fichier est de taille 0 mais ne peut pas être "unzeroed". Si le chemin est
non vide, le contenu sera utilisé comme remplissage du le fichier vide. S'il est vide, le caractère "blanc"
sera utilisé.

Une fois le ``Path`` transformé selon les remplacements dynamiques, il est utilisé comme
``String Format`` avec le ``Transfer Information`` utilisé en entrée (``String.format(Path,Info)``).

- Si ``Delay`` est 1, la tâche produira un log de niveau info
- Si ``Delay`` est 2, la tâche produira un log de niveau warn
- Le fichier n'est pas marqué comme déplacé.

Exemple:

.. code-block:: xml

  <task>
    <type>UNZEROED</type>
    <path>optional</path>
    <delay>1</delay>
  </task>

Ceci remplira le fichier courant s'il est vide avec le contenu "optional" et produiera un log de niveau
INFO en l'absence d'erreur, de niveau ERROR en cas d'erreur.

CHKFILE
"""""""

Cette tâche vérifie différentes propriétés relatives au fichier courant en fonction des arguments.

Une fois le ``Path`` transformé selon les remplacements dynamiques, il est utilisé comme
``String Format`` avec le ``Transfer Information`` utilisé en entrée (``String.format(Path,Info)``).

- ``SIZE LT/GT/LTE/GTE/EQ number``

  - vérifie la taille du fichier en fonction d'une limite (plus petit, plus grand, plus petit ou égal, plus
    grand ou égal, égal)

- ``DFCHECK``

  - vérifie que la taille du fichier à recevoir est compatible avec l'espace disponible restant tant sur
    l'espace de travail que sur l'espace final de réception (depuis le contexte)

- Le fichier n'est pas marqué comme déplacé.

Exemple:

.. code-block:: xml

  <task>
    <type>CHKFILE</type>
    <path>SIZE LT 1000000 SIZE GT 1000 DFCHECK</path>
    <delay/>
  </task>

Ceci testera si le fichier est plus petit que 10 MO (base 10), plus grand que 1000 octets et si les
répertoires de travail et de réceptions ont assez d'espace pour y écrire le fichier (taille annoncée par
l'émetteur).

CHMOD
"""""

Cette tâche permet de modifier les droits du fichier (comme la commande ``CHMOD`` sous Unix) avec les
arguments suivants :

- le chemin complet est celui du fichier courant
- Une fois le ``Path`` transformé selon les remplacements dynamiques, il est utilisé comme
  ``String Format`` avec le ``Transfer Information`` utilisé en entrée (``String.format(Path,Info)``).
- les arguments in fine seront de la forme ``[ua][+-=][rwx]`` où de multiples répétitions peuvent être
  spécifiées, séparées par un caractère ``blanc``

  - ``u/a`` signifiant l'utilisateur (Utilisateur système Waarp)/all (tous) (groupe et autre n'existent pas
    en Java),
  - ``+/-/=`` signifiant l'ajout, le retrait ou l'affectation (l'affectation signifie que tous les autres
    droits sont retirés),
  - ``r/w/x`` signifiant Read/Write/Execute (Lecture/Ecriture/Exécution)

- Le fichier n'est pas marqué comme déplacé.

Par exemple :

  - ``u=rwx a=r``
  - ``ua+rw``
  - ``u=rw a-wx``
  - ``a+rw``

Si plusieurs modes sont indiqués, ils seront exécutés en séquence.  Ainsi ``a=r a+w a-r`` donnera ``a=w``.

Exemple:

.. code-block:: xml

  <task>
    <type>CHMOD</type>
    <path>a=r a+w a-r</path>
    <delay/>
  </task>

.. _task-icap:

ICAP
""""

.. versionadded:: 3.4.0

.. seealso::

  Une documentation complète d'installation au regard des interactions avec un serveur ICAP est disponible
  :any:`ici <setup-icap>`

Cette tâche permet l'échange avec un serveur répondant à la norme RFC 3507 dite `ICAP`.
Elle permet de transférer le contenu du fichier vers un service ICAP via une commande
`RESPMOD` et d'obtenir la validation de ce fichier par le service (status `204`).

La liste des arguments est la suivante :

* ``-file filename`` spécifie le chemin du fichier sur lequel opérer (si le nom
  est ``EICARTEST``, un faux virus de test basé sur EICAR test sera envoyé).
* ``-to hostname`` spécifie l'adresse (via DNS ou IP) du serveur ICAP
* [``-port port``, défaut 1344]  spécifie le port à utiliser (défaut 1344)
* ``-service name`` | ``-model name``  spécifie le service ou modèle ICAP à
  utiliser
* [``-previewSize size``, défaut aucun] spécifie la taille de Preview à
  utiliser (défaut négociée)
* [``-blockSize size``, défaut 8192] spécifie la taille en émission à utiliser
  (défaut 8192)
* [``-receiveSize size``, défaut 65536] spécifie la taille en réception à
  utiliser (défaut 65536)
* [``-maxSize size``, défaut MAX_INTEGER] spécifie la taille maxmale d'un
  fichier à utiliser (défaut MAX_INTEGER)
* [``-timeout in_ms``, défaut equiv à 10 min] spécifie la limite de temps à
  utiliser (défaut equiv à 10 min)
* [``-keyPreview key -stringPreview string``, défaut aucun] spécifie la clef et
  la chaîne associée pour Options à valider (défaut aucun)
* [``-key204 key -string204 string``, défaut aucun] spécifie la clef et la
  chaîne associée pour 204 ICAP à valider (défaut aucun)
* [``-key200 key -string200 string``, défaut aucun] spécifie la clef et la
  chaîne associée pour 200 ICAP à valider (défaut aucun)
* [``-stringHttp string``, défaut aucun] spécifie la chaîne pour HTTP 200 ICAP
  à valider (défaut aucun)
* [``-logger DEBUG|INFO|WARN|ERROR``, défaut aucun] spécifie le niveau de log
  entre ``DEBUG`` | ``INFO`` | ``WARN`` | ``ERROR`` (défaut ``WARN``)
* [``-errorMove path`` | ``-errorDelete`` | ``-sendOnError``] spécifie l'action
  en cas de scan erronné : un répertoire de quarantaine, l'effacement du
  fichier, la retransmission (R66) vers un autre partenaire (mutuellement
  exclusif) (défaut aucun)
* [``-ignoreNetworkError``] spécifie que sur une erreur réseau, le fichier sera
  considéré comme OK
* [``-ignoreTooBigFileError``] spécifie que sur une erreur de fichier trop
  grand, le fichier sera considéré comme OK


Si une commande R66 de retransfert est demandée (``-sendOnError``), la dernière option pour ICAP devra être
suivie de ``--`` avant de poursuivre sur les options usuelles pour la commande ``TRANSFER``.


Exemple 1:

.. code-block:: xml

  <task>
    <type>ICAP</type>
    <path>-file #TRUEFULLPATH# -to hostname -service name
    -previewSize size -blockSize size -receiveSize size
    -maxSize size -timeout in_ms
    -keyPreview key -stringPreview string
    -key204 key -string204 string
    -key200 key -string200 string
    -stringHttp string -logger WARN -errorDelete
    -ignoreNetworkError</path>
    <delay>10000</delay>
  </task>

Ici, en cas de scan en erreur, le fichier sera effacé.

Exemple 2:

.. code-block:: xml

  <task>
    <type>ICAP</type>
    <path>-file #TRUEFULLPATH# -to hostname -model name
    -previewSize size -blockSize size -receiveSize size
    -maxSize size -timeout in_ms
    -keyPreview key -stringPreview string
    -key204 key -string204 string
    -key200 key -string200 string
    -stringHttp string -logger WARN -errorMove path
    -ignoreNetworkError</path>
    <delay>10000</delay>
  </task>

Ici, en cas de scan en erreur, le fichier sera déplacé dans un autre répertoire.

Exemple 3:

.. code-block:: xml

  <task>
    <type>ICAP</type>
    <path>-file #TRUEFULLPATH# -to hostname -model name
    -previewSize size -blockSize size -receiveSize size
    -maxSize size -timeout in_ms
    -keyPreview key -stringPreview string
    -key204 key -string204 string
    -key200 key -string200 string
    -stringHttp string -logger WARN -sendOnError
    -ignoreNetworkError -- -file #TRUEFULLPATH# -to
    requestedHost -rule rule [-copyinfo]
    [-info information]</path>
    <delay>10000</delay>
  </task>

Ici, en cas de scan en erreur, le fichier sera envoyé vers un autre serveur (l'effacement sera alors pris
en charge par la règle utilisée pour l'envoyer).

Exemple 4:

.. code-block:: xml

  <task>
    <type>ICAP</type>
    <path>-file #TRUEFULLPATH# -to hostname -model ICAP_AVSCAN
    -sendOnError -ignoreNetworkError -ignoreTooBigFileError --
    -file #TRUEFULLPATH# -to requestedHost -rule rule -copyinfo
    -info FILE INFECTED</path>
    <delay>10000</delay>
  </task>

Même cas que l'exemple 3 plus minimaliste et réaliste.



Tâches exécutant un sous-traitement
-----------------------------------

EXEC
""""

Exécute une commande externe en fonction des arguments ``Path`` et ``Transfer Information``.

- Le ``Delay`` est le temps maximum autorisé en millisecondes avant que la tâche ne soit considérée comme
  en time out et donc en erreur.
- Une fois le ``Path`` transformé selon les remplacements dynamiques, il est utilisé comme
  ``String Format`` avec le ``Transfer Information`` utilisé en entrée (``String.format(Path,Info)``).
- Le fichier n'est pas marqué comme déplacé.

La commande externe est supposée se comporter comme suit pour ses valeurs de retour :

- exit 0, pour une exécution correcte
- exit 1, pour une exécution correcte mais avec avertissement
- toute autre valeur pour une exécution en erreur

Exemple:

.. code-block:: xml

  <task>
    <type>EXEC</type>
    <path>/path/command arguments #TRANSFERID# #TRUEFULLPATH# %s</path>
    <delay>10000</delay>
  </task>

En prenant en compte les transformations dynamiques, la commande ``/path/command`` sera exécutée avec les
arguments suivants :
``arguments transferId /path/currentFilename transferInformation``.

EXECMOVE
""""""""

Exécute une commande externe en fonction des arguments ``Path`` et ``Transfer Information``.

- Le ``Delay`` est le temps maximum autorisé en millisecondes avant que la tâche ne soit considérée comme
  en time out et donc en erreur.
- Une fois le ``Path`` transformé selon les remplacements dynamiques, il est utilisé comme
  ``String Format`` avec le ``Transfer Information`` utilisé en entrée (``String.format(Path,Info)``).
- La dernière ligne retournée par la commande externe est interprétée comme le nouveau chemin absolu du
  fichier courant. La commande externe est responsable d'avoir réellement déplacer le fichier vers ce nouvel
  emplacement.
- Le fichier est marqué comme déplacé.

La commande externe est supposée se comporter comme suit pour ses valeurs de retour :

- exit 0, pour une exécution correcte
- exit 1, pour une exécution correcte mais avec avertissement
- toute autre valeur pour une exécution en erreur

Exemple:

.. code-block:: xml

  <task>
    <type>EXECMOVE</type>
    <path>/path/command arguments #TRANSFERID# #TRUEFULLPATH# %s</path>
    <delay>10000</delay>
  </task>


En prenant en compte les transformations dynamiques, la commande ``/path/command`` sera exécutée avec les
arguments suivants :
``arguments transferId /path/currentFilename transferInformation``.
La dernière ligne retournée par la commande externe est interprétée comme le nouveau chemin absolu du
fichier courant.

EXECOUTPUT
""""""""""

Exécute une commande externe en fonction des arguments ``Path`` et ``Transfer Information``.

- Le ``Delay`` est le temps maximum autorisé en millisecondes avant que la tâche ne soit considérée comme
  en time out et donc en erreur.
- Une fois le ``Path`` transformé selon les remplacements dynamiques, il est utilisé comme
  ``String Format`` avec le ``Transfer Information`` utilisé en entrée (``String.format(Path,Info)``).
- Toutes les lignes retournées par la commande externe (sortie standard) sont interprétées comme un
  possible message d'erreur.
- Le fichier n'est pas marqué comme déplacé, sauf en cas d'erreur et si ``NEWFILENAME`` est utilisé comme
  préfixe au nom du fichier).

La commande externe est supposée se comporter comme suit pour ses valeurs de retour :

- exit 0, pour une exécution correcte
- exit 1, pour une exécution correcte mais avec avertissement
- toute autre valeur pour une exécution en erreur et seulement dans ce cas, la sortie standard est
  utilisée comme message d'erreur. Des informations peuvent être retournées au serveur distant avec les
  balises ``#ERRORMSG#`` et ``#ERRORCODE#`` ou ``#ERRORSTRCODE#``, et ``NEWFINALNAME`` si le fichier a
  changé.

Exemple:

.. code-block:: xml

  <task>
    <type>EXECOUTPUT</type>
    <path>/path/command arguments #TRANSFERID# #TRUEFULLPATH# %s</path>
    <delay>10000</delay>
  </task>


En prenant en compte les transformations dynamiques, la commande ``/path/command`` sera exécutée avec les
arguments suivants :
``arguments transferId /path/currentFilename transferInformation``.

La dernière ligne retournée par la commande externe est interprétée comme le nouveau chemin absolu du
fichier courant. Des informations peuvent être retournées au serveur distant avec les
balises ``#ERRORMSG#`` et ``#ERRORCODE#`` ou ``#ERRORSTRCODE#``, et ``NEWFINALNAME`` si le fichier a changé.

EXECJAVA
""""""""

Exécute une classe Java externe en fonction des arguments ``Path`` et ``Transfer Information``.

- Le ``Delay`` est le temps maximum autorisé en millisecondes avant que la tâche ne soit considérée comme
  en time out et donc en erreur.
- Une fois le ``Path`` transformé selon les remplacements dynamiques, il est utilisé comme
  ``String Format`` avec le ``Transfer Information`` utilisé en entrée (``String.format(Path,Info)``).
- Le nom de la classe Java (qui doit implémenter ``R66Runnable`` ou étendre ``AbstractExecJavaTask``, en
  ignorant les méthodes ``validate/finalValidate/invalid`` utilisées uniquement pour les tâches ``Business``)
  est obtenu comme le premier argument. L'allocation est réalisée sous la forme ``new MyClass()``,
  c'est-à-dire un constructeur sans argument.
- Le fichier n'est pas marqué comme déplacé.

Exemple:

.. code-block:: xml

  <task>
    <type>EXECJAVA</type>
    <path>java.class.name #TRANSFERID# #TRUEFULLPATH#</path>
    <delay>10000</delay>
  </task>

Ceci va déclencher l'exécution de la commande nommée ``java.class.name`` avec les arguments suivants :
``arguments transferId /path/currentFilename``.

Eléments additionnels : Usage de la classe ExecJava

Afin de faciliter l'intégration dans des modules applicatifs, Waarp R66
supporte la possibilité de déclencher des classes Java spécifiques de 3
manières (depuis la version 2.3) :

- L'une est au travers de tâches de traitement pré- ou post-transfert, ou en
  cas d'erreur  en utilisant le mot clef EXECJAVA, suivi du nom complet de la
  classe Java qui doit implémenter l'interface ``R66Runnable``.
- Une autre est d'exécuter des commandes spécifiques ``R66Business``, qui sont
  également des implémentations de l'interface ``R66Runnable`` au travers de
  l'extension de ``AbstractExecJavaTask``.
- Enfin, il y a la possibilité d'associer une classe ``Business`` (voir
  ``R66BusinessInterface``) au travers
  d'une "factory" Business (voir ``R66BusinessFactoryInterface``) pour chacun des
  transfer et qui déclenche différentes méthodes lors des étapes de chaque transfert :

  - ``void checkAtStartup(R66Session session)``: lancé au démarrage avant les tâches de pré-tâches
  - ``void checkAfterPreCommand(R66Session session)``: lancé après les pré-tâches mais avant le transfert
  - ``void checkAfterTransfer(R66Session session)``: lancé après le transfer mais avant les
    post-tâches
  - ``void checkAfterPost(R66Session session)``: lancé après les post-tâches et avant la fin de la requête
  - ``void checkAtError(R66Session session)``: lancé si une erreur intervient
  - ``void checkAtChangeFilename(R66Session session)``: lancé si le nom du fichier change durant des
    tâches
  - ``void releaseResources()``: lancé à la toute fin pour nettoyer les possibles ressources utilisées
  - ``String getInfo()`` and ``void setInfo(String info)``: lancés de manière programmatique (code
    métier) pour permettre de positionner une information spéciale (chaîne de caractères) et de la récupérer
    à n'importe quel moment

Notez que la ``R66BusinessFactory`` peut être déclarée dans le fichier XML de configuration du moniteur
dans la balise ``businessfactory`` dans les parties ``server`` ou ``client``, mais est limitée à un
constructeur sans argument.

Notez enfin que pour autoriser des requêtes Business, le droit doit avoir été accordé au partenaire comme
suit dans le fichier de configuration XML :

.. code-block:: xml

      <business><businessid>hostname</businessid>...</business>

Si non positionné, le partenaire ne sera pas autorisé. Pour ``EXECJAVA``, la sécurité est assurée par le
fait que la règle est locale au serveur qui l'ecécute et que la règle peut elle aussi limiter les
partenaires qui peuvent l'utiliser.

RESTART
"""""""

Cette tâche permet de redémarrer un serveur Waarp. Il n'y a aucun argument.

Exemple:

.. code-block:: xml

  <task>
    <type>RESTART</type>
    <path></path>
    <delay>0</delay>
  </task>

L'exemple d'usage le plus fréquent est la mise à jour des binaires ou de la configuration XML du serveur
via un transfert, suivi d'un ``UNTAR`` ou ``UNZIP`` et enfin d'un ``RESTART``.

Tâches exécutant un transfert
-----------------------------

TRANSFER
""""""""

.. versionadded:: 3.4.0
   
   option ``-nofollow``

Soumet un nouveau transfert basé sur des arguments ``Path`` et ``Transfer Information``.

- Une fois le ``Path`` transformé selon les remplacements dynamiques, il est utilisé comme
  ``String Format`` avec le ``Transfer Information`` utilisé en entrée (``String.format(Path,Info)``).
- Les arguments de transferts sont obtenus à partir du ``Path`` transformé.
- Le résultat est considéré comme un ``r66send`` sauf ``-info`` qui doit être le dernier item, et
  ``-copyinfo`` copiera en première position les informations de transferts originales dans les nouvelles, en
  ayant toujours la possibilité d'en ajouter d'autres via ``-info``
- ``Delay`` est ignoré
- Le fichier n'est pas marqué comme déplacé.

Arguments du transfert :

::

  -to <arg>        Spécifie le partenaire distant
  (-id <arg>|      Spécifie l'identifiant du transfert
   (-file <arg>    Spécifie le fichier à opérer
    -rule <arg>))  Spécifie la règle de transfert
  [-block <arg>]   Spécifie la taille du bloc
  [-nofollow]      Spécifie que le trasfert ne devra pas intégrer un "follow" id
  [-md5]           Spécifie qu'un calcul d'empreinte doit être réalisé pour
                    valider le transfert
  [-delay <arg>|   Spécifie le délai comme un temps epoch ou un délai (+arg) en ms
   -start <arg>]   Spécifie la date de démarrage yyyyMMddHHmmss
  [-nolog]         Spécifie de ne rien conserver de ce transfert (en base)
  [-notlogWarn |   Spécifie que le log final est en mode Info si OK
   -logWarn]       Spécifie que le log final est en mode Warn si OK (défaut)
  [-copyinfo]      Spécifie que les informations de transfert seront recopiées
                    intégralement en préposition des nouvelles valeurs
  [-info <arg>)    Spécifie les informations de transfert (en dernière position)


Exemple:

.. code-block:: xml

  <task>
    <type>TRANSFER</type>
    <path>-file #TRUEFULLPATH# -to remotehost
    -rule ruletouse -info transfer Information</path>
    <delay/>
  </task>

Ceci créera une nouvelle requête de transfert (asynchrone) en utilisant le fichier courant
(``#TRUEFULLPATH#``), pour envoyer (ou recevoir selon la règle utilisée)
vers (ou depuis) le partenaire, en utilisant ``transfer Information`` comme argument de transfert.

RESCHEDULE
""""""""""

Replanifie une tâche de transfert en cas d'erreur avec un délai spécifié en millisecondes, si le code
d'erreur est un de
ceux spécifiés et si les intervalles optionnels de dates sont compatibles avec la nouvelle planification.

La balise ``path`` accepte les arguments suivants (les deux premiers sont
obligatoires) :

- ``-delay ms`` spécifie le délai en millisecondes après lequel retenter ce
  transfert
- ``-case errorCode,errorCode,...`` où les "errorCode" sont une liste de codes
  d'erreur pour lesquels la tâche est exécutée. Les codes suivants sont
  disponibles (e nom de l'erreur et le code d'une lettre peuvent petre
  utilisés) :

  - ``ConnectionImpossible(C)``,
  - ``ServerOverloaded(l)``
  - ``BadAuthent(A)``,
  - ``ExternalOp(E)``
  - ``TransferError(T)``
  - ``MD5Error(M)``
  - ``Disconnection(D)``
  - ``RemoteShutdown(r)``
  - ``FinalOp(F)``
  - ``Unimplemented(U)``
  - ``Shutdown(S)``
  - ``RemoteError(R)``
  - ``Internal(I)``
  - ``StoppedTransfer(H)``
  - ``CanceledTransfer(K)``
  - ``Warning(W)``
  - ``Unknown(-)``
  - ``QueryAlreadyFinished(Q)``
  - ``QueryStillRunning(s)``
  - ``NotKnownHost(N)``,
  - ``QueryRemotelyUnknown(u)``
  - ``FileNotFound(f)``
  - ``CommandNotFound(c)``
  - ``PassThroughMode(p)``

- ``-between starttime;endtime``, ``-notbetween starttime;endtime`` permettent
  de définir des plages horaires durant lesquelles les tentatives de transferts
  peuvent ou, respectivement, ne peuvent pas être retentés. Les règles
  suivantes sont utilisées :

  - Ces arguments peuvent être utilisés plusieurs fois et peuvent être mixés ;
  - Ils ont le format suivant : ``Yn:Mn:Dn:Hn:mn:Sn`` où n
    spécifie un nombre pour chaque partie d'une date (optionnelle) comme ``Y``
    = Année, ``M`` = Mois, ``D`` = Jour, ``H`` = Heure, ``m`` = minute, ``s`` =
    seconde ;
  - Le format peut être  ``X+n``, ``X-n``, ``X=n`` ou ``Xn`` où ``X+-n``
    signifie ajouter/soustraire n à la date courante, tandis que ``X=n`` ou
    ``Xn`` signifie une valeur exacte ;
  - Si aucune spécification de temps n'est présente, ce sera la date actuelle ;
  - La date planifiée ne doit pas être dans un des intervalles définis par
    les arguments ``-notbetween`` ;
  - La date planifiée doit être dans un des intervalles définis par les
    arguments ``-between`` ;
  - Si aucun de ces arguments n'est spécifié, la date planifiée sera toujours
    valide.
  - Si ``starttime`` est plus grand que ``endtime``, ``endtime`` prendra la valeur ``starttime`` + 1 jour ;
  - Si ``starttime`` et ``endtime`` sont inférieurs à la date planifiée, ils auront également un décalage d'un
    jour.

- ``-count limit`` sera la limite de retentatives. La valeur limite est prise des
  ``information de transfert`` et non de la règle.

  - Chaque fois que cette fonction est appelée, la valeur limite est remplacée
    par ``newlimit = limit - 1`` dans l'``information de transfert``.
  - Pour assurer la cohérence, la valeur doit être dans ce champ puisque elle
    sera changée statiquement.  Cependant, une valeur doit être positionnée dans
    la règle afin de réinitialiser la valeur lorsque le décompte tombe à 0.
  - Ainsi, dans la règle, ``-count resetlimit`` doit être présent, où
    ``resetlimit`` sera la nouvelle valeur lorsque celle-ci atteindra 0. Si elle
    est manquante la condition ne peut pas être appliquée.

.. important::

  * Notez que si un précédent appel à ``RESCHEDULE`` a été réalisé et courroné
    de succès, les appels suivants seront ignorés.
  * Toutes tâches qui suivent celle-ci seront ignorées et non exécutées si la
    replanification est acceptée. Au contraire, si la replanification est
    refusée, les tâches suivantes seront exécutées normalement.


Exemple:

.. code-block:: xml

  <task>
    <type>RESCHEDULE</type>
    <path>-delay 3600000
    -case ConnectionImpossible,ServerOverloaded,Shutdown
    -notbetween H7:m0:S0;H19:m0:S0
    -notbetween H1:m0:S0;H=3:m0:S0 -count 1</path>
    <delay/>
  </task>

Cet exemple illustre le cas d'une nouvelle tentative d'un transfert tombé en
erreur à cause d'une connexion impossible. La nouvelle tentative sera faite
dans une heure, si l'heure résultante n'est pas comprise 7H du matin et 7H du
soir, ni entre 1H du matin et 3H du matin avec une limite de 3 tentatives
(la valeur ``retry`` sera réinitialisée à 1 en cas de 3 tentatives).

Pour chaque tentative, le compteur sera décrémenté.

FTP
"""

Cette tâche permet de réaliser un transfert synchrone en utilisant FTP. Elle utilise les paramètres suivants :

- ``-file filepath``
- ``-to requestedHost``
- ``-port port``
- ``-user user``
- ``-pwd pwd``
- [``-account account``]
- [``-mode active/passive``]
- [``-ssl no/implicit/explicit``]
- [``-cwd remotepath``]
- [``-digest (crc,md5,sha1)``]
- [``-pre extraCommand1`` avec ',' comme séparateur d'arguments]
- ``-command command`` où ``commande`` est un parmi (``get``, ``put``, ``append``)
- [``-post extraCommand2`` avec ',' comme séparateur d'arguments]

L'orde des commandes sera alors :

1. Connexion au requestHost avec le port.

  Si ``-ssl`` vaut ``implicit``, une
  liaison liaison TLS native est utilisée et l'étape 5 n'est pas exécutée

2. ``USER user``
3. ``PASS pwd``
4. ``ACCT account``, si ``-account` est renseigné
5. ``AUTH TLS``, ``PBSZ 0`` et ``PROT P``, si ``-ssl`` vaut ``explicit``
6. ``PASV``, si ``-mode`` vaut ``passive``
7. ``CWD remotepath``

  En cas d'erreur, le dossier est créé: ``MKD remotepath`` puis ``CWD
  remotepath`` (en ignorant les erreurs)

8. Si ``-pre`` est renseigné, ``extraCommand1`` avec ',' remplacés par ' '

  **note** : n'utilisez pas des commande standards FTP comme ``ACCT``,
  ``PASS``, ``REIN``, ``USER``, ``APPE``, ``STOR``, ``STOU``, ``RETR``,
  ``RMD``, ``RNFR``, ``RNTO``, ``ABOR``, ``CWD``, ``CDUP``, ``MODE``,
  ``PASV``, ``PORT``, ``STRU``, ``TYPE``, ``MDTM``, ``MLSD``, ``MLST``,
  ``SIZE``, ``AUTH``

9. ``BINARY`` (binary format)
10. Transfert des données :

  * Si ``-command`` vaut ``get``, ``RETR filepath.basename``
  * Si ``-command`` vaut ``put`, ``STOR filepath``
  * Si ``-command`` vaut ``append``, ``APPE filepath.basename``

11. Si l'argument ``-digest`` est donné et que le serveur FTP distant est
    compatible avec les commandes ``XCRC``, ``XMD5``, ``XSHA1``, ``FEAT`` (le
    résultat vérifie la présente des options disponibles) ; puis
    ``XCRC``/``XMD5``/``XSHA1`` ``filepath.basename`` ; puis localement il y
    aura la comparaison de ce hash avec le fichier local
12. Si ``-post`` est renseigné, ``extraCommand2`` avec ',' remplacés by ' '

  **note** : n'utilisez pas des commande standards FTP comme ``ACCT``, ``PASS``,
  ``REIN``, ``USER``, ``APPE``, ``STOR``, ``STOU``, ``RETR``, ``RMD``,
  ``RNFR``, ``RNTO``, ``ABOR``, ``CWD``, ``CDUP``, ``MODE``, ``PASV``,
  ``PORT``, ``STRU``, ``TYPE``, ``MDTM``, ``MLSD``, ``MLST``, ``SIZE``,
  ``AUTH``

13. ``QUIT``


Le fichier courant est inchangé et non marqué comme déplacé.

Exemple:

.. code-block:: xml

  <task>
    <type>FTP</type>
    <path>-file /path/file -to remotehost -port port
    -user username -pwd password -command put</path>
    <delay/>
  </task>

Ceci enverra (``put``) le fichier ``/path/file`` au serveur FTP ``remotehost`` sur le port ``port`` en
utilisant les ``username`` et ``password``.

