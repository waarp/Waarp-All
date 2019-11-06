####################################
Purge de l'historique des transferts
####################################

.. todo:: purge par ecran admin

En ligne de commande
====================

Pour purger l'historique des transferts en ligne de commande, il faut executer
la :ref:`commande d'export de l'historique <r66client-log-export>` avec
l'argument ``-purge``.

Tous les arguments de la commande d'export peuvent s'appliquer pour filtrer la
purge.

Par exemple, pour purger de l'historique les transferts datant d'avant le  
1er juin 2018, la commande suivante peut être utilisée :

.. code-block:: sh

   # Installation avec les packages
   waarp-r66client HOSTID log-export -purge -stop 201806010000

   # installation avec les archives portables
   ./bin/waarp-r66client.sh HOSTID log-export -purge -stop 201806010000

L'historique est eporté en XML dans le dossier ``arch`` de l'instance avant
d'être définitivement purgé de la base de données.

Avec l'API REST
===============

L'API REST de Waarp R66 expose un point d'entrée qui permet d'exporter et de
purger l'historique de transfert.

.. seealso::
 
   - :ref:`La documentation l'API <rest-log>`

Par exemple, pour purger de l'historique les transferts datant d'avant le  
1er juin 2018, la commande suivante peut être utilisée :

.. code-block:: sh

   $ cat <<EOT | curl https://[IP SERVEUR]:8088/log -X GET -d @-
   {
      "@class": "org.waarp.openr66.protocol.localhandler.packet.json.LogJsonPacket", 
      "requestUserPacket": 16, 
      "purge": true,  
      "stop": 1527804000
   }
   EOT

Réponse du serveur pour cet exemple :

.. code-block:: json

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
            "purge":true,
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
            "filename":"data/server1/arch/server1_1521651615878_runners.xml",
            "exported":0,
            "purged":0
            }]
         },
         "command":"GetLog",
         "message":"OK",
         "code":200
      }
