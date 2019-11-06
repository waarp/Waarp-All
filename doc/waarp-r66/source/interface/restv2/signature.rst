Signature des requêtes
######################

Pour s'assurer que le contenu d'une requête n'est pas modifié lors de son acheminement, les requêtes peuvent
être signées avec un hash de leur contenu.
La signature de requête nécessite que l'authentification des requêtes soit également activée sur le serveur.

La signature des requêtes se fait avec les entêtes suivants :

* **X-Auth-Signature** - Cet entête contient un hash du contenu de la requête. Ce hash est obtenu avec
  l'algorithme de hash *HMAC SHA256* en utilisant la clé de signature REST du serveur.
  La chaîne de caractères originale est la concaténation des éléments suivants :
  
    * les identifiants de l'utilisateur (i.e. l'entête *Authorization*)
    * le corps de la requête
    * l'URI de la requête
    * la méthode HTTP de la requête
  
  Cela permet de s'assurer qu'aucun de ces éléments n'a été altéré durant sa transmission.
  
  * *exemple* : Pour l'utilisateur 'toto' avec le mot de passe 'totomdp' envoyant une requête 'PUT' sur
    l'URI '/v2/hosts/serveur1' avec le contenu 
    
    .. code-block:: json
        
        { "port": 8080 }
        
    cela donne 
    
    -> ``Basic toto:totomdp{ "port": 8080 }/v2/hosts/serveur1PUT`` avant encodage et hachage

    -> ``Basic dG90bzp0b3RvbWRw{ "port": 8080 }/v2/hosts/serveur1PUT`` après encodage
    
    -> ``4dd8bfd9c1c537dbe67ce1572ceac0c18fbce70b2d08100ebd1fe773c32573dd`` après hachage
