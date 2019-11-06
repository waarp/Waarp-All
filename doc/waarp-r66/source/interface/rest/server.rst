/server
#######

GET /server
***********

Réponse
=======

Modèle
------

  ================== =======================
  Champs             Type
  ================== =======================
  answer             Array[]
  ================== =======================

  ================== ============= ============
  Champs             Type          Description
  ================== ============= ============
  HostID             string        hosta
  Date               timestamp     2014-05-13T11:02:52.185+02:00
  LastRun            timestamp     1970-01-01T01:00:00.000+01:00
  FromDate           timestamp     1970-01-01T01:00:00.000+01:00
  SecondsRunning     integer       0
  NetworkConnections integer       0
  NbThreads          integer       0
  InBandwidth        integer       0
  OutBandwidth       integer       0
  OVERALL            OVERALL
  STEPS              STEPS
  RUNNINGSTEPS       RUNNINGSTEPS
  ERRORTYPES         ERRORTYPES
  ================== ============= ============

  **OVERALL**

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============ 
  AllTransfer        integer   0
  Unknown            integer   0
  NotUpdated         integer   0
  Interrupted        integer   0
  ToSubmit           integer   0
  Error              integer   0
  Running            integer   0
  Done               integer   0
  InRunning          integer   0
  OutRunning         integer   0
  LastInRunning      timestamp 2014-05-13T11:02:43.732+02:00
  LastOutRunning     timestamp 2014-05-13T11:02:43.732+02:00
  InAll              integer   0
  OutAll             integer   0
  InError            integer   0
  OutError           integer   0 
  ================== ========= ============

  **STEPS**

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============ 
  Notask             integer   0
  Pretask            integer   0
  Transfer           integer   0
  Posttask           integer   0
  AllDone            integer   0
  Error              integer   0 
  ================== ========= ============

  **RUNNINGSTEPS**

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  AllRunning         integer   0
  Running            integer   0
  InitOk             integer   0
  PreProcessingOk    integer   0
  TransferOk         integer   0
  PostProcessingOk   integer   0
  CompleteOk         integer   0 
  ================== ========= ============

  **ERRORTYPES**

  ====================== ========= ============
  Champs                 Type      Description
  ====================== ========= ============
  ConnectionImpossible   interger  0
  ServerOverloaded       interger  0
  BadAuthent             interger  0
  ExternalOp             interger  0
  TransferError          interger  0
  MD5Error               interger  0
  Disconnection          interger  0
  FinalOp                interger  0
  Unimplemented          interger  0
  Internal               interger  0
  Warning                interger  0
  QueryAlreadyFinished   interger  0
  QueryStillRunning      interger  0
  KnownHost              interger  0
  RemotelyUnknown        interger  0
  CommandNotFound        interger  0
  PassThroughMode        interger  0
  RemoteShutdown         interger  0
  Shutdown               interger  0
  RemoteError            interger  0
  Stopped                interger  0
  Canceled               interger  0
  FileNotFound           interger  0
  Unknown                interger  0 
  ====================== ========= ============

Exemple
-------

  .. code-block:: json

    [{
      "HostID" : "hosta",
      "Date" : "2014-05-13T11:02:52.185+02:00",
      "LastRun" : "1970-01-01T01:00:00.000+01:00",
      "FromDate" : "1970-01-01T01:00:00.000+01:00",
      "SecondsRunning" : 0,
      "NetworkConnections" : 0,
      "NbThreads" : 0,
      "InBandwidth" : 0,
      "OutBandwidth" : 0,
      "OVERALL" : { 
        "AllTransfer" : 0,
        "Unknown" : 0,
        "NotUpdated" : 0,
        "Interrupted" : 0,
        "ToSubmit" : 0,
        "Error" : 0,
        "Running" : 0,
        "Done" : 0,
        "InRunning" : 0,
        "OutRunning" : 0,
        "LastInRunning" : "2014-05-13T11:02:43.732+02:00",
        "LastOutRunning" : "2014-05-13T11:02:43.732+02:00",
        "InAll" : 0,
        "OutAll" : 0,
        "InError" : 0,
        "OutError" : 0 
      },
      "STEPS" : { 
        "Notask" : 0,
        "Pretask" : 0,
        "Transfer" : 0,
        "Posttask" : 0,
        "AllDone" : 0,
        "Error" : 0 
      },
      "RUNNINGSTEPS" : { 
        "AllRunning" : 0,
        "Running" : 0,
        "InitOk" : 0,
        "PreProcessingOk" : 0,
        "TransferOk" : 0,
        "PostProcessingOk" : 0,
        "CompleteOk" : 0 
      },
      "ERRORTYPES" : {
        "ConnectionImpossible" : 0,
        "ServerOverloaded" : 0,
        "BadAuthent" : 0,
        "ExternalOp" : 0,
        "TransferError" : 0,
        "MD5Error" : 0,
        "Disconnection" : 0,
        "FinalOp" : 0,
        "Unimplemented" : 0,
        "Internal" : 0,
        "Warning" : 0,
        "QueryAlreadyFinished" : 0,
        "QueryStillRunning" : 0,
        "KnownHost" : 0,
        "RemotelyUnknown" : 0,
        "CommandNotFound" : 0,
        "PassThroughMode" : 0,
        "RemoteShutdown" : 0,
        "Shutdown" : 0,
        "RemoteError" : 0,
        "Stopped" : 0,
        "Canceled" : 0,
        "FileNotFound" : 0,
        "Unknown" : 0 
      }
    },]


PUT /server
***********

Réponse
=======

Modèle
------

  ================== =======================
  Champs             Type
  ================== =======================
  answer             Array[]
  ================== =======================

  ================== ============= ============
  Champs             Type          Description
  ================== ============= ============
  @class             string        org.waarp.openr66.protocol.localhandler.packet.json.ShutdownOrBlockJsonPacket
  comment            string        Shutdown Or Block request (PUT)
  requestUserPacket  integer       0
  key                [byte]        S2V5
  shutdownOrBlock    boolean       false
  restartOrBlock     boolean       false
  ================== ============= ============

Exemple
-------

  .. code-block:: json

    [{
      "@class" : "org.waarp.openr66.protocol.localhandler.packet.json.ShutdownOrBlockJsonPacket",
      "comment" : "Shutdown Or Block request (PUT)",
      "requestUserPacket" : 0,
      "key" : "S2V5",
      "shutdownOrBlock" : false,
      "restartOrBlock" : false
    },]


Paramètres
==========

  ================== ========= ============
  Paramètre          Type      Description
  ================== ========= ============
  @class             string    org.waarp.openr66.protocol.localhandler.packet.json.ShutdownOrBlockJsonPacket
  comment            string    Shutdown Or Block request (PUT)
  requestUserPacket  integer   0
  key                [byte]    S2V5
  shutdownOrBlock    boolean   false
  restartOrBlock     boolean   false
  ================== ========= ============
