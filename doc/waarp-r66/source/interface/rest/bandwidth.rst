/bandwidth
##########

GET /bandwidth
**************

Réponse
=======

Modèle
------

  ================== =======================
  Champs             Type
  ================== =======================
  answer             Array[DbBandwith]
  ================== =======================

  **DbBandwith**

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  @class             string    org.waarp.openr66.protocol.localhandler.packet.json.BandwidthJsonPacket
  comment            string    Bandwidth getter (GET)
  requestUserPacket  integer   19
  setter             boolean   false
  writeglobal        integer   -10
  readglobal         integer   -10
  writesession       integer   -10
  readsession        integer   -10
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    [{
      "@class": "org.waarp.openr66.protocol.localhandler.packet.json.BandwidthJsonPacket",
      "comment": "Bandwidth getter (GET)",
      "requestUserPacket": 19,
      "setter": false,
      "writeglobal": -10,
      "readglobal": -10,
      "writesession": -10,
      "readsession": -10
    }]

Paramètres
==========

  ================== ========= ============
  Paramètre          Type      Description
  ================== ========= ============
  @class             string      
  comment            string     
  requestUserPacket  string               
  setter             boolean    
  writeglobal        integer         
  readglobal         integer        
  writesession       integer           
  readsession        integer         
  ================== ========= ============

PUT /bandwidth
**************

Réponse
=======

Modèle
------

  ================== =======================
  Champs             Type
  ================== =======================
  answer             Array[DbBandwith]
  ================== =======================

  **DbBandwith**

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  @class             string    org.waarp.openr66.protocol.localhandler.packet.json.BandwidthJsonPacket
  comment            string    Bandwidth setter (SET)
  requestUserPacket  integer   19
  setter             boolean   false
  writeglobal        integer   -10
  readglobal         integer   -10
  writesession       integer   -10
  readsession        integer   -10
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    [{
      "@class" : "org.waarp.openr66.protocol.localhandler.packet.json.BandwidthJsonPacket",
      "comment" : "Bandwidth setter (SET)",
      "requestUserPacket" : 19,
      "setter" : true,
      "writeglobal" : -10,
      "readglobal" : -10,
      "writesession" : -10,
      "readsession" : -10
    },]

Paramètres
==========

  ================== ========= ============
  Paramètre          Type      Description
  ================== ========= ============
  @class             string      
  comment            string     
  requestUserPacket  string               
  setter             boolean    
  writeglobal        integer         
  readglobal         integer        
  writesession       integer           
  readsession        integer 
  ================== ========= ============
  
