# Note pour une nouvelle release Waarp-All

## 1  Pré-requis

Les outils suivants sont nécessaires. Des variantes peuvent être utilisées :

- Java 1.8 (les tests requièrent java 1.8, même si les modules sont compilés en
  Java 1.6), Java 1.11 pour créer la version `jre11` (attention, pour les
  versions `jre6` et `jre8`, JRE8 est nécessaire)
- Maven 3.6.3
- Docker (19 par exemple)
- Artifactory (ou équivalent) pour pré-publier les jar (via Docker sur le PC :
  https://www.jfrog.com/confluence/display/JFROG/Installing+Artifactory )
  - Note : Afin d'autoriser Maven à communiquer avec SonarQube, dans votre
    fichier `~/.m2/settings.xml`, il faut ajouter les éléments suivants :
    
    `<servers>
      <server>
        <id>centralArtifactory</id>
        <username>USER_NAME</username>
        <password>PASSWORD</password>
      </server>
    </servers>`

- SonarQube (via Docker par exemple :
  https://docs.sonarqube.org/latest/setup/get-started-2-minutes/ )
  - Note : Afin d'autoriser Maven à communiquer avec SonarQube, dans votre
    fichier `~/.m2/settings.xml`, il faut ajouter les éléments suivants :
    
    `<properties>
       <sonar.host.url>http://localhost:9000</sonar.host.url>
       <sonar.login>USER_CODE</sonar.login>
     </properties>`

    
- De préférence un IDE moderne comme IntelliJ (en version communautaire) :
  un modèle de configuration est disponible.
- Un Host Linux (Debian, Ubuntu ou Redhat ou assimilé) avec au moins 2 cœurs et
  au moins 8 Go de mémoire disponibles ainsi que 20 Go de disques (pour les
  sources et compilation) et 10 Go sur le répertoire `/tmp`

### 1.1  Installer localement les JAR de dépendances si nécessaires

Certains jars sont inclus dans le projet car ils dépendent de projets non forcément
disponibles sur tous les sites repository de Maven. Pour faciliter la compilation
et leur inclusion, il peut être nécessaire de les inclure statiquement.

Sont concernés :

- `netty-http-java6` : fork de la version netty-http mais pour JRE 6
- `selenium-java` : pour avoir une version compatible JRE 6
- `Waarp-Shaded-Parent` : le parent de ces deux jars

Pour mettre à jour ces jars, veuillez utiliser la procéduire suivante :

- Installer dans Artifactory et le depôt local `.m2` via la commande
  `mvn clean deploy -P release` depuis le projet spécifique `Waarp-Shaded-Parent`
- Copier les répertoires depuis le dépôt local `.m2` les dossiers correspondants

  - `.m2/repository/Waarp/Waarp-Shaded-Parent/1.0.3/` dans `lib/Waarp/Waarp-Shaded-Parent/1.0.3/`
  - `.m2/repository/Waarp/selenium-java/3.141.59/` dans `lib/Waarp/selenium-java/3.141.59/`
  - `.m2/repository/Waarp/netty-http-java6/1.5.0/` dans `lib/Waarp/netty-http-java6/1.5.0/`
  
- Mettre à jour dans chacun des dossiers parents le fichier `maven-metadata-local.xml` pour ne conserver
  que la dernière version (`lastest`, `release` et `version`) et la dernière date de mise à jour
  (`lastUpdated`) ; par exemple :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<metadata>
  <groupId>Waarp</groupId>
  <artifactId>Waarp-Shaded-Parent</artifactId>
  <versioning>
    <latest>1.0.3</latest>
    <release>1.0.3</release>
    <versions>
      <version>1.0.3</version>
    </versions>
    <lastUpdated>20210424111617</lastUpdated>
  </versioning>
</metadata>
```
  
Les jars suivants n'ont a priori pas à etre mis à jour (principalement car
soit ils ne sont plus maintenus mais utiles, soit utilisables pour des
packages non maintenus comme WaarpAdministrator ou WaarpXmlEditor).

- `ftp4j` en version 1.7.2 absente des repository Maven
- `javasysmon` en version 0.3.6 absente des repository Maven
- `sun plugin` en version 1.6 absente des repository Maven
- `Xerces`, `XML-APIS` en version 2.5.0 absente des repository Maven
- `XMLEditor` en version 2.2 absente des repository Maven


## 2  Vérification

Il s’agit de s’assurer du bon fonctionnement de la version selon les tests
automatisés et de la qualité du code.

### 2.1  S’assurer de la bonne documentation

Dans le répertoire `doc/waarp-r66/source`, s’assurer a minima que les fichiers
`changes.rst` et `conf.py` sont à jour tant sur les versions que la
documentation.

### 2.2  Via Maven

#### 2.2.1  Mise à jour des versions dans le `pom.xml` parent dans `Waarp-All`

Dans la section `profiles`, 3 profils sont importants et doivent faire l'objet
d'une mise à jour pour une nouvelle version :

Le champ à modifier est `waarp.version` avec une valeur du type `x.y.z`.

##### 2.2.1.1  Profil `jre6`

Dans ce profil, la compilation cible JRE6 et les paquets seront nommés de
manière standard `NomPackage-version.jar`.



**IMPORTANT** : chaque commande Maven doit être accompagnée de l'option
spécifiant le profil : **`mvn -P jre6 ...`**. Une JDK8 est nécessaire.

##### 2.2.1.2  Profil `jre8`

Dans ce profil, la compilation cible JRE8 et les paquets seront nommés de
manière standard `NomPackage-version-jre8.jar`.

**IMPORTANT** : chaque commande Maven doit être accompagnée de l'option
spécifiant le profil : **`mvn -P jre8 ...`**. Une JDK8 est nécessaire.

##### 2.2.1.3  Profil `jre11`

Dans ce profil, la compilation cible JRE11 et les paquets seront nommés de
manière standard `NomPackage-version-jre11.jar`.

**IMPORTANT** : chaque commande Maven doit être accompagnée de l'option
spécifiant le profil : **`mvn -P jre11 ...`**. **Une JDK11 est nécessaire.**

#### 2.2.2 Validation Maven

A la racine du projet `Waarp-All` :

- Vérification des dépendances : ATTENTION, `Waarp-All` est prévu pour des
  dépendances pures Java 6, certains modules ne peuvent donc être upgradés.
  - Dépendances logicielles Java : l’inspection des résultats proposera des
    mises à jour définies dans le `pom.xml` parent (notamment la partie
    `properties`) (à prendre avec précaution)
    - `mvn -P jreX versions:display-dependency-updates`
  - Dépendances logicielles Maven
    - `mvn -P jreX versions:display-plugin-updates`
- Vérification du code
  - `mvn -P jreX clean install`
    - Ceci vérifiera l’ensemble des modules selon les tests Junits et les tests
      IT en mode « simplifiés » (raccourcis).
    - Si un module plante, à partir de votre IDE, vous pouvez relancer les
      tests concernés et corriger ou faire pour un module unique :
      `mvn -P jreX install -f nom_du_module` (où `nom_du_module` est par exemple
      `WaarpR66`). Il faudra ensuite relancer la commande complète de tests
      `mvn -P jreX clean install`
    - À noter que quelques tests peuvent parfois avoir une erreur sans être
      reproductible (en raison de tests dont les conditions d’exécution ne sont
      pas garantis à 100 % pour des raisons de concurrences, de charge de la
      machine…) :
      - Le test dans `WaarpCommon` concernant `FileMonitor`
      - Plus rarement dans `WaarpFtp-SimpleImpl` ou `WaarpFtpClient`
      - Une relance suffit à 99 % à obtenir un test stable (la plupart du
        temps, la raison est une machine trop chargée au moment des tests avec
        d’autres activités)

Ces tests produisent différents fichiers, dont ceux relatifs à SonarQube
`jacoco.xml`.

### 2.3  Autres tests

Il y a d’autres tests possibles qui nécessitent la mise en place d’un
environnement ad-hoc, basé sur Docker.

#### 2.3.1  ICAP

- Construction de l’image ICAP server :
  - `docker build -t cicap-avscan-natif .`
- Lancement de l’image ICAP server :
  - `docker run --rm -a STDERR cicap-avscan-natif`
- Récupération des paramètres de l’image ICAP server :
  - `docker inspect --format='{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $1`
- ou mieux
  - `docker inspect --format='{{range $p, $conf := .NetworkSettings.Ports}}`
    `{{$p}} -> {{(index $conf 0).HostPort}} {{end}}' $1`

- Puis lancement des tests avec Docker avec le client Waarp ICAP avec ces
  paramètres :
  - Depuis son IDE ou en ligne de commande :
    - `IcapTestClientReal`
    - NB : Le test est prévu pour s’exécuter sur l’adresse `172.17.0.2` par
      défaut de Docker, mais si ce n’est pas la bonne adresse, il faut la
      spécifier en premier et seul argument.
      - `IcapTestClientReal XXX.XXX.XXX.XXX`

#### 2.3.2  Autres tests

Selon les besoins (par exemple, tests de résistance, de stress, de
performances)…

Par exemple, pour lancer les tests IT de "longue durée", il faut exécuter la
commande suivante :

- `mvn -P jreX -Dit.test=ClassDuTest -DIT_LONG_TEST=true -f ModuleName failsafe:integration-test`
  où `ClassDuTest` est le nom de la classe contenant le test long et 
  `ModuleName` le nom du module la contenant (en général `WaarpR66`)
- ou `mvn -P jreX -DIT_LONG_TEST=true failsafe:integration-test` pour tous les vérifier

### 2.4  Étape SonarQube

Cette étape permet de générer l’analyse complète SonarQube qui sera intégrée
partiellement dans le Site Maven.

Prérequis : les tests complets (`mvn -P jre6 clean install`) doivent avoir été
préalablement exécutés avec succès. *(pas besoin de générer pour JRE8 ou JRE11)*

SonarQube doit être actif durant cette étape.

- `mvn -P jreX -pl '!WaarpR66Gui,!WaarpAdministrator,!WaarpXmlEditor' sonar:sonar`
- Il est possible et recommandé ensuite de constater les résultats sur
  l’interface Web de SonarQube.
- Si nécessaire, apportez encore des corrections pour des failles de sécurité
  ou mauvaises pratiques, le cas échéant en rejouant tous les tests depuis le
  début.

L'exclusion des 3 packages est en raison de l'absence de tests sur ces modules
graphiques.

SonarQube peut être arrêté une fois cette étape terminée.


## 3  Publication

Il s’agit ici de préparer les éléments nécessaires pour la publication JAR,
HTML et GITHUB.

### 3.1  Publication des JAR

Grâce à Artifactory (ou équivalent), qui doit être actif durant cette étape,
via Maven, il est possible de pré-publier les Jar dans un dépôt local maven :

- `mvn -P release,jreX clean deploy -DskipTests`

Si vous disposez de Java 11, vous pouvez donc publier la version :
- Java 11 avec `jre11`

Si vous disposez de Java 8, vous pouvez publier 2 versions :
- Java 6 avec `jre6`
- Java 8 avec `jre8`

Note pour Artifactory : l’export sera effectué sur le répertoire attaché au
host non Docker dans `/jfrog/artifactory/logs/maven2/`.

Il est possible de modifier l'adresse du service Maven de publication dans le
profile `release`.

Une fois publiés dans le dépôt Maven local, il faut suivre la procédure pour
recopier le résultat dans le dépôt GITHUB correspondant. Pour Artifactory :

- Une fois connecté comme administrateur
- Déclencher la réindexation du dépôt `libs-release-local` (menu système)
- Déclencher l’export du dépôt `libs-release-local` en ayant pris soin de
  cocher les cases « create .m2 compatible export » et « Exclude Metadata »
  en sélectionnant le répertoire de sortie `/opt/jfrog/logs/maven2` dans
  l’image Docker, qui sera situé in fine dans `/jfrog/logs/maven2`.
- Dans l’interface de consultation du dépôt `libs-release-local` d’Artifactory,
  cliquer sur le bouton de droite pour pouvoir accéder en mode Web natif à ce
  dépôt et enregistrer sous les deux fichiers placés sous le répertoire
  « .index » dans le répertoire correspondant sous 
  `/jfrog/logs/maven2/libs-release-local/.index`
- En ligne de commande, accordez les droits à tous les fichiers pour être copiés :
  - `sudo chmod -R a+rwX /jfrog/logs/maven2/libs-release-local/`
- Recopier ensuite le contenu dans le répertoire cible (projet `Waarp-Maven2`,
  répertoire `maven2`)
- Vous pouvez ensuite publier la mise à jour sous Github
  - `git add .`
  - `git commit -m « Nom de la release »`
  - `git push origin master`

Artifcatory ou équivalent peut être arrêté à partir d’ici.

NB : la première fois qu’Artifcatory ou équivalent est installé, il faut
installer tous les jars pré-existants dans le dépôt (procédure manuelle ou
automatisée si l’on peut), ceci afin de disposer d’un dépôt complet et
correspondant à l’existant.

### 3.2  HTML

Il s’agit ici de générer les pages automatisées Maven (Site Maven) du projet
`Waarp-All`.
Pour cela, il est conseillé de disposer de 2 clones de `Waarp-All`, l’un pour
s’occuper des sources java et constructions, l’autre pour ne gérer que la
branche `gh-pages`.

#### 3.2.1  Étape Site Maven

Cette étape permet de générer le Site Maven (dans `Waarp-All/target/staging`).

- `mvn -P jre6 site site:stage` *(pas besoin de générer pour JRE8 ou JRE11)*

Recopier ensuite le contenu de ce site dans le clone `Waarp-All` pour la
branche `gh-pages` prévu à cet effet et enfin publier :

- `git add .`
- `git commit -m « Nom de la release »`
- `git push origin gh-pages`

#### 3.2.2  Étape Site HTML global

Dans cette étape, il s’agit de mettre à jour les fichiers Web du site global
`Waarp` sous Github (tant que maintenu) :

- Pour cela, il faut disposer d’un clone du projet `Waarp` sur la branche
  `gh-pages`
- Mettre à jour les fichiers suivants (et plus si nécessaire) :
  `index.html` (numéro de version), `Downloads.html` (à la dernière version),
  `Roadmap.html` et `ReleaseHistory.html` (pour les données de versions)
- Puis de mettre à jour le site
  - `git add .`
  - `git commit -m « Nom de la release »`
  - `git push origin gh-pages`

### 3.3  GITHUB

Il s’agit ici de publier la release sous Github :

- Préparer une note de version (en langue Anglaise)
- Aller sur le site (la version étant à jour sur la branche maître
  correspondante)
- Créer une nouvelle release sous Github (tags → Releases → Draft a new release)
- Associer au moins les 2 jars complets pour `WaarpGatewayFtp` et `WaarpR66` à
  la release (trouvés respectivement dans 
  `Waarp-All/WaarpGatewayFtp/target/WaarpGatewayFtp-X.Y.Z-jar-with-dependencies.jar`
  et `Waarp-All/WaarpR66/target/WaarpR66-X.Y.Z-jar-with-dependencies.jar`)
  - Le cas échéant, y ajouter les versions JRE8 et JRE11.
- Publier


D’autres étapes sont nécessaires, comme la publication sur le site
officiel de la société Waarp.
