/configurations
###############
      
GET /configurations
*******************

Réponse
=======

Modèle
------

  ================== =======================
  Champs             Type
  ================== =======================
  DbConfiguration    Array[DBConfiguration]
  ================== =======================

  *DbConfiguration*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  READGLOBALLIMIT    integer
  WRITEGLOBALLIMIT   integer
  READSESSIONLIMIT   integer
  WRITESESSIONLIMIT  integer
  DELAYLIMIT         integer
  UPDATEDINFO        integer
  HOSTID             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    [{
      "READGLOBALLIMIT" : "BIGINT",
       "WRITEGLOBALLIMIT" : "BIGINT",
       "READSESSIONLIMIT" : "BIGINT",
       "WRITESESSIONLIMIT" : "BIGINT",
       "DELAYLIMIT" : "BIGINT",
       "UPDATEDINFO" : "INTEGER",
       "HOSTID" : "VARCHAR"
    },]

Paramètres
==========

  =========== ========= ====================================== 
  Paramètre   Type      Desctiption                            
  =========== ========= ====================================== 
  HOSTID      string    host name                              
  BANDWIDTH   integer   <0 for no filter, =0 for no bandwidth, >0 for a limit greater than value integer
  =========== ========= ====================================== 

GET /configurations/:id
***********************
  
Réponse
=======

Modèle
------
  
  *DbConfiguration*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  READGLOBALLIMIT    integer
  WRITEGLOBALLIMIT   integer
  READSESSIONLIMIT   integer
  WRITESESSIONLIMIT  integer
  DELAYLIMIT         integer
  UPDATEDINFO        integer
  HOSTID             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    {
      "READGLOBALLIMIT" : "BIGINT",
       "WRITEGLOBALLIMIT" : "BIGINT",
       "READSESSIONLIMIT" : "BIGINT",
       "WRITESESSIONLIMIT" : "BIGINT",
       "DELAYLIMIT" : "BIGINT",
       "UPDATEDINFO" : "INTEGER",
       "HOSTID" : "VARCHAR"
    }

Paramètres
==========

  =========== ========= ======================================
  Paramètre   Type      Desctiption                           
  =========== ========= ======================================
  :id         varchar   HostId in URI as hostconfigs/id       
  =========== ========= ======================================

POST /configurations
********************

Réponse
=======

Modèle
------

  *DbConfiguration*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  READGLOBALLIMIT    integer
  WRITEGLOBALLIMIT   integer
  READSESSIONLIMIT   integer
  WRITESESSIONLIMIT  integer
  DELAYLIMIT         integer
  UPDATEDINFO        integer
  HOSTID             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    {
      "READGLOBALLIMIT" : "BIGINT",
      "WRITEGLOBALLIMIT" : "BIGINT",
      "READSESSIONLIMIT" : "BIGINT",
      "WRITESESSIONLIMIT" : "BIGINT",
      "DELAYLIMIT" : "BIGINT",
      "UPDATEDINFO" : "INTEGER",
      "HOSTID" : "VARCHAR"
    }

Paramètres
==========

  ================== ========= ======================================
  Paramètre          Type      Desctiption
  ================== ========= ======================================
  READGLOBALLIMIT    bigint
  WRITEGLOBALLIMIT   bigint
  READSESSIONLIMIT   bigint
  WRITESESSIONLIMIT  bigint
  DELAYLIMIT         bigint
  updatedinfo        integer
  HOSTID             varchar
  ================== ========= ======================================

PUT /configurations/:id
***********************

Réponse
=======

Modèle
------

  *DbConfiguration*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  READGLOBALLIMIT    integer
  WRITEGLOBALLIMIT   integer
  READSESSIONLIMIT   integer
  WRITESESSIONLIMIT  integer
  DELAYLIMIT         integer
  UPDATEDINFO        integer
  HOSTID             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    {
      "READGLOBALLIMIT" : "BIGINT",
      "WRITEGLOBALLIMIT" : "BIGINT",
      "READSESSIONLIMIT" : "BIGINT",
      "WRITESESSIONLIMIT" : "BIGINT",
      "DELAYLIMIT" : "BIGINT",
      "UPDATEDINFO" : "INTEGER",
      "HOSTID" : "VARCHAR"
    }

Paramètres
==========

  ================== ========= ====================================== 
  Paramètre          Type      Desctiption                            
  ================== ========= ====================================== 
  :id                varchar   HostId in URI as hostconfigs/id        
  READGLOBALLIMIT    bigint                                        
  WRITEGLOBALLIMIT   bigint                                        
  READSESSIONLIMIT   bigint                                        
  WRITESESSIONLIMIT  bigint                                        
  DELAYLIMIT         bigint                                        
  updatedinfo        integer                                        
  ================== ========= ====================================== 

DELETE /configurations/:id
**************************

Réponse
=======

Modèle
------

  *DbConfiguration*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  READGLOBALLIMIT    integer
  WRITEGLOBALLIMIT   integer
  READSESSIONLIMIT   integer
  WRITESESSIONLIMIT  integer
  DELAYLIMIT         integer
  UPDATEDINFO        integer
  HOSTID             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    {
      "READGLOBALLIMIT" : "BIGINT",
      "WRITEGLOBALLIMIT" : "BIGINT",
      "READSESSIONLIMIT" : "BIGINT",
      "WRITESESSIONLIMIT" : "BIGINT",
      "DELAYLIMIT" : "BIGINT",
      "UPDATEDINFO" : "INTEGER",
      "HOSTID" : "VARCHAR"
    }

Paramètres
==========

  =========== ========= ======================================
  Paramètre   Type      Desctiption                           
  =========== ========= ======================================
  :id         varchar   HostId in URI as hostconfigs/id       
  =========== ========= ======================================