SSL
###

 * `server.xml`: Obligatoire si <usessl> du groupe <server> est `True`
 * `client.xml`: Optionel

.. option:: keypath

   *JKS-File*

   JKS KeyStore pour les connections R66 chiffrées (Contient le certificat serveur)

.. option:: keystorepass

   *String*

   Mot de passe du KeyStore <keypath>

.. option:: keypass

   *String*

   Mot de passe du certificat du KeyStore <keypath>

.. option:: trustkeypath

   *JKS-File*

   JKS TrustStore pour les connections R66 chiffrées (Contient les certificats clients)

.. option:: trustkeystorepass

   *String*

   Mot de passe du TrustStore <trustkeypath>

.. option:: trustuseclientauthenticate

   *Boolean* (False)

   Si vrai R66 n'authorise les clients que via l'authentification SSL
