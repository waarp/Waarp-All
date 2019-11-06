Identity
########

 * `server.xml`: Obligatoire
 * `client.xml`: Obligatoire

.. option:: hostid

   *String* Obligatoire si <server.usenossl> est `True`

   Hostid utilisé dans les communication en clair

.. option:: hostidssl

   *String* Obligatoire si <server.usessl> est `True`

   Hostid utilisé dans les communication chiffrée

.. option:: cryptokey

   *String* Obligatoire

   Fichier de la CryptoKey Des utilisées pour le chiffrage des mots de passe R66
