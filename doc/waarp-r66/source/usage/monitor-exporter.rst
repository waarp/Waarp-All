.. _setup-monitor:

Configuration du Monitoring en mode PUSH HTTP(S) REST ou vers un service Elasticsearch
######################################################################################

.. versionadded:: 3.6.0

Description générale
--------------------

Le moniteur en mode export REST JSON des états des transferts permet
d'envoyer en mode POST vers un service REST HTTP(S) de son choix
l'état des transferts à intervalles réguliers.

Il permet au choix de le faire vers une simple API REST ou vers un
serveur Elasticsearch. La configuration est différente mais le
fonctionnement est globalement le même.

Seuls les transferts ayant subi un changement sont envoyés.

Le format du JSON est comme suit :

.. code-block::

    {
      "results": [                            # Array of Transfer information
        {
          "specialId": 12345,                     # Id as Long (-2^63 to 2^63 - 1)
          "uniqueId": "owner.requester.requested.specialId", # Unique global Id
          "hostId": "R66Owner",                   # R66 Owner (Server name)
          "globalStep": "step",                   # Global Current Step
          "globalLastStep": "laststep",           # Global Last Step previous Current
          "step": 1,                              # Current Step in Global Current Step
          "rank": 123,                            # Current Rank in transfer step
          "status": "status",                     # Current status
          "stepStatus": "stepstatus",             # Status of previous Step
          "originalFilename": "originalFilename", # Original Filename
          "originalSize": 123456,                 # Original file size
          "filename": "filename",                 # Resolved local filename
          "ruleName": "ruleName",                 # Rule name
          "blockSize": 123,                       # Block size during transfer
          "fileInfo": "fileInfo",                 # File information, containing associated file transfer information
          "followId": 123456,                     # Follow Id as Long (-2^63 to 2^63 - 1)
          "transferInfo": "transferInfo as Json", # Transfer internal information as Json String
          "start": "2021-03-28T11:55:15Z",        # Start date time of the transfer operation
          "stop": "2021-03-28T11:58:32Z",         # Current last date time event of the transfer operation
          "requested": "requested",               # Requested R66 hostname
          "requester": "requester",               # Requester R66 hostname
          "retrieve": true,                       # True if the request is a Pull, False if it is a Push
          "errorCode": "errorCode",               # Code of error as one char
          "errorMessage": "errorMessage",         # String message of current Error
          "waarpMonitor": {                       # Extra information for indexing if necessary
            "from": "2021-03-28T11:58:15Z",       # filter from (could be empty if none)
            "to": "2021-03-28T11:59:15Z",         # filter to
            "index": "r66owner"                   # R66 Hostname lowercase
          }
        },
        ...
      ]
    }


Configuration
-------------
Une seule configuration est possible. Si ``index`` est positionné, il s'agit d'une configuration pour
Elasticsearch, sinon pour API REST.

Pour un POST sur une API REST :

.. code-block:: xml

    <pushMonitor>
      <url>http://127.0.0.1:8999</url>
      <endpoint>/log</endpoint>
      <delay>1000</delay>
      <basicAuthent>basicAuthent</basicAuthent>
      <token>token</token>
      <apiKey>apiKey</apiKey>
      <keepconnection>True</keepconnection>
      <intervalincluded>True</intervalincluded>
      <transformlongasstring>False</transformlongasstring>
    </pushMonitor>

Description des paramètres :

- ``url`` indique l'URL de base du service REST HTTP(S) distant
- ``endpoint`` indique l'extension URI du service REST HTTP(S) distant

  - Ainsi, pour l'exemple, l'URI complète sera ``http://127.0.0.1:8999/log``
  - Si HTTPS est utilisé, le KeyStore et TrustStore par défaut de Waarp seront utilisés

- ``delay`` indique le délai en ms entre deux vérifications pour récupérer les
  transferts dont l'information aurait changée. Par défaut, la valeur est de ``1000`` ms. La valeur
  minimale est de 500ms.
- ``keepconnection`` Si « True », la connexion HTTP(S) sera en Keep-Alive
  (pas de réouverture sauf si le serveur la ferme), sinon la connexion sera réinitialisée
  pour chaque appel (défaut: False)

  - Avec la valeur ``True``, les performances sont améliorées en évitant les reconnexions.

- ``intervalincluded`` indique si les informations de l'intervalle utilisé seront fournies (défaut: True)
- ``transformlongasstring`` indique si les nombres « long » seront convertis en chaîne de caractères,
  sinon ils seront numériques (certaines API REST ne supportent pas des long sur 64 bits) (défaut: True)

  - Utile notamment avec ELK car les nombres longs (identifiant unique) sont trop long lors du parsing et sont
    tronqués.

- Si une authentification est nécessaire, plusieurs options sont possibles (unique) :

  - Authentification Basic : ``basicAuthent`` contient l'authentification Basic au format Base 64
  - Bearer Token : ``token`` contenant le token d'accès
  - ApiKey : ``apiKey`` contenant la clef d'API sous la forme ``apiId:apiKey``

Pour une indexation par Bulk sur Elasticsearch :

.. code-block:: xml

    <pushMonitor>
      <url>http://127.0.0.1:8999</url>
      <prefix>/pathPrefix</prefix>
      <delay>1000</delay>
      <index>indexName</index>
      <username>username</username><paswd>password</passwd>
      <token>token</token>
      <apiKey>apiKey</apiKey>
      <intervalincluded>True</intervalincluded>
      <transformlongasstring>False</transformlongasstring>
      <compression>True</compression>
    </pushMonitor>

Description des paramètres :

- ``url`` indique l'URL de base du service REST HTTP(S) distant ; plusieurs url sont possibles, séparées
  par ','
- ``prefix`` indique un prefix à ajouter à chaque requête, notamment si Elasticsearch est derrière un Proxy
  (non obligatoire)
- ``delay`` indique le délai en ms entre deux vérifications pour récupérer les
  transferts dont l'information aurait changée. Par défaut, la valeur est de ``1000`` ms. La valeur
  minimale est de 500ms.
- ``intervalincluded`` indique si les informations de l'intervalle utilisé seront fournies (défaut: True)
- ``transformlongasstring`` indique si les nombres « long » seront convertis en chaîne de caractères,
  sinon ils seront numériques (certaines API REST ne supportent pas des long sur 64 bits) (défaut: False)

  - Utile notamment avec ELK car les nombres longs (identifiant unique) sont trop long lors du parsing et sont
    tronqués.

- ``index`` contient le nom de l'index. Des substitutions sont possibles pour avoir de multiples index :

  - ``%%WAARPHOST%%`` remplacé par le nom du serveur R66
  - ``%%DATETIME%%`` remplacé par la date au format ``YYYY.MM.dd.HH.mm``
  - ``%%DATEHOUR%%`` remplacé par la date au format ``YYYY.MM.dd.HH``
  - ``%%DATE%%`` remplacé par la date au format ``YYYY.MM.dd``
  - ``%%YEARMONTH%%`` remplacé par la date au format ``YYYY.MM``
  - ``%%YEAR%%`` remplacé par la date au format ``YYYY``
  - La date considérée est la date lors du dernier déclenchement du monitoring
  - Le nom de l'index sera en minuscule, quelque soit la casse d'origine (exigence Elasticsearch)
  - Ainsi ``waarpR66-%%WAARPHOST%%-%%DATE%%`` donnerait
    ``waarpr66-hosta-2021-06-21``

- Si une authentification est nécessaire, plusieurs options sont possibles (unique) :

  - Authentification Basic : ``username`` et ``paswd`` contienent l'authentification Basic
  - Bearer Token : ``token`` contenant le token d'accès
  - ApiKey : ``apiKey`` contenant la clef d'API sous la forme ``apiId:apiKey``

- ``compression`` spécifie si les transferts d'information vers Elasticsearch utiliseront
  la compression (``True``) ou pas (``False``) (défaut: ``True``)


Dernière date de vérification
"""""""""""""""""""""""""""""

A chaque transfert réussi, le moniteur met à jour la date de référence pour la
prochaine vérification dans la base dans le champ ``others`` de la configuration
du Host. Ceci permet, en cas d'arrêt du serveur, d'enregistrer le dernier état et
ainsi de limiter le nombre de possibles doublons qui seraient renvoyés lors du
redémarrage.

Si besoin, vous pouvez modifier cette valeur directement dans la base pour
refléter le timestamp à utiliser comme point de départ (``lastMonitoringDateTime``).


Cas particulier des clusters
""""""""""""""""""""""""""""

Afin de ne pas publier plusieurs fois les mêmes logs, il est recommandé
de n'activer cette option que sur un seul des membres du cluster.

Si celui-ci devait s'arrêter, la reprise à son redémarrage reprendra là où
il en était.

Si c'est un problème plus grave (le serveur physique est indisponible), vous
pouvez alors activer cette fonction en la basculant sur un autre membre du cluster.

Pour Elasticsearch
""""""""""""""""""

Si la connection est directe avec Elasticsearch (ou au travers d'un proxy),
il n'est pas besoin d'utiliser l'option ``transformlongasstring``, en la laissant
à ``False`` par défaut.

Exemple de configuration d'un Logstash
""""""""""""""""""""""""""""""""""""""

Il est possible par exemple de router vers un service Logstash les logs JSON ainsi
produits via une API REST (et non directement dans Elasticsearch).

La configuration du Logstash peut être la suivante : (avec le mode ``transformlongasstring`` as True)

.. code-block:: text

  # Waarp R66 -> Logstash -> Elasticsearch pipeline.
  input {
    http {
      # default: 0.0.0.0
      host => "0.0.0.0"
      ssl => false
      # default: 8080
      port => 5044
      type => "r66json"
    }
  }

  filter {
    if [type] == "r66json" {
      # Split from array resuts
      if !("splitted" in [tags]) {
        split {
           field => "results"
           add_tag => ["splitted"]
        }
      }
      if ("splitted" in [tags]) {
        # Move to root
        ruby {
          code => "
              event.get('results').each {|k, v|
                  event.set(k, v)
              }
              event.remove('results')
          "
        }
        # Discover extra Json field
        # Change Date String as DateTime
        date {
          match => [ "start", "ISO8601" ]
          target => "start"
        }
        date {
          match => [ "stop", "ISO8601" ]
          target => "stop"
        }
        date {
          match => [ "[waarpMonitor][from]", "ISO8601" ]
          target => "[waarpMonitor][from]"
        }
        date {
          match => [ "[waarpMonitor][to]", "ISO8601" ]
          target => "[waarpMonitor][to]"
        }
        # Create index name : %{[logInfo][level]}
        mutate {
          add_field => { "[@metadata][target_index]" => "waarpr66-%{[waarpMonitor][index]}-%{+YYYY.MM.dd}" }
        }
        # Remove headers from HTTP request and extra fields
        mutate {
          remove_field => [ "headers", "host", "sort", "tags", "@version" ]
        }
      }
    }
  }

  output {
    if "r66json" in [type] {
      elasticsearch {
        hosts => ["http://127.0.0.1:9200"]
        index => "%{[@metadata][target_index]}"
        document_id => "%{uniqueId}"
        doc_as_upsert => true
        #user => "elastic"
        #password => "changeme"
      }
    }
    # Debug mode file and output
  #  file {
  #    path => "/tmp/logstash-R66.log"
  #  }
  #  stdout{
  #    codec => rubydebug
  #  }
  }

On Elastic, the mapping shall be defined to ensure correct type:


- waarpMonitor.to Date
- waarpMonitor.from Date
- stop Date
- start Date
- specialId string
- followId string
- originalSize string
- hostId string
- waarpMonitor.index string
- blockSize int
- errorMessage string
- filename string
- type string
- stepStatus string
- transferInfo string
- originalFilename string
- requester string
- globalStep string
- ruleName string
- requested string
- fileInfo string
- status string
- errorCode int
- retrieve boolean
- globalLastStep string
- step int
- rank long
- uniqueId string
