Exemples
########

Serveur
*******

**Pour démarer le serveur server1**

.. code-block:: bash

  waarpr66-server server1 start

**Arrêter le serveur**

Pour arrêter le serveur server1:

.. code-block:: bash

  waarpr66-server server1 stop


Client
******

**Envoie d'un fichier test.file du client client1 au serveur server2 en utilisant la règle default**

.. code-block:: bash

  waarpr66-client client1 send -file test.file -to server2 -rule default

**Envoie des fichier test.file et file.test du client2 au serveur server1 en utilisant la règle prezip en utilisant des blocks de 64b**

.. code-block:: bash

  waarpr66-client client2 msend -file test.file,file.test -o server1 -rule prezip -block 64


**Reprise du transfert id 4 de client3 a serveur8**

.. code-block:: bash

  waarpR66-client client3 send -to server8 -id 4

