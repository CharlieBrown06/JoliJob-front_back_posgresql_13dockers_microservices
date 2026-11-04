# JoliJob

JoliJob est une plateforme d'emploi composée de **sept applications Spring Boot indépendantes et de Keycloak** pour l'identité. Le build Maven à la racine ne lance plus l'ancien monolithe : il agrège les services dans `microservices/`.

## Architecture

```text
Client ──OIDC Authorization Code + PKCE──> Keycloak (:8081, realm jolijob)
Client ──Bearer access token──> API Gateway (:8080)
             ├── Profiles (:8083) ──> PostgreSQL `profiles`
             ├── Job Offers (:8084) ─> PostgreSQL `joboffers`
             ├── Applications (:8085) > PostgreSQL `applications`
             └── Search (:8086) ─────> Elasticsearch

Applications ──HTTP──> Profiles, Job Offers
Job Offers ────HTTP──> Profiles
Job Offers ──outbox PostgreSQL──> Kafka `job-offer.events`──> Indexer ──> Elasticsearch
Applications ──outbox PostgreSQL──> Kafka `application.events`──> Notifications ──> SMTP
```

| Service | Responsabilité |
| --- | --- |
| Keycloak | Comptes, authentification OIDC, rôles et clés de signature persistantes |
| `profile-service` | Profils candidat et entreprise, plus quelques lectures internes |
| `job-offer-service` | Publication, gestion et propriété des offres |
| `application-service` | Dépôt, suivi et retrait des candidatures |
| `search-service` | API de recherche et lecture de l'index Elasticsearch |
| `job-offer-indexer-service` | Projection asynchrone des événements d'offre dans Elasticsearch |
| `notification-service` | Notifications e-mail déclenchées par les événements de candidature |
| `api-gateway` | Routage REST vers les API métier ; transmission des jetons Bearer |

## Structure hexagonale

Les services métier organisent leur code en quatre zones :

```text
adapter entrant → port entrant → cas d'usage/domaine → port sortant ← adaptateur sortant
```

Les contrôleurs REST, les consommateurs Kafka et la gateway sont des adaptateurs entrants. Les ports entrants exposent les cas d'usage. Le domaine et les services applicatifs ne dépendent ni de Spring ni de JPA. Les ports sortants décrivent les besoins du cœur ; les adaptateurs JPA, HTTP, Kafka, Elasticsearch et SMTP les réalisent.

Chaque service possède son POM, son modèle persistant et son contrat d'événement local. Les échanges interservices utilisent HTTP ou des messages JSON, pas des entités Java partagées. Par défaut, les données sont réparties dans les bases distinctes `profiles`, `joboffers`, `applications` et `keycloak`. Le mode local décrit ci-dessous utilise plutôt une seule base `jolijob_db`, avec un schéma dédié par service.

## PostgreSQL local et DBeaver

| Configuration | Base(s) PostgreSQL | Activation |
| --- | --- | --- |
| Par défaut | `profiles`, `joboffers`, `applications` ; `keycloak` pour l'identité | Sans profil Spring `local` |
| Développement local | `jolijob_db`, schémas `profiles`, `joboffers`, `applications`, `keycloak` | Profil Spring `local` pour les trois services métier ; configuration Keycloak séparée |

Dans le mode local, la connexion cible `localhost:5432` avec le rôle `admin`.
Les schémas isolent logiquement les données, mais le rôle commun peut accéder à
tous les schémas : ce n'est pas une isolation des permissions adaptée à la production.
Les références entre services restent logiques, sans clés étrangères interschémas.
Le schéma `public` n'est pas utilisé par cette configuration.

Le [guide PostgreSQL et DBeaver](database/postgresql/README.md) détaille :

- la création du rôle et de la base avec `database/postgresql/00-bootstrap.psql` ;
- la création des six tables et de leurs index avec `database/postgresql/01-schema.sql` ;
- les jointures de consultation avec `database/postgresql/02-joins.sql` ;
- la connexion des services et l'initialisation de Keycloak dans son schéma dédié.

Les tables internes de Keycloak sont créées par Keycloak lui-même, pas par le script métier.
Cette configuration ne migre pas automatiquement les données d'anciennes bases.

### Lancement local dans IntelliJ IDEA

Dans **Run → Edit Configurations…**, définir les variables d'environnement de
chaque configuration `profile-service`, `job-offer-service` et `application-service` :

```text
SPRING_PROFILES_ACTIVE=local
DB_HOST=localhost
DB_PORT=5432
DB_NAME=jolijob_db
DB_USERNAME=admin
DB_PASSWORD=<mot-de-passe-local>
```

Remplacer le marqueur de mot de passe dans IntelliJ uniquement. Ne pas activer
**Store as project file / Share through VCS** pour ces configurations contenant
des secrets, et ne jamais versionner le mot de passe dans les YAML ou les scripts.
Un fichier `.env` n'est pas chargé automatiquement par Spring Boot.

Les fichiers `application-local.yaml` ciblent chacun leur schéma via
`currentSchema` et utilisent `ddl-auto: validate` : les tables doivent exister
avant le démarrage, et Hibernate refuse une structure incompatible plutôt que
de la modifier. Le profil Spring ne configure pas Keycloak ; suivre le guide dédié.

## Identité et clés JWT

Le service Identity maison et sa génération de clés en mémoire ont été retirés des microservices. La configuration native de Keycloak et les instructions de démarrage, de stockage des secrets et de rotation des clés se trouvent dans [`identity/keycloak/`](identity/keycloak/README.md). Keycloak n'est pas installé ou démarré automatiquement par Maven.

Le navigateur utilise le client public `jolijob-web` avec **Authorization Code + PKCE S256**. Les anciens endpoints `/api/v1/auth/login` et `/api/v1/auth/register/*` ne sont plus routés. La connexion passe par les pages Keycloak ; l'inscription libre est désactivée et les comptes sont provisionnés sous le contrôle d'un administrateur. Le flux mot de passe direct et le flux implicite sont désactivés.

Les services protégés vérifient la signature RS256 via JWKS, l'émetteur du realm, l'expiration et l'audience `jolijob-api`. L'identifiant utilisateur est désormais le claim standard `sub` ; les rôles viennent de `realm_access.roles`, et l'adresse e-mail du claim `email`. Les anciens comptes ne sont pas automatiquement migrés : les anciens identifiants de profils doivent être réconciliés avec les nouveaux `sub` avant de réutiliser des données existantes.

Les microservices ne reçoivent aucune clé privée de signature. La protection de la base Keycloak, les sauvegardes chiffrées, TLS et la restriction de l'administration restent indispensables ; un import de realm ne configure pas à lui seul un coffre-fort ou un HSM.

## Outbox transactionnel

Dans les services offres et candidatures, une transaction locale enregistre **le changement métier et l'événement JSON dans la même base**. Le relais lit les événements en attente, attend l'accusé de réception Kafka, puis marque l'événement publié. Un échec conserve l'événement pour une nouvelle tentative ; un événement défaillant bloque les suivants du même agrégat pour préserver l'ordre.

La garantie est **au moins une fois**, pas exactement une fois : un arrêt après l'accusé Kafka mais avant le commit SQL peut produire un doublon avec le même `eventId`. La projection Elasticsearch utilise l'identifiant de l'offre ; les e-mails SMTP peuvent être envoyés plusieurs fois dans ce scénario. Une garantie d'envoi unique nécessite un fournisseur acceptant une clé d'idempotence ou une stratégie de déduplication adaptée ; SMTP seul ne permet pas une transaction atomique avec Kafka/PostgreSQL.

## Construire et tester

### Tests Java

Les tests Java automatisés nécessitent le JDK 25, mais pas de services externes démarrés ni de Docker. Leur organisation, leurs doublures et leurs limites sont détaillées dans la [documentation des tests](microservices/README.md#organisation-des-tests).

Pour construire les sept services et exécuter leurs tests :

```sh
./mvnw verify
```

### Tests CRUD sur PostgreSQL réel

Après initialisation de `jolijob_db`, lancer depuis la racine du projet :

```sh
bash database/postgresql/tests/run-crud.sh
```

Le lanceur demande le mot de passe PostgreSQL. Les **41 vérifications** couvrent
CREATE, READ, UPDATE et DELETE sur les six tables, les jointures, les contraintes,
les données historiques et le rollback SQL métier/outbox. Les résultats
affichent `PASS` ; une anomalie provoque une erreur et un code de sortie non nul.
Le script SQL peut aussi être exécuté entièrement dans DBeaver, selon
les [instructions des tests CRUD](database/postgresql/README.md#tests-crud-postgresql).

Les données de test sont annulées par `ROLLBACK` ; les séquences des outbox
peuvent néanmoins avancer. Utiliser uniquement une base locale ou de test.
Ces tests nécessitent PostgreSQL et `psql` (ou DBeaver), pas Java ni les services
démarrés. Ils vérifient la base SQL, **pas** le mapping Hibernate, les API REST,
les autorisations JWT, Kafka ou les e-mails.

### Démarrage des services

Pour exécuter les applications avec leurs infrastructures réelles, prévoir une
distribution Keycloak compatible avec son propre JDK, PostgreSQL dans l'un des
deux modes décrits ci-dessus, Kafka, Elasticsearch et un serveur SMTP de
développement. Les paramètres sont configurables avec les variables définies
dans les `application.yaml` de chaque service. Pour `jolijob_db`, activer le profil
`local` et fournir les variables de connexion avant le lancement.

Pour démarrer une application indépendamment, par exemple :

```sh
./mvnw -f microservices/profile-service/pom.xml spring-boot:run
./mvnw -f microservices/job-offer-service/pom.xml spring-boot:run
./mvnw -f microservices/application-service/pom.xml spring-boot:run
./mvnw -f microservices/search-service/pom.xml spring-boot:run
./mvnw -f microservices/job-offer-indexer-service/pom.xml spring-boot:run
./mvnw -f microservices/notification-service/pom.xml spring-boot:run
./mvnw -f microservices/api-gateway/pom.xml spring-boot:run
```

La gateway est accessible sur `http://localhost:8080`; l'émetteur local Keycloak est `http://localhost:8081/realms/jolijob`. Le navigateur contacte Keycloak directement ; la gateway ne relaie pas les endpoints OAuth. En production, utiliser un émetteur HTTPS canonique et les mêmes paramètres d'émetteur/audience dans tous les services.

## Limites actuelles

L'orchestration Docker de développement est disponible dans le
[`compose.yaml` à la racine](../compose.yaml). Elle démarre les sept services,
le frontend et leurs dépendances locales. Cela ne remplace pas une configuration
de production. Les 41 vérifications SQL ont été exécutées avec succès sur
PostgreSQL 18, y compris sur la base locale `jolijob_db`. Cela ne valide pas les parcours de bout en bout :
le démarrage des microservices avec Hibernate sur PostgreSQL réel et les
intégrations avec Keycloak, Kafka, Elasticsearch et SMTP restent à vérifier
ensemble dans l'environnement de développement. Les lignes outbox publiées
doivent faire l'objet d'une politique de rétention et les événements en erreur
d'une surveillance opérationnelle.

Les anciennes sources du monolithe restent dans `src/` à titre de référence ; elles ne sont plus incluses dans le build Maven racine.
