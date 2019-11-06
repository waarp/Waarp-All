/business
#########

GET /business
*************

Réponse
=======

Modèle
------

  ================== =======================
  Champs             Type
  ================== =======================
  answer             Array[]
  ================== =======================

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============     
  @class             string    org.waarp.openr66.protocol.localhandler.packet.json.BusinessRequestJsonPacket
  comment            string    Business execution request (GET)
  requestUserPacket  integer   22
  className          string    Class name to execute
  arguments          string    Arguments of the execution
  extraArguments     string    Extra arguments
  delay              integer   0
  toApplied          boolean   false
  validated          boolean   false
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    [{
      "@class" : "org.waarp.openr66.protocol.localhandler.packet.json.BusinessRequestJsonPacket"
      "comment" : "Business execution request (GET)",
      "requestUserPacket" : 22,
      "className" : "Class name to execute",
      "arguments" : "Arguments of the execution",
      "extraArguments" : "Extra arguments",
      "delay" : 0,
      "toApplied" : false,
      "validated" : false
    },]

Paramètres
==========

  ================= ========= =========================================== 
  Paramètre         Type      Desctiption                                
  ================= ========= ===========================================
  @class            string    org.waarp.openr66.protocol.localhandler.packet.json.BusinessRequestJsonPacket
  comment           string    Business execution request (GET)
  requestUserPacket integer   22
  className         string    Class name to execute
  arguments         string    Arguments of the execution
  extraArguments    string    Extra arguments
  delay             integer   0
  toApplied         boolean   false
  validated         boolean   false
  ================= ========= ===========================================

