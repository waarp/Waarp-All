###########
Généralités
###########

Authentification des requêtes
=============================

L'authentification des requêtes doit être activée dans la configuration du
serveur Waarp R66 (valeur de la balise ``restauthenticated`` à ``true``).

Si la requête doit être authentifiée, le nom de l'utilisateur doit être ajouté à
la requête en définissant l'en-tête HTTP :http:header:`X-Auth-User`.

.. note::

   Le mot de passe n'est jamais transmis en clair dans la requête, mais sert à
   générer la :ref:`signature de la requête <rest-request-signature>`.

   Pour plus de sécurité, il vaut mieux activer la signature quand les requêtes
   sont authentifiées.



.. _rest-request-signature:

Signature des requêtes
======================

La signature des requêtes REST permet de s'assurer que les requêtes n'ont pas
été modifiées entre l'envoi et l'émission du message http (*man in the middle*)
en transmettant une signature cryptographique de la requête.

La signature des requêtes doit être activée dans la configuration du serveur
Waarp R66 (valeur de la balise ``restsignature`` à ``true``).

Si la requête doit être signée, la signature doit être ajoutée à
la requête en définissant l'en-tête HTTP :http:header:`X-Auth-Key`.
La valeur de cet en-tête est calculé selon l'algorithme décrit ci-dessous.

Calcul de la signature
----------------------

La signature est calculée à partir de du chemin et de la chaîne de requête de l'URL
requêtée selon l'algorithme suivant :

1. Tous les arguments de la chaîne de requête (*query string*), auxquels sont
   ajoutés les entêtes de ``X-Auth-Timestamp`` et ``X-Auth-User`` si ceux-ci
   sont utilisés, sont convertis en minuscules et triés dans l'ordre alphabétique
   des identifiants pour reconstruire une nouvelle chaine de requête.
2. Le paramètre ``X-Auth-InternalKey`` est ajouté à la chaîne de requête
   obtenue à l'étape précédente, avec pour valeur le mot de passe de
   l'utilisateur.
3. La chaîne de caractère utilisée pour le calcul de la signature est construite
   en concaténant le chemin de l'url et la chaîne de requête obtenue
   précédemment.
4. Enfin, la signature est calculée en appliquant la fonction HMAC-SHA256 à la
   chaîne de caractère obtenue à l'étape précédente avec
   la clef secrète du serveur (définie dans la balise ``restsigkey`` de sa
   configuration). C'est la représentation hexadécimale de la signature qui est
   conservée.

**Exemple**

On effectue une requête pour exporter l'historique des transferts terminés vers
un fichier XML. La requête est envoyée à l'URL ``http://127.0.0.1:8088/log`` avec
l'utilisateur ``adminuser`` (mot de passe : ``adminpass``). La conficuration du
serveur demande à ce que la requête soit horodatée.

La chaîne de requete utilisée pour le calcul de la signature sera donc
``x-auth-user=adminuser&x-auth-timestamp=2017-04-12T23:20:50.52Z``.

Après ajout du mot de passe (étape 2), elle devient
``x-auth-user=adminuser&x-auth-timestamp=2017-04-12T23:20:50.52Z&X-Auth-InternalKey=adminpass``.

La chaine de caractère complète utilisée pour le calcul de la signature est donc
``/log?x-auth-user=adminuser&x-auth-timestamp=2017-04-12T23:20:50.52Z&X-Auth-InternalKey=adminpass``.

Enfin, c'est la représentation hexadécimale du hash HMAC-SHA256 appliqué à cette
chaîne de caractère qui est utilisée comme signature.


Clef de signature
-----------------

La clef utilisée pour la génération de la signature est stockée dans un fichier
dont le chemin doit être renseigné dans la balise ``restsigkey`` du fichier de
configuration du serveur.

Il s'agit d'une séquence aléatoire de 32 octets. Sous Linux, elle peut être
générée avec la commande suivante :

.. code-block:: sh

   # Pour afficher une clef
   head -b 32 /dev/urandom

   # Pour générer directement le fichier
   head -b 32 /dev/urandom > path/to/restsigning.key



Horodatage des requêtes
=======================

Pour augmenter la sécurité de l'API, un serveur peut demander à ce que les
requêtes soient horodatées.

L'horodatage des requêtes doit être activé dans la configuration du
serveur Waarp R66 (valeur de la balise ``resttimelimit`` supérieur à ``0``).

La valeur de la balise ``resttimelimit`` représente la durée de validité de la
requête : si l'écart entre l'heure du serveur Waarp R66 au moment de la
recéption de la requête et la date contenue dans la requête est supérieur à
``resttimelimit``, la requête est refusée.

Si la requête doit être horodatée, le date et l'heure doivent être ajoutées à
la requête en définissant l'en-tête HTTP :http:header:`X-Timestamp`. La date
doit être au format :rfc:`3339` (exemple: ``2018-03-22T16:00:05.352Z``).

