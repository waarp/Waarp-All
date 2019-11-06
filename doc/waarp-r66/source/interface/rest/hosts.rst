/hosts
######

GET /hosts
**********

  Retourne tous les hôtes connus du moniteur

Réponse
=======

Modèle
------

  ================== =======================
  Champs             Type
  ================== =======================
  DbConfiguration    Array[DbHostAuth]
  ================== =======================

  *DbHostAuth*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  ADDRESS            string
  PORT               integer
  ISSSL              bit
  HOSTKEY            [binary]
  ADMINROLE          bit
  ISCLIENT           bit
  ISACTIVE           bit
  ISPROXIFIED        bit
  UPDATEDINFO        integer
  HOSTID             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    [{
      "ADDRESS" : "127.0.0.1",
      "PORT" : "6666",
      "ISSSL" : 0,
      "HOSTKEY" : "VARBINARY",
      "ADMINROLE" : false,
      "ISCLIENT" : false,
      "ISACTIVE" : true,
      "ISPROXIFIED" : false,
      "UPDATEDINFO" : "INTEGER",
      "HOSTID" : "server1"
    },]

Paramètres
==========

  =========== ========= ====================================== 
  Paramètre   Type      Desctiption                            
  =========== ========= ====================================== 
  HOSTID      string    Host name                              
  ADDRESS     string    Address of this partner                
  ISSSL       bool      Is Ssl entry                           
  ISACTIVE    bool      Is Active entry                        
  =========== ========= ====================================== 

GET /hosts/:id
**************

  Retourne l'hôte spécifié

Réponse
=======

Modèle
------

  *DbHostAuth*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  ADDRESS            string
  PORT               integer
  ISSSL              bit
  HOSTKEY            [binary]
  ADMINROLE          bit
  ISCLIENT           bit
  ISACTIVE           bit
  ISPROXIFIED        bit
  UPDATEDINFO        integer
  HOSTID             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    {
      "ADDRESS" : "127.0.0.1",
      "PORT" : "6666",
      "ISSSL" : 0,
      "HOSTKEY" : "VARBINARY",
      "ADMINROLE" : false,
      "ISCLIENT" : false,
      "ISACTIVE" : true,
      "ISPROXIFIED" : false,
      "UPDATEDINFO" : "INTEGER",
      "HOSTID" : "server1"
    }

Paramètres
==========

  =========== ========= ====================================== 
  Paramètre   Type      Desctiption                            
  =========== ========= ====================================== 
  :id         string    HostId in URI as hosts/id              
  =========== ========= ====================================== 

POST /hosts
***********

  Crée un nouvel hôte

Réponse
=======

Modèle
------

  *DbHostAuth*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  ADDRESS            string
  PORT               integer
  ISSSL              bit
  HOSTKEY            [binary]
  ADMINROLE          bit
  ISCLIENT           bit
  ISACTIVE           bit
  ISPROXIFIED        bit
  UPDATEDINFO        integer
  HOSTID             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    {
      "ADDRESS" : "127.0.0.1",
      "PORT" : "6666",
      "ISSSL" : 0,
      "HOSTKEY" : "VARBINARY",
      "ADMINROLE" : false,
      "ISCLIENT" : false,
      "ISACTIVE" : true,
      "ISPROXIFIED" : false,
      "UPDATEDINFO" : "INTEGER",
      "HOSTID" : "server1"
    }


Paramètres
==========

  =========== ========= ======================================
  Paramètre   Type      Desctiption                           
  =========== ========= ======================================
  ADDRESS     varchar                                       
  PORT        integer                                       
  ISSSL       bit                                       
  HOSTKEY     varbinary                                       
  ADMINROLE   bit                                       
  ISCLIENT    bit                                       
  ISACTIVE    bit                                       
  ISPROXIFIED bit                                       
  UPDATEDINFO integer                                       
  HOSTID      varchar                                       
  =========== ========= ======================================

PUT /hosts/:id
**************

  Modifie l'hôte spécifié

Réponse
=======

Modèle
------

  *DbHostAuth*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  ADDRESS            string
  PORT               integer
  ISSSL              bit
  HOSTKEY            [binary]
  ADMINROLE          bit
  ISCLIENT           bit
  ISACTIVE           bit
  ISPROXIFIED        bit
  UPDATEDINFO        integer
  HOSTID             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    {
      "ADDRESS" : "127.0.0.1",
      "PORT" : "6666",
      "ISSSL" : 0,
      "HOSTKEY" : "VARBINARY",
      "ADMINROLE" : false,
      "ISCLIENT" : false,
      "ISACTIVE" : true,
      "ISPROXIFIED" : false,
      "UPDATEDINFO" : "INTEGER",
      "HOSTID" : "server1"
    }

Paramètres
==========

  =========== ========= ======================================
  Paramètre   Type      Desctiption                           
  =========== ========= ======================================
  HOSTID      varchar   HostId in URI as hosts/id             
  ADDRESS     varchar                                       
  PORT        integer                                       
  ISSSL       bit                                       
  HOSTKEY     varbinary                                       
  ADMINROLE   bit                                       
  ISCLIENT    bit                                       
  ISACTIVE    bit                                       
  ISPROXIFIED bit                                       
  UPDATEDINFO integer                                       
  =========== ========= ======================================

DELETE /hosts/:id
*****************

  Supprime l'hôte spécifié

Réponse
=======

Modèle
------

  *DbHostAuth*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  ADDRESS            string
  PORT               integer
  ISSSL              bit
  HOSTKEY            [binary]
  ADMINROLE          bit
  ISCLIENT           bit
  ISACTIVE           bit
  ISPROXIFIED        bit
  UPDATEDINFO        integer
  HOSTID             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    {
      "ADDRESS" : "127.0.0.1",
      "PORT" : "6666",
      "ISSSL" : 0,
      "HOSTKEY" : "VARBINARY",
      "ADMINROLE" : false,
      "ISCLIENT" : false,
      "ISACTIVE" : true,
      "ISPROXIFIED" : false,
      "UPDATEDINFO" : "INTEGER",
      "HOSTID" : "server1"
    }

Paramètres
==========

  =========== ========= ======================================
  Paramètre   Type      Desctiption                           
  =========== ========= ======================================
  :id         varchar   HostId in URI as hosts/id             
  =========== ========= ======================================
