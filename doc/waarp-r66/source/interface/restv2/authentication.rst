Authentification des requêtes
#############################

Lorsque l'authentification des requêtes est activée dans la configuration REST du serveur,
celle-ci peut se faire de 2 manières différentes.

Authentification Basique
========================

Une requête peut-être authentifiée avec l'authentification basique. Il s'agit de l'authentification
HTTP basique standard telle qu'elle est définie dans la `RFC 2617 <https://www.ietf.org/rfc/rfc2617.txt>`_.

Cette méthode d'authentification a l'avantage d'être simple a utilisée, mais elle n'est pas sûre sans
utilisation de SSL car les identifiants utilisateur seront envoyés en clair sur le réseau. Il n'est donc
recommandé d'utiliser cette méthode uniquement en combinaison avec HTTPS.

Cette authentification se fait avec les entêtes suivants :

* **Authorization** - Cet entête doit contenir la chaîne de caractères 'Basic', afin d'annoncer l'utilisation
  du mode d'authentification basique, suivi du nom d'utilisateur et de son mot de passe.
  Le nom d'utilisateur et le mot de passe doivent être séparés par le caractère deux-points, et le tout
  doit être encodé en Base64.
    
    * *exemple* : Pour l'utilisateur 'toto' avec le mot de passe 'totomdp' cela donne
    
      -> ``Basic toto:totomdp`` avant encodage

      -> ``Basic dG90bzp0b3RvbWRw`` après encodage
      
      
Authentification HMAC
=====================

Une requête REST peut également être authentifiée en utilisant un hash des identifiants utilisateur.

Cette méthode à l'avantage d'être sûre, même sans utilisation de SSL, mais elle nécessite que le client
ait connaissance au préalable de la clé de signature REST du serveur. Cette clé est la clé renseignée 
dans la configuration REST du serveur.

Cette authentification se fait avec les entêtes suivants :

* **X-Auth-User** - Cet entête contient le nom de l'utilisateur s'authentifiant.

* **X-Auth-Timestamp** - Cet entête contient la date (en format `ISO 8601 <https://www.w3.org/TR/NOTE-datetime>`_) 
  d'émission de la requête. Cette date est utilisée pour déterminer si la requête a expirée ou non.
  La durée de validité d'une requête est fixée dans la configuration REST du serveur.
  
* **Authorization** - Cet entête contient le hash des identifiants utilisateur. Ce hash est obtenu avec
  l'algorithme de hash *HMAC SHA256* en utilisant la clé de signature REST du serveur.
  La chaîne de caractères originale est la concaténation des éléments suivants :
  
    * la date de la requête (i.e. l'entête *X-Auth-Timestamp*)
    * le nom d'utilisateur (i.e. l'entête *X-Auth-User*)
    * le mot de passe de l'utilisateur
    
  Le hash doit être préfixé de la chaîne 'HMAC' pour spécifier l'utilisation du mode d'authentication HMAC.
  
  * *exemple* : Pour l'utilisateur 'toto' avec le mot de passe 'totomdp' à la date '1970-01-01T01:00:00+00:00'
    cela donne
    
    -> ``HMAC 1970-01-01T01:00:00+00:00totototomdp`` avant hachage
    
    -> ``HMAC e4219167eb4cf1f8590d684713218c4ad011d475d8f3b2d37fb15ce3da675021`` après hachage
