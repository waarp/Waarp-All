Gestion des logs
################

Selon la méthode d'installation, les logs sont écrits dans le dossier 
:file:`/var/log/waarp/{HOSTID}` ou :file:`data/{HOSTID}/log`.

Les logs sont configurables dans les fichiers :file:`/etc/waarp/{HOSTID}/logback-*.xml`
ou :file:`etc/donf.d/{HOSTID}/logback-*.xml`. Ces fichiers permettent
notamment de définir l’emplacement des logs, les paramètres de rotation
des logs et le niveau de verbosité.

Le détail des options de configuration est disponible sur le site de la
`librairie logback`_.

.. _librairie logback: http://logback.qos.ch/manual/configuration.html

.. todo:: 
  
   Activation syslog
