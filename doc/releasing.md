# Note pour une nouvelle release Waarp-All

## 1  Pré-requis

Les outils suivants sont nécessaires. Des variantes peuvent être utilisées :

- Java 1.8 (les tests requièrent java 1.8 même si les modules sont compilés en
  Java 1.6)
- Maven 3.6.3
- Docker (19 par exemple)
- Artifactory (ou équivalent) pour pré-publier les jar (via Docker sur le PC :
  https://www.jfrog.com/confluence/display/JFROG/Installing+Artifactory )
- SonarQube (via Docker par exemple :
  https://docs.sonarqube.org/latest/setup/get-started-2-minutes/ )
- De préférence un IDE moderne comme IntelliJ (en version communautaire) :
  un modèle de configuration est disponible.
- Un Host Linux (Debian, Ubuntu ou Redhat ou assimilé) avec au moins 2 cœurs et
  au moins 8 Go de mémoire disponibles ainsi que 20 Go de disques (pour les
  sources et compilation) et 10 Go sur le répertoire `/tmp`


## 2  Vérification

Il s’agit de s’assurer du bon fonctionnement de la version selon les tests
automatisés et de la qualité du code.

### 2.1  S’assurer de la bonne documentation

Dans le répertoire `doc/waarp-r66/source`, s’assurer a minima que les fichiers
`changes.rst` et `conf.py` sont à jour.

### 2.2  Via Maven

A la racine du projet `Waarp-All` :

- Vérification des dépendances : ATTENTION, `Waarp-All` est prévu pour des
  dépendances pures Java 6, certains modules ne peuvent donc être upgradés.
  - Dépendances logicielles Java : l’inspection des résultats proposera des
    mises à jour définies dans le `pom.xml` parent (notamment la partie
    `properties`) (à prendre avec précaution)
    - `mvn versions:display-dependency-updates`
  - Dépendances logicielles Maven
    - `mvn versions:display-plugin-updates`
- Vérification du code
  - `mvn clean install`
    - Ceci vérifiera l’ensemble des modules selon les tests Junits et les tests
      IT en mode « simplifiés » (raccourcis).
    - Si un module plante, à partir de votre IDE, vous pouvez relancer les
      tests concernés et corriger ou faire pour un module unique :
      `mvn install -f nom_du_module` (où `nom_du_module` est par exemple
      `WaarpR66`). Il faudra ensuite relancer la commande complète de tests
      `mvn clean install`
    - A noter que quelques tests peuvent parfois avoir une erreur sans être
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

- `mvn -Dit.test=ClassDuTest -DIT_LONG_TEST=true -f ModuleName failsafe:integration-test`
  où `ClassDuTest` est le nom de la classe contenant le test long et 
  `ModuleName` le nom du module la contenant (en général `WaarpR66`)
- ou `mvn -DIT_LONG_TEST=true failsafe:integration-test` pour tous les vérifier

### 2.4  Étape SonarQube

Cette étape permet de générer l’analyse complète SonarQube qui sera intégrée
partiellement dans le Site Maven.

Prérequis : les tests complets (`mvn clean install`) doivent avoir été
préalablement exécutés avec succès.
SonarQube doit être actif durant cette étape.

- `mvn -pl '!WaarpR66Gui,!WaarpAdministrator,!WaarpXmlEditor' sonar:sonar`
- Il est possible et recommandé ensuite de constater les résultats sur
  l’interface Web de SonarQube.
- Si nécessaire, apportez encore des corrections pour des failles de sécurité
  ou mauvaises pratiques, le cas échéant en rejouant tous les tests depuis le
  début.

SonarQube peut être arrêté une fois cette étape terminée.


## 3  Publication

Il s’agit ici de préparer les éléments nécessaires pour la publication JAR,
HTML et GITHUB.

### 3.1  Publication des JAR

Grâce à Artifactory (ou équivalent), qui doit être actif durant cette étape,
via Maven, il est possible de pré-publier les Jar dans un dépôt local maven :

- `mvn -P release deploy`

Note pour Artifactory : l’export sera effectué sur le répertoire attaché au
host non Docker dans `/jfrog/artifactory/logs/maven2/`

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

- `mvn site site:stage`

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
- Publier


D’autres étapes sont nécessaires, comme par exemple la publication sur le site
officiel de la société Waarp.
