/rules
######

GET /rules
**********

Réponse
=======

Modèle
------

  ================== =======================
  Champs             Type
  ================== =======================
                     Array[DbRule]
  ================== =======================

  *DbRule*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  HOSTIDS            string
  MODETRANS          integer
  RECVPATH           string
  SENDPATH           string
  ARCHIVEPATH        string
  WORKPATH           string
  RPRETASKS          string
  RPOSTTASKS         string
  RERRORTASKS        string
  SPRETASKS          string
  SPOSTTASKS         string
  SERRORTASKS        string
  UPDATEDINFO        integer
  IDRULE             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    [{
      "HOSTIDS" : "LONGVARCHAR",
      "MODETRANS" : "INTEGER",
      "RECVPATH" : "VARCHAR",
      "SENDPATH" : "VARCHAR",
      "ARCHIVEPATH" : "VARCHAR",
      "WORKPATH" : "VARCHAR",
      "RPRETASKS" : "LONGVARCHAR",
      "RPOSTTASKS" : "LONGVARCHAR",
      "RERRORTASKS" : "LONGVARCHAR",
      "SPRETASKS" : "LONGVARCHAR",
      "SPOSTTASKS" : "LONGVARCHAR",
      "SERRORTASKS" : "LONGVARCHAR",
      "UPDATEDINFO" : "INTEGER",
      "IDRULE" : "VARCHAR"
    },]

Paramètres
==========

  ================== ========= ============
  Paramètre          Type      Description
  ================== ========= ============
  "IDRULE"                     rule name
  "MODETRANS"                  MODETRANS value
  ================== ========= ============

GET /rules/:id
**************

Réponse
=======

Modèle
------

  *DbRule*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  HOSTIDS            string
  MODETRANS          integer
  RECVPATH           string
  SENDPATH           string
  ARCHIVEPATH        string
  WORKPATH           string
  RPRETASKS          string
  RPOSTTASKS         string
  RERRORTASKS        string
  SPRETASKS          string
  SPOSTTASKS         string
  SERRORTASKS        string
  UPDATEDINFO        integer
  IDRULE             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    {
      "HOSTIDS" : "LONGVARCHAR",
      "MODETRANS" : "INTEGER",
      "RECVPATH" : "VARCHAR",
      "SENDPATH" : "VARCHAR",
      "ARCHIVEPATH" : "VARCHAR",
      "WORKPATH" : "VARCHAR",
      "RPRETASKS" : "LONGVARCHAR",
      "RPOSTTASKS" : "LONGVARCHAR",
      "RERRORTASKS" : "LONGVARCHAR",
      "SPRETASKS" : "LONGVARCHAR",
      "SPOSTTASKS" : "LONGVARCHAR",
      "SERRORTASKS" : "LONGVARCHAR",
      "UPDATEDINFO" : "INTEGER",
      "IDRULE" : "VARCHAR"
    }

Paramètres
==========

  ================== ========= ============
  Paramètre          Type      Description
  ================== ========= ============
  IDRULE             string    RuleId in URI as rules/id
  ================== ========= ============


POST /rules
***********

Réponse
=======

Modèle
------

  *DbRule*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  HOSTIDS            string
  MODETRANS          integer
  RECVPATH           string
  SENDPATH           string
  ARCHIVEPATH        string
  WORKPATH           string
  RPRETASKS          string
  RPOSTTASKS         string
  RERRORTASKS        string
  SPRETASKS          string
  SPOSTTASKS         string
  SERRORTASKS        string
  UPDATEDINFO        integer
  IDRULE             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    {
      "HOSTIDS" : "LONGVARCHAR",
      "MODETRANS" : "INTEGER",
      "RECVPATH" : "VARCHAR",
      "SENDPATH" : "VARCHAR",
      "ARCHIVEPATH" : "VARCHAR",
      "WORKPATH" : "VARCHAR",
      "RPRETASKS" : "LONGVARCHAR",
      "RPOSTTASKS" : "LONGVARCHAR",
      "RERRORTASKS" : "LONGVARCHAR",
      "SPRETASKS" : "LONGVARCHAR",
      "SPOSTTASKS" : "LONGVARCHAR",
      "SERRORTASKS" : "LONGVARCHAR",
      "UPDATEDINFO" : "INTEGER",
      "IDRULE" : "VARCHAR"
    }

Paramètres
==========

  ================== ========= ============
  Paramètre          Type      Description
  ================== ========= ============
  HOSTIDS"           string
  MODETRANS          integer
  RECVPATH           string
  SENDPATH           string
  ARCHIVEPATH        string
  WORKPATH           string
  RPRETASKS          string
  RPOSTTASKS         string
  RERRORTASKS        string
  SPRETASKS          string
  SPOSTTASKS         string
  SERRORTASKS        string
  UPDATEDINFO        integer
  ================== ========= ============

PUT /rules/:id
**************

Réponse
=======

Modèle
------

  *DbRule*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  HOSTIDS            string
  MODETRANS          integer
  RECVPATH           string
  SENDPATH           string
  ARCHIVEPATH        string
  WORKPATH           string
  RPRETASKS          string
  RPOSTTASKS         string
  RERRORTASKS        string
  SPRETASKS          string
  SPOSTTASKS         string
  SERRORTASKS        string
  UPDATEDINFO        integer
  IDRULE             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    {
      "HOSTIDS" : "LONGVARCHAR",
      "MODETRANS" : "INTEGER",
      "RECVPATH" : "VARCHAR",
      "SENDPATH" : "VARCHAR",
      "ARCHIVEPATH" : "VARCHAR",
      "WORKPATH" : "VARCHAR",
      "RPRETASKS" : "LONGVARCHAR",
      "RPOSTTASKS" : "LONGVARCHAR",
      "RERRORTASKS" : "LONGVARCHAR",
      "SPRETASKS" : "LONGVARCHAR",
      "SPOSTTASKS" : "LONGVARCHAR",
      "SERRORTASKS" : "LONGVARCHAR",
      "UPDATEDINFO" : "INTEGER",
      "IDRULE" : "VARCHAR"
    }

Paramètres
==========

  ================== ========= ============
  Paramètre          Type      Description
  ================== ========= ============
  IDRULE             string    RuleId in URI as rules/id
  HOSTIDS"           string
  MODETRANS          integer
  RECVPATH           string
  SENDPATH           string
  ARCHIVEPATH        string
  WORKPATH           string
  RPRETASKS          string
  RPOSTTASKS         string
  RERRORTASKS        string
  SPRETASKS          string
  SPOSTTASKS         string
  SERRORTASKS        string
  UPDATEDINFO        integer
  ================== ========= ============

DELETE /rules/:id
*****************

Réponse
=======

Modèle
------

  *DbRule*

  ================== ========= ============
  Champs             Type      Description
  ================== ========= ============
  HOSTIDS            string
  MODETRANS          integer
  RECVPATH           string
  SENDPATH           string
  ARCHIVEPATH        string
  WORKPATH           string
  RPRETASKS          string
  RPOSTTASKS         string
  RERRORTASKS        string
  SPRETASKS          string
  SPOSTTASKS         string
  SERRORTASKS        string
  UPDATEDINFO        integer
  IDRULE             string
  ================== ========= ============

Exemple
-------

  .. code-block:: json

    {
      "HOSTIDS" : "LONGVARCHAR",
      "MODETRANS" : "INTEGER",
      "RECVPATH" : "VARCHAR",
      "SENDPATH" : "VARCHAR",
      "ARCHIVEPATH" : "VARCHAR",
      "WORKPATH" : "VARCHAR",
      "RPRETASKS" : "LONGVARCHAR",
      "RPOSTTASKS" : "LONGVARCHAR",
      "RERRORTASKS" : "LONGVARCHAR",
      "SPRETASKS" : "LONGVARCHAR",
      "SPOSTTASKS" : "LONGVARCHAR",
      "SERRORTASKS" : "LONGVARCHAR",
      "UPDATEDINFO" : "INTEGER",
      "IDRULE" : "VARCHAR"
    }

Paramètres
==========

  ================== ========= ============
  Paramètre          Type      Description
  ================== ========= ============
  IDRULE             string    RuleId in URI as rules/id
  ================== ========= ============

