.. _setup-monitor:

Configuration du Monitoring en mode PUSH HTTP(S) REST
#####################################################

.. versionadded:: 3.6.0

Description générale
--------------------

Le moniteur en mode export REST JSON des états des transferts permet
d'envoyer en mode POST vers un service REST HTTP(S) de son choix
l'état des transferts à intervalles réguliers.

Seuls les transferts ayant subi un changement sont envoyés.

Le format du JSON est comme suit :

.. code-block:: json

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

.. code-block:: xml

    <pushMonitor>
      <url>http://127.0.0.1:8999</url>
      <endpoint>/log</endpoint>
      <delay>1000</delay>
      <keepconnection>True</keepconnection>
      <intervalincluded>True</intervalincluded>
      <transformlongasstring>False</transformlongasstring>
    </pushMonitor>

Description des paramètres :

- ``url`` indique l'URL de base du service REST HTTP(S) distant
- ``endpoint`` indique l'extension URI du service REST HTTP(S) distant

  - Ainsi, pour l'exemple, l'URI complète sera ``http://127.0.0.1:8999/log``

- ``delay`` indique le délai en ms entre deux vérifications pour récupérer les
  transferts dont l'information aurait changée. Par défaut, la valeur est de ``1000`` ms.
- ``keepconnection`` Si « True », la connexion HTTP(S) sera en Keep-Alive
  (pas de réouverture sauf si le serveur la ferme), sinon la connexion sera réinitialisée
  pour chaque appel

  - Avec la valeur ``True``, les performances sont améliorées en évitant les reconnexions.

- ``intervalincluded`` indique si les informations de l'intervalle utilisé seront fournies
- ``transformlongasstring`` indique si les nombres « long » seront convertis en chaîne de caractères,
  sinon ils seront numériques (certaines API REST ne supportent pas des long sur 64 bits)

  - Utile notamment avec ELK car les nombres longs (identifiant unique) sont trop long lors du parsing et sont
    tronqués.

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


Exemple de configuration d'un Logstash
""""""""""""""""""""""""""""""""""""""

Il est possible par exemple de router vers un service Logstash les logs JSON ainsi
produits.

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
