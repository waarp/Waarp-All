REST
####

 * `server.xml`: 

.. option:: restaddress

   Address

   Adresse de l'interface REST du serveur

.. option:: serverrestport

   Integer (8068) 

   Port utilisé pour l'interface REST

.. option:: restssl

   Boolean (False)

   Vrai si l'interface REST utilise un canal chiffré

.. option:: restauthenticated 

   Boolean (False)

   Vrai si l'interface REST utilise une authentification

.. option:: resttimelimit

   Long (-1)

   Temps limite de l'interface REST, une valeur négative ne désactive la limitation

.. option:: restsignature

   Boolean (True)

   Vrai si l'interface REST utilise une signature à la demande

.. option:: restsigkey

   Path

   Le fichier clef SHA 256 de signature de l'interface REST


Il est possible de définir les droits CRUD accessiblent via l'interface REST d'une ressource.
Cela est fait via les blocs `<restmethod>` composé de:

.. option:: restname

   String

   Quelle ressource est accessible (All ou DbHostAuth, DbRule, DbTaskRunner, 
   DbHostConfiguration, DbConfiguration, Bandwidth, Business, Config, n, Log, Server, Control)

.. option:: restcrud

   String

   Quelles operations sont authorisées: CRUD (Create Read Update Delete), par exemple R ou CRU ou CRUD*

.. code-block:: xml

   <rest>
      ...
      <restmethod>
        <restname>ressourceA</restname>
        <restcrud>CRUD</restcrud>
      </restmethod>
      <restmethod>
        <restname>ressourceB</restname>
        <restcrud>R</restcrud>
      </restmethod>
      ...
   </rest>