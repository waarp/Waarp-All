DB
##

 * `server.xml`: Obligatoire
 * `client.xml`: Obligatoire si le moniteur utilise une base de données

.. option:: dbdriver

   *String* Obligatoire

   Les bases oracle, mysql, postgresql et h2 sont supportées

.. option:: dbserver

   *String* Obligatoire

   Connection en mode JDBC à la base de données (jdbc:type://[host:port]....). 
   Veuillez vous reférer à la documention de la base de données utiliée

.. option:: dbuser

   *String* Obligatoire

   Utilisateur de la base de données

.. option:: dbpasswd

   *String* Obligatoire

   Mot de passe de la base de données
