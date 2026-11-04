# JoliJob — services et architecture

L'application métier est découpée en applications Spring Boot autonomes sous `microservices/`. Chaque service possède son modèle, son API et son accès aux données ; les services ne partagent pas d'entités Java.

```text
Client ──OIDC/PKCE──> Keycloak :8081 (realm jolijob)
Client ──Bearer──> API Gateway :8080
             ├── profile-service :8083 ───> base profiles
             ├── job-offer-service :8084 ─> base joboffers
             ├── application-service :8085 > base applications
             └── search-service :8086 ────> Elasticsearch

application-service ──HTTP──> profiles, job offers
job-offer-service ─────HTTP──> profiles
job-offer-service ──outbox──> Kafka job-offer.events──> job-offer-indexer-service ──> Elasticsearch
application-service ──outbox──> Kafka application.events──> notification-service ──> SMTP
```

Les services métier suivent la direction hexagonale `adapter entrant -> port entrant -> cas d'usage -> port sortant <- adaptateur sortant`. Les couches `domain` et `application` ne dépendent pas de Spring ni de JPA ; les entités JPA, contrôleurs REST, clients HTTP, producteurs/consommateurs Kafka et adaptateurs Elasticsearch/SMTP sont à l'extérieur du cœur. La gateway transmet le jeton Bearer ; Keycloak émet les JWT, validés indépendamment par chaque service protégé.

Les bases sont indépendantes logiquement (`profiles`, `joboffers`, `applications`, plus `keycloak`) et peuvent utiliser le même serveur PostgreSQL local avec des bases distinctes. Chaque service a son propre POM Maven. Les contrats d'événements sont des messages JSON locaux à chaque consommateur ; ils ne partagent pas de classes Java. Les offres et candidatures utilisent un outbox transactionnel avec livraison au moins une fois.

## Lancer et tester sans conteneurs

Alternative locale : le [guide PostgreSQL et DBeaver](../database/postgresql/README.md)
configure la base unique `jolijob_db` avec un schema par service. Activer le
profil Spring `local` pour ce mode ; les bases distinctes restent le mode par defaut.

Prérequis : JDK 25, Maven 3.9+, PostgreSQL, Keycloak, Kafka, Elasticsearch et un SMTP de développement. Créer les bases indiquées ci-dessus. Chaque processus métier lit `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`; les URL des dépendances externes et l'émetteur JWT sont également configurables dans les `application.yaml` propres aux services. La configuration Keycloak se trouve dans [`../identity/keycloak/`](../identity/keycloak/README.md).

Depuis la racine, construire et tester les sept applications en une seule commande Maven :

```sh
./mvnw verify
```

Les services restent indépendants et peuvent aussi être testés individuellement avec `./mvnw -f microservices/<service>/pom.xml test`, puis lancés avec `./mvnw -f microservices/<service>/pom.xml spring-boot:run`. La gateway cible par défaut les ports locaux ci-dessus. Pour OIDC dans un navigateur, utiliser Keycloak directement sur `http://localhost:8081/realms/jolijob`. Les anciens endpoints d'inscription/connexion maison sont retirés ; les clients doivent adopter Authorization Code + PKCE.

La conteneurisation et l'orchestration des services sont laissées pour une étape ultérieure. L'ancien code source monolithique reste dans `src/` comme référence de migration, mais il ne fait plus partie du build Maven racine : seul le reactor des services sous `microservices/` est construit.

## Organisation des tests

Les tests sont dans `src/test/java` de chaque service et utilisent JUnit 5, AssertJ, Mockito et les outils de test Spring. Ils sont tous exécutés par Maven Surefire pendant la phase `test`, y compris les classes `*IntegrationTest` : aucun profil ni lancement Docker n'est nécessaire. Le JDK 25 suffit ; les dépendances Maven doivent être disponibles ou téléchargeables.

Les tests unitaires instancient les cas d'usage sans contexte Spring et remplacent les ports sortants par des doublures. Les tests d'intégration assemblent plusieurs couches réelles : API, sécurité, cas d'usage, persistance ou consommateurs Kafka.

| Service | Principales frontières couvertes |
|---|---|
| `profile-service` | Cas d'usage candidat/entreprise, droits REST, JWT et persistance JPA sur H2 |
| `job-offer-service` | Gestion des offres, client profils, JPA, JWT et atomicité/ordre/reprise de l'outbox |
| `application-service` | Règles de candidature, droits, clients HTTP et transaction candidature/outbox |
| `search-service` | Paramètres REST, cas d'usage, construction des filtres et client Elasticsearch HTTP |
| `job-offer-indexer-service` | Traitement des événements et intégration du consommateur Kafka |
| `notification-service` | Traitement des événements, contenu des e-mails et intégration du consommateur Kafka |
| `api-gateway` | Routage HTTP réel, propagation des requêtes/erreurs et absence de routes internes/auth maison |

Exemples de commandes depuis la racine :

```sh
# Tous les tests des sept services
./mvnw test

# Un seul service
./mvnw -f microservices/application-service/pom.xml test

# Un test unitaire et un test d'integration de la recherche
./mvnw -f microservices/search-service/pom.xml \
  -Dtest=SearchServiceTest,SearchHttpIntegrationTest test
```

Les rapports sont produits dans `microservices/<service>/target/surefire-reports/`.

Les limites des doublures sont importantes : H2 ne remplace pas une validation sur PostgreSQL, les serveurs HTTP locaux simulent les services distants/Elasticsearch, et les sorties Elasticsearch/SMTP des consommateurs sont remplacées par des mocks. Les tests Kafka embarqués exercent un vrai broker local au processus de test, pas un cluster de production. Les tests de JWT utilisent des clés de test et/ou une authentification simulée selon le scénario ; ils ne prouvent pas l'import du realm ni le parcours navigateur sur un Keycloak réel. Les essais de bout en bout avec toutes les infrastructures déployées restent distincts.
