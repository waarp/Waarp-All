/transfers
##########

GET /transfers
**************

  Retourne les transferts monitorés par le moniteur

Réponse
=======

Modèle
------

  ================== =======================
  Champs             Type
  ================== =======================
  DbConfiguration    Array[DbTaskRunner]
  ================== =======================

  *DbTaskRunner*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  GLOBALSTEP         integer
  GLOBALLASTSTEP     integer
  STEP               integer
  RANK               integer
  STEPSTATUS         string
  RETRIEVEMODE       bit
  FILENAME           string
  ISMOVED            bit
  IDRULE             string
  BLOCKSZ            integer
  ORIGINALNAME       string
  FILEINFO           string
  TRANSFERINFO       string
  MODETRANS          integer
  STARTTRANS         timestamp
  STOPTRANS          timestamp
  INFOSTATUS         string
  UPDATEDINFO        integer
  OWNERREQ           string
  REQUESTER          string
  REQUESTED          string
  SPECIALID          int
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    [{
      "GLOBALSTEP" : 1,
      "GLOBALLASTSTEP" :2,
      "STEP" : 1,
      "RANK" : 1,
      "STEPSTATUS" : "PENDING",
      "RETRIEVEMODE" : 1,
      "FILENAME" : "test-file.txt",
      "ISMOVED" : 0,
      "IDRULE" : "2a1",
      "BLOCKSZ" : 32,
      "ORIGINALNAME" : "test-file.txt",
      "FILEINFO" : "LONGVARCHAR",
      "TRANSFERINFO" : "LONGVARCHAR",
      "MODETRANS" : 1,
      "STARTTRANS" : "TIMESTAMP",
      "STOPTRANS" : "TIMESTAMP",
      "INFOSTATUS" : "VARCHAR",
      "UPDATEDINFO" : "INTEGER",
      "OWNERREQ" : "VARCHAR",
      "REQUESTER" : "VARCHAR",
      "REQUESTED" : "VARCHAR",
      "SPECIALID" : "BIGINT"
    }]

Paramètres
==========
  
  =========== =============== ======================================
  Paramètre   Type            Desctiption                           
  =========== =============== ======================================
  LIMIT       integer         Maximum number of transfers returned  
  ORDERBYID   bool            Is the answer sorted by ID            
  STARTID     integer         Lower transfer ID returned            
  STOPID      integer         Higher transfer ID returned           
  IDRULE      string          Name of the rule used                 
  PARTNER     string          Hostname of transfer partner          
  PENDING     bool            Return "Pending" transfer             
  INTRANSFER  bool            Return "Running" transfer             
  INERROR     bool            Return "Failed" transfer              
  DONE        bool            Return "Completed" transfer           
  ALLSTATUS   bool            Return all transfer                   
  STARTTRANS  ISO 8061 or ms  Return transfer after this time       
  STOPSTRANS  ISO 8061 or ms  Return transfer before this time      
  OWNERREQ    string          Owner of the request                  
  =========== =============== ======================================

GET /transfers/:id
******************

  Retourne le transfert spécifié

Réponse
=======

Modèle
------

  *DbTaskRunner*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  GLOBALSTEP         integer
  GLOBALLASTSTEP     integer
  STEP               integer
  RANK               integer
  STEPSTATUS         string
  RETRIEVEMODE       bit
  FILENAME           string
  ISMOVED            bit
  IDRULE             string
  BLOCKSZ            integer
  ORIGINALNAME       string
  FILEINFO           string
  TRANSFERINFO       string
  MODETRANS          integer
  STARTTRANS         timestamp
  STOPTRANS          timestamp
  INFOSTATUS         string
  UPDATEDINFO        integer
  OWNERREQ           string
  REQUESTER          string
  REQUESTED          string
  SPECIALID          int
  ================== ========= ============

Exemple
-------

.. code-block:: json

    {
      "GLOBALSTEP" : 1,
      "GLOBALLASTSTEP" :2,
      "STEP" : 1,
      "RANK" : 1,
      "STEPSTATUS" : "PENDING",
      "RETRIEVEMODE" : 1,
      "FILENAME" : "test-file.txt",
      "ISMOVED" : 0,
      "IDRULE" : "2a1",
      "BLOCKSZ" : 32,
      "ORIGINALNAME" : "test-file.txt",
      "FILEINFO" : "LONGVARCHAR",
      "TRANSFERINFO" : "LONGVARCHAR",
      "MODETRANS" : 1,
      "STARTTRANS" : "TIMESTAMP",
      "STOPTRANS" : "TIMESTAMP",
      "INFOSTATUS" : "VARCHAR",
      "UPDATEDINFO" : "INTEGER",
      "OWNERREQ" : "VARCHAR",
      "REQUESTER" : "VARCHAR",
      "REQUESTED" : "VARCHAR",
      "SPECIALID" : "BIGINT"
    }

Paramètres
==========

================ ========= =========================================== 
Paramètre        Type      Desctiption                                
================ ========= ===========================================
SPECIALID        integer   Special Id as LONG in URI as transfers/id  
REQUESTER        varchar   Partner as requester                       
REQUESTED        varchar   Partner as requested                       
OWNERREQ         varchar   Owner of this request                      
================ ========= ===========================================

POST /transfers
***************

Ajoute un nouveau transfer au moniteur

Réponse
=======

Modèle
------

*DbTaskRunner*

================== ========= ============
Champs             Type      Description
================== ========= ============
GLOBALSTEP         integer
GLOBALLASTSTEP     integer
STEP               integer
RANK               integer
STEPSTATUS         string
RETRIEVEMODE       bit
FILENAME           string
ISMOVED            bit
IDRULE             string
BLOCKSZ            integer
ORIGINALNAME       string
FILEINFO           string
TRANSFERINFO       string
MODETRANS          integer
STARTTRANS         timestamp
STOPTRANS          timestamp
INFOSTATUS         string
UPDATEDINFO        integer
OWNERREQ           string
REQUESTER          string
REQUESTED          string
SPECIALID          int
================== ========= ============

Exemple
-------

.. code-block:: json

    {
      "GLOBALSTEP" : 1,
      "GLOBALLASTSTEP" :2,
      "STEP" : 1,
      "RANK" : 1,
      "STEPSTATUS" : "PENDING",
      "RETRIEVEMODE" : 1,
      "FILENAME" : "test-file.txt",
      "ISMOVED" : 0,
      "IDRULE" : "2a1",
      "BLOCKSZ" : 32,
      "ORIGINALNAME" : "test-file.txt",
      "FILEINFO" : "LONGVARCHAR",
      "TRANSFERINFO" : "LONGVARCHAR",
      "MODETRANS" : 1,
      "STARTTRANS" : "TIMESTAMP",
      "STOPTRANS" : "TIMESTAMP",
      "INFOSTATUS" : "VARCHAR",
      "UPDATEDINFO" : "INTEGER",
      "OWNERREQ" : "VARCHAR",
      "REQUESTER" : "VARCHAR",
      "REQUESTED" : "VARCHAR",
      "SPECIALID" : "BIGINT"
    }

Paramètres
==========

================ ============ ====================================== 
Paramètre        Type         Desctiption                           
================ ============ ======================================
GLOBALSTEP       integer                                       
GLOBALLASTSTEP   integer                                       
STEP             integer                                       
RANK             integer                                       
STEPSTATUS       varchar                                       
RETRIEVEMODE     bit                                       
FILENAME         varchar                                       
ISMOVED          bit                                       
IDRULE           varchar                                       
BLOCKSZ          integer                                       
ORIGINALNAME     varchar                                       
FILEINFO         longvarchar                                       
TRANSFERINFO     longvarchar                                       
MODETRANS        integer                                       
STARTTRANS       timestamp                                       
STOPTRANS        timestamp                                       
INFOSTATUS       varchar                                       
UPDATEDINFO      integer                                       
OWNERREQ         varchar                                       
REQUESTER        varchar                                       
REQUESTED        varchar                                       
SPECIALID        bigint                                       
================ ============ ======================================

PUT /transfers/:id
******************

Modifie le transfert spécifié

Réponse
=======

Modèle
------

*DbTaskRunner*

================== ========= ============
Champs             Type      Description
================== ========= ============
GLOBALSTEP         integer
GLOBALLASTSTEP     integer
STEP               integer
RANK               integer
STEPSTATUS         string
RETRIEVEMODE       bit
FILENAME           string
ISMOVED            bit
IDRULE             string
BLOCKSZ            integer
ORIGINALNAME       string
FILEINFO           string
TRANSFERINFO       string
MODETRANS          integer
STARTTRANS         timestamp
STOPTRANS          timestamp
INFOSTATUS         string
UPDATEDINFO        integer
OWNERREQ           string
REQUESTER          string
REQUESTED          string
SPECIALID          int
================== ========= ============

Exemple
-------

.. code-block:: json

    {
      "GLOBALSTEP" : 1,
      "GLOBALLASTSTEP" :2,
      "STEP" : 1,
      "RANK" : 1,
      "STEPSTATUS" : "PENDING",
      "RETRIEVEMODE" : 1,
      "FILENAME" : "test-file.txt",
      "ISMOVED" : 0,
      "IDRULE" : "2a1",
      "BLOCKSZ" : 32,
      "ORIGINALNAME" : "test-file.txt",
      "FILEINFO" : "LONGVARCHAR",
      "TRANSFERINFO" : "LONGVARCHAR",
      "MODETRANS" : 1,
      "STARTTRANS" : "TIMESTAMP",
      "STOPTRANS" : "TIMESTAMP",
      "INFOSTATUS" : "VARCHAR",
      "UPDATEDINFO" : "INTEGER",
      "OWNERREQ" : "VARCHAR",
      "REQUESTER" : "VARCHAR",
      "REQUESTED" : "VARCHAR",
      "SPECIALID" : "BIGINT"
    }

Paramètres
==========
  
================ ============ ====================================== 
Paramètre        Type         Desctiption                           
================ ============ ======================================
SPECIALID        bigint                                       
REQUESTER        varchar                                       
REQUESTED        varchar                                       
OWNERREQ         varchar                                       
GLOBALSTEP       integer                                       
GLOBALLASTSTEP   integer                                       
STEP             integer                                       
RANK             integer                                       
STEPSTATUS       varchar                                       
RETRIEVEMODE     bit                                       
FILENAME         varchar                                       
ISMOVED          bit                                       
BLOCKSZ          integer                                       
ORIGINALNAME     varchar                                       
FILEINFO         longvarchar                                       
TRANSFERINFO     longvarchar                                       
MODETRANS        integer                                       
STARTTRANS       timestamp                                       
STOPTRANS        timestamp                                       
INFOSTATUS       varchar                                       
UPDATEDINFO      integer                                       
================ ============ ======================================

DELETE /transfers/:id
*********************

Supprime le transfert spécifié

Réponse
=======

Modèle
------

*DbTaskRunner*

================== ========= ============
Champs             Type      Description
================== ========= ============
GLOBALSTEP         integer
GLOBALLASTSTEP     integer
STEP               integer
RANK               integer
STEPSTATUS         string
RETRIEVEMODE       bit
FILENAME           string
ISMOVED            bit
IDRULE             string
BLOCKSZ            integer
ORIGINALNAME       string
FILEINFO           string
TRANSFERINFO       string
MODETRANS          integer
STARTTRANS         timestamp
STOPTRANS          timestamp
INFOSTATUS         string
UPDATEDINFO        integer
OWNERREQ           string
REQUESTER          string
REQUESTED          string
SPECIALID          int
================== ========= ============

Exemple
-------

.. code-block:: json

    {
      "GLOBALSTEP" : 1,
      "GLOBALLASTSTEP" :2,
      "STEP" : 1,
      "RANK" : 1,
      "STEPSTATUS" : "PENDING",
      "RETRIEVEMODE" : 1,
      "FILENAME" : "test-file.txt",
      "ISMOVED" : 0,
      "IDRULE" : "2a1",
      "BLOCKSZ" : 32,
      "ORIGINALNAME" : "test-file.txt",
      "FILEINFO" : "LONGVARCHAR",
      "TRANSFERINFO" : "LONGVARCHAR",
      "MODETRANS" : 1,
      "STARTTRANS" : "TIMESTAMP",
      "STOPTRANS" : "TIMESTAMP",
      "INFOSTATUS" : "VARCHAR",
      "UPDATEDINFO" : "INTEGER",
      "OWNERREQ" : "VARCHAR",
      "REQUESTER" : "VARCHAR",
      "REQUESTED" : "VARCHAR",
      "SPECIALID" : "BIGINT"
    }

Paramètres
==========
  
================ ========= =========================================== 
Paramètre        Type      Desctiption                                
================ ========= ===========================================
SPECIALID        integer   Special Id as LONG in URI as transfers/id  
REQUESTER        varchar   Partner as requester                       
REQUESTED        varchar   Partner as requested                       
OWNERREQ         varchar   Owner of this request                      
================ ========= ===========================================
