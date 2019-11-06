authent
#######

.. option:: entry

   Permet d'initialisé les hôtes distants 
   Used to initialize remote Hosts table at setup or with client with no database support

.. option:: hostid

   *String* Obligatoire
   
   Hostid de l'hôte distant

.. option:: address

   *Address* Obligatoire
   
   Addresse de l'hôte distant (IP ou entrée DNS)

.. option:: port

   *Integer* Obligatoire
   
   Port associé à l'addresse de l'hôte distant

.. option:: isssl

   *Boolean* (False)
   
   Vrai si l'entrée utilise des communications chiffrées

.. option:: admin

   *Boolean* (False)
   
   Vrai si l'entrée authorise les accès Administrateur via le protocol R66.
   A partir de la version 2.4.9 cela peut être remplacé si par la configuration fichier des rôles

.. option:: isclient

   *Boolean* (False)
   
   Vrai si l'entrée est un client

.. option:: isactive

   *Boolean* (False)
   
   Vrai si l'entrée est active/authorisée

.. option:: isproxified

   *Boolean* (False)

   Vrai si l'entrée est accesible via un proxy, désactive la vérification d'IP

.. option:: key

   *nonEmptyString*

   Mot de passe de l'entrée (Redondant avec <keyfile>)

.. option:: keyfile 
   
   *GGP-File*
                       
   Fichier contentnat le mot de passe de l'entrée (Redondant avec <key>)

