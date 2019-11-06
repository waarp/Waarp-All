.. _rest-log:

#######################
Gestion de l'historique
#######################

Export et purge de l'historique
===============================

.. http:get:: /log

   Ce point d'entrée permet d’exporter l’historique de transfert du serveur
   Waarp R66 associé au client, et le cas échéant de purger l’historique.

   Les fichiers XML produit sont déposés dans le dossier arch définitions dans
   la configuration du serveur (le chemin complet est fourni dans la réponse de
   la requête).

   **Paramètres de la requête**

   Le corps de la réponse doit être un objet JSON valide. Celui-ci continet
   plusieurs groupes de paramètres.

   Les paramètres ``@class`` et ``requestUserPacket`` sont obligatoires et leur
   valeur est fixe (voir ci-dessous).

   Le second groupe de paramètres permet de filtrer l'historique exporté par
   date, statut, identifiant, règle de transfert et/ou partenaire.

   Enfin, deux paramètres permettent d'effectuer des opérations de maintenance
   conjointement à l'export : ``purge``, qui supprime de la base de données
   l'historique exporté, et ``clean``, qui corrige le statut de transferts
   erronés quand celui-ci est erroné.


   :<json string @class: Le type de requête. Doit être
                         ``org.waarp.openr66.protocol.localhandler.packet.json.LogJsonPacket``
   :<json string comment: Un commentaire optionnel
   :<json int requestUserPacket: Le type de requête. Doit être ``16``
   :<json bool statuspending: Exporte les transferts en attente (défaut :``false``)
   :<json bool statustransfer: Exporte les transferts en cours(défaut: ``false``) 
   :<json bool statusdone: Exporte les transferts terminés (défaut: ``false``)
   :<json bool statuserror: Exporte les transferts en erreur (défaut: ``false``)
   :<json string rule: Limite l’export à une règle spécifique
   :<json int string request: Corrige le statut des transferts terminés erronés
   :<json int start: Exporte seulement l’historique postérieur à cette date.
                     La date doit être fournie sous la forme d'un timestamp
                     Unix *en millisecondes*.
   :<json int stop: Exporte seulement l’historique postérieur à cette date.
                    La date doit être fournie sous la forme d'un timestamp
                    Unix *en millisecondes*.
   :<json int startid: Valeur minimale d’identifiants de transfert à exporter
   :<json int stopid: Valeur maximale d’identifiants de transfert à exporter
   :<json bool purge: Si ``true``, l'historique exporté est également purgé de
                      la base de données (defaut: ``false``)
   :<json bool clean: Corrige le statut des transferts terminés erronés 
                       (defaut: ``false``)
    

   **Détails de la réponse**

   La réponse contient le statut de la requête, ainsi que de nombreuses données
   récapitulant la requête.

   Les éléments les plus significatifs de la réponse sont les suivants :

   :>json string answer.results.0.filename: Le chemin complet du fichier
                                            contenant les données exportées
   :>json int answer.results.0.exported: Le nombre de transferts exportés
   :>json int answer.results.0.purged: Le nombre de transferts purgés
   :>json string message: Statut de la requête comme texte (``OK`` signifie un
                          succès)
   :>json int code: Statut de la requête comme code réponse HTTP
   :>json string details: En cas d'erreur (code retour différent de ``200``, un
                          message expliquant la cause de l'erreeur

   **Codes retours**

   Les requêtes vers ce point d'entrée peuvent avoir les code retour HTTP
   suivants. En cas d'erreur, les détails peuvent se retrouver dans le champ
   ``details`` de la réponse. 

   :statuscode 200: Succès 
   :statuscode 400: Une erreur est présente dans le corps de la requête
   :statuscode 401: Authentification invalide pour l'utilisateur
   :statuscode 405: Le point d'entrée est désactivé pour ce serveur

   **Exemple de requête** 

   .. code-block:: http

      GET /log HTTP/1.1
      Content-Type: application/json

      {
         "@class": "org.waarp.openr66.protocol.localhandler.packet.json.LogJsonPacket",
         "requestUserPacket": 16,
         "purge": false,
         "stop": 1399760601400
      }

   **Exemple de réponse (succès)** 
   
   .. code-block:: http
      
      HTTP/1.1 200 OK
      content-type: application/json

      {
         "X-method":"GET",
         "path":"/log",
         "base":"log",
         "uri":{},
         "answer":{
            "@model":"Log",
            "results":[{
               "@class":"org.waarp.openr66.protocol.localhandler.packet.json.LogResponseJsonPacket",
               "comment":null,
               "requestUserPacket":16,
               "purge":false,
               "clean":false,
               "statuspending":false,
               "statustransfer":false,
               "statusdone":false,
               "statuserror":false,
               "rule":null,
               "request":null,
               "start":null,
               "stop":1399760601400,
               "startid":null,
               "stopid":null,
               "command":16,
               "filename":"[...]/data/server1/arch/server1_1521715697441_runners.xml",
               "exported":0,
               "purged":0
            }]
         },
         "command":"GetLog",
         "message":"OK",
         "code":200
      }
      
   **Exemple de réponse (erreur)** 
   
   .. code-block:: http

      HTTP/1.1 400 Bad Request
      content-type: application/json

      {
         "code": 400,
         "detail": "com.fasterxml.jackson.databind.JsonMappingException:
             Unexpected token (END_OBJECT), expected FIELD_NAME: missing
             property '@class' that is to contain type id  (for class
             org.waarp.openr66.protocol.localhandler.packet.json.JsonPacket)\n
             at [Source: {\"class\":
             \"org.waarp.openr66.protocol.localhandler.packet.json.LogJsonPacket\",
             \"requestUserPacket\": 16, \"purge\": true, \"stop\":
             1399760601400}; line: 1, column: 141]",
         "message": "Bad Request"
      }

