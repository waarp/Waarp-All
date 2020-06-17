############
``rule.xml``
############

.. todo:: move to new format

.. option:: comment

  *String*

.. option:: idrule

  *nonEmptyString*
  
  ID de la règle
        
.. option:: hostids
  
  List of Host Ids allowed to use this rule. No Host Id means all allowed

  [hostid]
  
.. option:: hostid

    *nonEmptyString*
    
    Hostname des partenaires authorisés à utiliser cette règle

.. code-block:: xml

    <rule>
      ...
      <hostids>
        <hostid>server1</hostid>
        <hostid>server2</hostid>
        ...
      </hostids>
      ...
    <rule>
  
.. option:: mode
  
  *nonNulInteger*
  
  1=SEND 2=RECV 3=SEND+MD5 4=RECV+MD5 5=SENDTHROUGHMODE 6=RECVTHROUGHMODE 7=SENDMD5THROUGHMODE 8=RECVMD5THROUGHMODE

.. option:: recvpath

  *token* (IN)
  
  Dossier de reception
  
.. option:: sendpath
  
  *token* (OUT)

  Dossier d'envoi

.. option:: archivepath

  *token* (ARCH)

  Dossier d'archive

.. option:: workpath

  *token* (WORK)
  
  Dossier de travail

.. option:: rpretasks
  
  .. option:: Tasks 

    [Task]

    List des tâche à exécuter avant le transfert par l'envoyeur
  
.. option:: rposttasks
  
  Tasks 

    [Task]

    List des tâche à exécuter après le transfert par l'envoyeur
  
.. option:: rerrortasks
  
  .. option:: Tasks 

    [Task]

    List des tâche à exécuter en cas d'erreur du transfert par l'envoyeur
  
.. option:: spretasks
  
   .. option:: Tasks 

    [Task]

    List des tâche à exécuter avant le transfert par le receveur
  
.. option:: sposttasks
  
  .. option:: Tasks 

    [Task]

    List des tâche à exécuter après le transfert par le receveur

.. option:: serrortasks
  
  .. option:: Tasks 

    [Task]

    List des tâche à exécuter en cas d'erreur le transfert par le receveur³


Les blocs `<task>` definissent les tâches opérées par les différents acteurs de la règle

  .. option:: type
    
    *nonEmptyString*

    Type de tâche: LOG, SNMP, MOVE, MOVERENAME, COPY, COPYRENAME, LINKRENAME, RENAME, DELETE, VALIDFILEPATH,
    EXEC, EXECMOVE, EXECOUTPUT, EXECJAVA, RESTART,
    TRANSFER, RESCHEDULE, FTP, TAR, ZIP, TRANSCODE, UNZEROED, CHKFILE, CHMOD, ICAP

  .. option:: path 
    
    *nonEmptyString*
    
    Argument -généralement un path- appliqué à la tâche, des substitutions sont possibles
    #TRUEFULLPATH#, #FILESIZE#, #RULE#, #DATE#, #TRANSFERID#, ...&quot;
                
  .. option:: delay
    
    *nonNegInteger*
    
    Delai (ms) maximum pour l'execution de la tâche
