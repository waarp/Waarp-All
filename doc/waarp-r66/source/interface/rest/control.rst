/control
########

GET /control
************

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
    },]

Paramètres
==========

  =================== ========= ======================================
  Paramètre           Type      Description
  =================== ========= ======================================
  @class              string    org.waarp.openr66.protocol.localhandler.packet.json.InformationJsonPacket
  comment             string    Information on Transfer request (GET)
  requestUserPacket   integer   18
  id                  integer   0
  request             integer   0
  rulename            string    remoteHost
  filename            string    null
  idRequest           boolean   true
  to                  boolean   false
  =================== ========= ======================================

PUT /control
************

Réponse
=======

Modèle
------

  ================== =======================
  Champs             Type
  ================== =======================
  DbConfiguration    Array[DbTaskRunner]
  ================== =======================

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  @class             string    org.waarp.openr66.protocol.localhandler.packet.json.RestartTransferJsonPacket
  comment            string    Restart Transfer request (PUT)
  requestUserPacket  integer   4
  requester          string    Requester host
  requested          string    Requested host
  specialid          integer   -9223372036854775808
  restarttime        integer   1399760601381
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    [{
      "@class" : "org.waarp.openr66.protocol.localhandler.packet.json.RestartTransferJsonPacket"
      "comment" : "Restart Transfer request (PUT)"
      "requestUserPacket" : 4
      "requester" : "Requester host"
      "requested" : "Requested host"
      "specialid" : -9223372036854775808
      "restarttime" : 1399760601381
    },]

Paramètres
==========

  ================== ========= ============
  Paramètre          Type      Description
  ================== ========= ============
  @class             string    Class to execute ({Restart,Cancel}TransferJsonPacket)
  comment            string    Restart Transfer request (PUT)
  requestUserPacket  integer   4
  requester          string    Requester host
  requested          string    Requested host
  specialid          integer   -9223372036854775808
  restarttime        timestamp 1399760601381
  ================== ========= ============

POST /control
*************

Réponse
=======

Modèle
------

  ================== =======================
  Champs             Type
  ================== =======================
                     Array[DbTaskRunner]
  ================== =======================

  *DbTaskRunner*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  @class             string    org.waarp.openr66.protocol.localhandler.packet.json.TransferRequestJsonPacket
  comment            string    Transfer Request (POST)
  requestUserPacket  integer   7
  rulename           string    Rulename
  mode               integer   0
  filename           string    Filename
  requested          string    Requested host
  blocksize          integer   0
  rank               integer   0
  specialId          integer   0
  validate           integer   0
  originalSize       integer   0
  fileInformation    string    File information
  separator          char      {
  start              integer   1399760601381
  delay              integer   0
  toValidate         boolean   true
  additionalDelay    boolean   false
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    [{
      "@class" : "org.waarp.openr66.protocol.localhandler.packet.json.TransferRequestJsonPacket",
      "comment" : "Transfer Request (POST)",
      "requestUserPacket" : 7,
      "rulename" : "Rulename",
      "mode" : 0,
      "filename" : "Filename",
      "requested" : "Requested host",
      "blocksize" : 0,
      "rank" : 0,
      "specialId" : 0,
      "validate" : 0,
      "originalSize" : 0,
      "fileInformation" : "File information",
      "separator" : "{",
      "start" : 1399760601381,
      "delay" : 0,
      "toValidate" : true,
      "additionalDelay" : false
    },]

Paramètres
==========

  ================== ========= ============
  Paramètre          Type      Description
  ================== ========= ============
  @class             string    org.waarp.openr66.protocol.localhandler.packet.json.TransferRequestJsonPacket
  comment            string    Transfer Request (POST)
  requestUserPacket  integer   7
  rulename           string    Rulename
  mode               integer   0
  filename           string    Filename
  requested          string    Requested host
  blocksize          integer   0
  rank               integer   0
  specialId          integer   0
  validate           integer   0
  originalSize       integer   0
  fileInformation    string    File information
  separator          char      {
  start              timestamp 1399760601381
  delay              integer   0
  toValidate         boolean   true
  additionalDelay    boolean   false
  ================== ========= ============
