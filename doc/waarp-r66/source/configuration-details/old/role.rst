Role
####
 * `server.xml`: 

Depuis la versions 2.4.9, les balises <role> permettent de remplacé le role d'un hôte 
en base de données par celui précisé dans la configuration.
Par défault le serveur Local doit avoir le role FULLADMIN

.. option:: roleid

   String

   Les id des hôtes dont le rôle en base de données est remplacé

.. option:: roleset

   *String*

   Le role assigné à l'hôte: NOACCESS, READONLY, TRANSFER, RULE, HOST, LIMIT, SYSTEM, LOGCONTROL,
   PARTNER (READONLY, TRANSFER), CONFIGADMIN(PARTNER, RULE, HOST),
   FULLADMIN (CONFIGADMIN, LIMIT, SYSTEM, LOGCONTROL)


.. code-block:: xml

  ...
  <role>
    <roleid> </roleid>
    <roleset>NOACCESS</roleset>
  </role>
  <role>
    <roleid> </roleid>
    <roleset>TRANSFER|RULE|HOST</roleset>
  </role>
  ...