############
Référentiels
############

Référentiels communs
====================

``updatedinfo``
---------------

===== =========================
Code    Détail
===== =========================
``0`` Inconnu
``1`` Non modifié
``2`` Interrompu
``3`` Planifié
``4`` Erreur
``5`` En cours
``6`` Terminé
===== =========================


Table ``runner``
================

``globalstep`` et ``globallaststep``
------------------------------------


===== =========================
Code    Détail
===== =========================
``0`` Inconnu
``1`` Traitements pré-transfert
``2`` Transfert en cours
``3`` Traitements post-transfert
``4`` Terminé
``5`` Erreur
===== =========================


``infostatus`` et ``stepstatus``
--------------------------------

===== =========================
Code    Détail
===== =========================
``i`` Initialistation OK
``B`` Pré tâches OK
``X`` Transfert OK
``P`` Post tâches OK
``O`` Transfert complet OK
``C`` Connection en échec
``l`` Connection en échec pour cause de limitation
``A`` Mauvaise authentification
``E`` Opération externe en erreur (pre, post ou erreur)
``T`` Transfert en erreur
``M`` Transfert en erreur causé par un problème de hash MD5
``D`` Déconnexion du partenaire distant
``r`` Partenaire distant en cours d'arrêt
``F`` Action finale en erreur
``U`` Fonctionnalité non implémentée
``S`` Arrêt en cours
``R`` Erreur d'execution sur le partenaire distant
``I`` Erreur interne
``H`` Arrêt du transfert demandé
``K`` Annulation du transfert demandé
``W`` Alerte relevée lors de l'execution
``-`` Type d'erreur inconnu
``Q`` Requête déjà terminée par le partenaire distant
``s`` Requête en cours d'execution
``N`` Partenaire inconnu
``L`` Erreur car requête à soit même
``u`` Requête non trouvée sur le partenaire distant
``f`` Fichier inconnu
``c`` Commande inconnue
``p`` PassThrough demandé mais impossible
``z`` Traitement en cours
``n`` Commande erronée
``a`` Fichier non autorisé
``d`` Taille de fichier non autorisée
===== =========================
