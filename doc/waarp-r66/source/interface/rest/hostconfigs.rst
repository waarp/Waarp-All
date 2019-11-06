/hostconfigs
############

GET /hostconfigs
****************

  Retourne toutes les configurations des hôotes

Réponse
=======

Modèle
------

  ================== =======================
  Champs             Type
  ================== =======================
  DbConfiguration    Array[DbHostConfiguration]
  ================== =======================

  *DbHostConfiguration*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  BUSINESS           string
  ROLES              string
  ALIASES            string
  OTHERS             string
  UPDATEDINFO        integer
  HOSTID             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    [{
      "BUSINESS" : "LONGVARCHAR",
      "ROLES" : "LONGVARCHAR",
      "ALIASES" : "LONGVARCHAR",
      "OTHERS" : "LONGVARCHAR",
      "UPDATEDINFO" : "INTEGER",
      "HOSTID" : "VARCHAR"
    },]

Paramètres
==========

  =========== ============ ======================================
  Paramètre   Type         Desctiption                           
  =========== ============ ======================================
  BUSINESS    longvarchar                                       
  ROLES       longvarchar                                       
  ALIASES     longvarchar                                       
  OTHERS      longvarchar                                       
  HOSTID      varchar                                       
  =========== ============ ======================================

GET /hostconfigs/:id
********************

Réponse
=======

Modèle
------

  *DbHostConfiguration*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  BUSINESS           string
  ROLES              string
  ALIASES            string
  OTHERS             string
  UPDATEDINFO        integer
  HOSTID             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    {
      "BUSINESS" : "LONGVARCHAR",
      "ROLES" : "LONGVARCHAR",
      "ALIASES" : "LONGVARCHAR",
      "OTHERS" : "LONGVARCHAR",
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

PUT /hostconfigs/:id
********************

Réponse
=======

Modèle
------

  *DbHostConfiguration*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  BUSINESS           string
  ROLES              string
  ALIASES            string
  OTHERS             string
  UPDATEDINFO        integer
  HOSTID             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    {
      "BUSINESS" : "LONGVARCHAR",
      "ROLES" : "LONGVARCHAR",
      "ALIASES" : "LONGVARCHAR",
      "OTHERS" : "LONGVARCHAR",
      "UPDATEDINFO" : "INTEGER",
      "HOSTID" : "VARCHAR"
    }

Paramètres
==========

  =========== ============= ======================================
  Paramètre   Type          Desctiption                           
  =========== ============= ======================================
  :id         varchar       HostId in URI as hostconfigs/id       
  BUSINESS    longvarchar                                       
  ROLES       longvarchar                                       
  ALIASES     longvarchar                                       
  OTHERS      longvarchar                                       
  UPDATEDINFO integer                                       
  =========== ============= ======================================

POST /hostconfigs
*****************

Réponse
=======

Modèle
------

  *DbHostConfiguration*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  BUSINESS           string
  ROLES              string
  ALIASES            string
  OTHERS             string
  UPDATEDINFO        integer
  HOSTID             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    {
      "BUSINESS" : "LONGVARCHAR",
      "ROLES" : "LONGVARCHAR",
      "ALIASES" : "LONGVARCHAR",
      "OTHERS" : "LONGVARCHAR",
      "UPDATEDINFO" : "INTEGER",
      "HOSTID" : "VARCHAR"
    }

Paramètres
==========

  =========== ============= ======================================
  Paramètre   Type          Desctiption                           
  =========== ============= ======================================
  BUSINESS    longvarchar                                       
  ROLES       longvarchar                                       
  ALIASES     longvarchar                                       
  OTHERS      longvarchar                                       
  UPDATEDINFO integer                                       
  HOSTID      varchar                                       
  =========== ============= ======================================

DELETE /hostconfigs/:id
***********************

Réponse
=======

Modèle
------

  *DbHostConfiguration*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  BUSINESS           string
  ROLES              string
  ALIASES            string
  OTHERS             string
  UPDATEDINFO        integer
  HOSTID             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    {
      "BUSINESS" : "LONGVARCHAR",
      "ROLES" : "LONGVARCHAR",
      "ALIASES" : "LONGVARCHAR",
      "OTHERS" : "LONGVARCHAR",
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
