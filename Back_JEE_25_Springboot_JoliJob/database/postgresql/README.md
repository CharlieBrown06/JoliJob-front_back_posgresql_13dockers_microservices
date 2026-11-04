# PostgreSQL local et DBeaver

La configuration locale utilise **`localhost:5432`, base `jolijob_db`, login `admin`**.
Le mot de passe est fourni interactivement, jamais stocke dans le depot.
Le role applicatif nouvellement cree n'est pas superutilisateur et ne peut
creer ni bases ni roles. Cette configuration est reservee au developpement local.

| Schema | Service | Tables |
| --- | --- | --- |
| `profiles` | `profile-service` | `candidates`, `companies` |
| `joboffers` | `job-offer-service` | `job_offers`, `job_offer_outbox` |
| `applications` | `application-service` | `applications`, `application_outbox` |
| `keycloak` | Keycloak | Gerees automatiquement par Keycloak |

## 1. Creer le role et la base

Depuis la racine du depot, sur Linux avec une installation PostgreSQL standard :

```sh
sudo -u postgres psql -X -d postgres -f - < database/postgresql/00-bootstrap.psql
```

Le script demande deux fois le mot de passe du role `admin` : saisir la valeur
locale choisie. Ce script utilise des commandes propres a **psql**, pas a DBeaver,
et ne doit pas etre execute dans une transaction. L'executer depuis un terminal
interactif pour la saisie du mot de passe. La redirection permet au shell courant
de lire le fichier meme si le compte systeme `postgres` ne peut pas acceder au
repertoire personnel. Avec une autre installation,
se connecter a la base `postgres` avec un compte administrateur autorise a creer
des roles et des bases, puis executer le meme fichier avec `psql -X -f`.

La base et le role ne sont crees que s'ils n'existent pas. Une nouvelle execution
**change le mot de passe d'un role `admin` existant**, mais ne change pas ses
privileges ni le proprietaire d'une base existante. Verifier ces objets avant de
relancer le script sur une installation partagee.

## 2. Creer les schemas et les tables

```sh
psql -X -h localhost -p 5432 -U admin -W -d jolijob_db \
  -v ON_ERROR_STOP=1 -f database/postgresql/01-schema.sql
```

Ou, dans **DBeaver**, creer une connexion PostgreSQL avec :

| Parametre | Valeur |
| --- | --- |
| Hote | `localhost` |
| Port | `5432` |
| Base de donnees | `jolijob_db` |
| Utilisateur | `admin` |
| Mot de passe | Celui saisi a l'etape 1 |

Tester la connexion, ouvrir un editeur SQL sur cette connexion et executer
**tout le script** `01-schema.sql`. Rafraichir ensuite le navigateur de bases
et afficher les schemas `profiles`, `joboffers`, `applications` et `keycloak`.
Le schema `public` n'est pas utilise par les services dans ce mode.

Le script correspond aux six entites JPA actuelles : UUID, enums stockes en
chaines, dates, montants, contraintes d'unicite et index des outbox inclus.
Les UUID metier sont fournis par les applications ; les compteurs d'outbox
sont generes par PostgreSQL. Il ne contient aucune donnee de demonstration.
Il peut etre rejoue sans supprimer les tables ou donnees, mais **ce n'est pas
un outil de migration** : `IF NOT EXISTS` ne corrige pas une table existante
incompatible. Sauvegarder et migrer explicitement toute ancienne structure.

## 3. Connecter les services Spring Boot

Dans chaque terminal de lancement, depuis la racine, avec Bash :

```sh
source database/postgresql/local-env.sh
./mvnw -f microservices/profile-service/pom.xml spring-boot:run
```

Pour les deux autres services, utiliser respectivement
`microservices/job-offer-service/pom.xml` et
`microservices/application-service/pom.xml` apres avoir source le meme fichier.
JDK 25 est necessaire. Le fichier demande le mot de passe sans l'afficher,
exporte les parametres de connexion et active le profil Spring **`local`**.
Il ne demarre aucun service et remplace les variables de connexion du terminal.
Une fois les processus arretes, `unset DB_PASSWORD` retire le secret du shell.

Chaque `application-local.yaml` cible son schema via `currentSchema`, y compris
pour les requetes SQL natives des relais outbox. Hibernate utilise
`ddl-auto: validate` : le script doit etre execute avant le demarrage ;
une structure incompatible provoque une erreur au lieu d'etre modifiee.
Sans le profil `local`, les configurations precedentes restent inchangees.
Kafka, Elasticsearch, SMTP et Keycloak restent des dependances distinctes ;
creer cette base ne suffit pas a demarrer toute la plateforme.

## 4. Keycloak

Le schema `keycloak` est cree vide : **ne pas creer manuellement ses tables**.
Keycloak les cree et les migre lors de son initialisation. Apres avoir source
le fichier d'environnement, configurer dans le meme terminal :

```sh
export KC_DB=postgres
export KC_DB_URL='jdbc:postgresql://localhost:5432/jolijob_db'
export KC_DB_SCHEMA=keycloak
export KC_DB_USERNAME="$DB_USERNAME"
export KC_DB_PASSWORD="$DB_PASSWORD"
```

Puis suivre [les instructions d'import et de lancement Keycloak](../../identity/keycloak/README.md)
en conservant cette URL et ce schema pour **l'import et le demarrage**.
Le compte administrateur de Keycloak est distinct du role PostgreSQL `admin`.
Ce parcours concerne une nouvelle installation ; il ne deplace pas les donnees
d'une ancienne base Keycloak.

## Relations et jointures

```text
profiles.companies.id  1 ---- N  joboffers.job_offers.company_id
profiles.candidates.id 1 ---- N  applications.applications.candidate_id
joboffers.job_offers.id 1 --- N  applications.applications.job_offer_id
profiles.companies.id  1 ---- N  applications.applications.company_id
```

Ces liens sont **logiques**, sans cles etrangeres interschemas : les services
verifient leurs references par HTTP et conservent leurs propres donnees.
Les identifiants `user_id` des profils correspondent au `sub` Keycloak ;
aucune jointure directe avec les tables internes de Keycloak n'est requise.
Les evenements d'outbox sont conserves meme apres suppression de leur agregat.

Executer `02-joins.sql` dans DBeaver pour consulter les candidatures avec leurs
candidats, offres et entreprises, compter les candidatures par offre et suivre
les evenements en attente. Les `LEFT JOIN` preservent les donnees historiques
si une offre ou un profil a ete supprime. Les jointures interschemas sont pour
l'exploration locale, pas pour remplacer les API interservices. DBeaver ne
deduira pas automatiquement un diagramme de cles etrangeres pour ces liens.

Le role unique `admin` peut acceder a tous les schemas : cette separation est
logique, pas une isolation des permissions. En production, utiliser des roles
distincts avec privileges minimaux et un gestionnaire de secrets.

Pour changer le mot de passe, utiliser `\password admin` dans `psql`, puis
mettre a jour la connexion DBeaver et ressaisir le nouveau mot de passe dans
les terminaux de lancement (et les variables Keycloak).

## Tests CRUD PostgreSQL

Depuis la racine du depot :

```sh
bash database/postgresql/tests/run-crud.sh
```

Le lanceur demande le mot de passe PostgreSQL et utilise par defaut
`localhost:5432`, `jolijob_db`, `admin`. Les variables `DB_HOST`, `DB_PORT`,
`DB_NAME` et `DB_USERNAME` permettent de changer la connexion. Il fonctionne
aussi depuis un autre repertoire avec son chemin absolu :

```sh
bash /home/demians/IdeaProjects/jolijob/database/postgresql/tests/run-crud.sh
```

Dans DBeaver, ouvrir `tests/crud.sql` sur une connexion `jolijob_db` dediee,
en mode auto-commit et sans transaction ni modifications en attente, puis
**executer le script entier**, pas seulement la requete sous le curseur.
Consulter les resultats `test_name / result` et le total `tests_passed`.
Une verification incorrecte declenche une erreur explicite `FAIL`.
Le lanceur terminal s'arrete sur la premiere erreur avec un code de sortie
non nul. En cas d'erreur dans DBeaver, executer `ROLLBACK;` avant de reutiliser
la connexion.

Les 41 verifications couvrent CREATE, READ, UPDATE et DELETE sur les six tables,
les jointures interschemas, les montants decimaux, dates et textes longs,
les contraintes obligatoires, les enums, l'unicite des profils, des candidatures
et des evenements, le rollback SQL metier/outbox et la conservation des donnees
historiques apres suppression de references distantes.

Les donnees sont fictives, identifiees par des UUID aleatoires ; toutes les
modifications ciblent ces identifiants. Le script ne cree ni ne remplace les
tables applicatives et ne touche pas aux tables internes de Keycloak.
Il annule sa transaction a la fin, y compris les fonctions et resultats
temporaires. Les sequences PostgreSQL ne sont pas transactionnelles :
les compteurs des outbox peuvent avancer meme apres rollback, sans modifier
les lignes existantes. Utiliser uniquement une base locale ou de test.

**Portee : tests de la base reelle, pas tests HTTP des microservices.**
Ils ne prouvent pas le mapping Hibernate, les permissions JWT, les regles
metier, la publication Kafka ou l'envoi d'e-mails. Les suppressions SQL sont
verifiees meme lorsqu'un service expose plutot une desactivation d'offre ou
un retrait de candidature. Keycloak gere son propre schema ; les services
de recherche, d'indexation, de notification et la gateway n'ont pas de tables
PostgreSQL metier a tester ici.
