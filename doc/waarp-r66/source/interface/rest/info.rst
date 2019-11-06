/info
#####

GET /info
*********

Réponse
=======

Modèle
------

================== =======================
Champs             Type
================== =======================
answer             Array[path]
================== =======================

Exemple
-------

.. code-block:: json
   
    [
      "PathA",
      "PathB"
    ]
     

Paramètre
=========

================== ========= ============
Paramètre          Type      Description
================== ========= ============
@class             string    org.waarp.openr66.protocol.localhandler.packet.json.InformationJsonPacket
comment            string    Information request (GET)
requestUserPacket  integer   18
id                 integer   0
request            integer   0
rulename           string    The rule name associated with the remote repository
filename           string    The filename to look for if any
idRequest          boolean   false
to                 boolean   false
================== ========= ============

GET /info
*********

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
SPECIALID          integer
================== ========= ============

Exemple
-------

.. code-block:: json

    [{
      "GLOBALSTEP" : "INTEGER",
      "GLOBALLASTSTEP" : "INTEGER",
      "STEP" : "INTEGER",
      "RANK" : "INTEGER",
      "STEPSTATUS" : "VARCHAR",
      "RETRIEVEMODE" : "BIT",
      "FILENAME" : "VARCHAR",
      "ISMOVED" : "BIT",
      "IDRULE" : "VARCHAR",
      "BLOCKSZ" : "INTEGER",
      "ORIGINALNAME" : "VARCHAR",
      "FILEINFO" : "LONGVARCHAR",
      "TRANSFERINFO" : "LONGVARCHAR",
      "MODETRANS" : "INTEGER",
      "STARTTRANS" : "TIMESTAMP",
      "STOPTRANS" : "TIMESTAMP",
      "INFOSTATUS" : "VARCHAR",
      "UPDATEDINFO" : "INTEGER",
      "OWNERREQ" : "VARCHAR",
      "REQUESTER" : "VARCHAR",
      "REQUESTED" : "VARCHAR",
      "SPECIALID" : "BIGINT"
    },]

Paramètre
=========

  ================== ========= ============
  Paramètre          Type      Description
  ================== ========= ============
  @class             string    org.waarp.openr66.protocol.localhandler.packet.json.InformationJsonPacket
  comment            string    Information on Transfer request (GET)
  requestUserPacket  integer   18
  id                 integer   0
  request            integer   0
  rulename           string    remoteHost
  filename           string    null
  idRequest          boolean   true
  to                 boolean   false
  ================== ========= ============

