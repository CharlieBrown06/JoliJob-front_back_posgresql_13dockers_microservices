# JoliJob — conteneurs Docker

Ce dépôt conserve la **version pédagogique microservices** de JoliJob :
frontend Angular, sept services Spring Boot et infrastructure Docker Compose.
La version monolithe sera maintenue séparément ; elle n'est pas le mode de
démarrage de ce dépôt.

## Microservices de l'application

JoliJob comprend **sept services Spring Boot indépendants**, situés dans
`Back_JEE_25_Springboot_JoliJob/microservices/` :

| Service | Rôle |
| --- | --- |
| `api-gateway` | Point d'entrée du backend : dirige les requêtes vers les API métier et transmet les jetons d'authentification. |
| `profile-service` | Gestion des profils des candidats et des entreprises. |
| `job-offer-service` | Publication et gestion des offres d'emploi. |
| `application-service` | Dépôt, suivi et retrait des candidatures. |
| `search-service` | Recherche d'offres à partir de l'index Elasticsearch. |
| `job-offer-indexer-service` | Écoute les événements d'offres sur Kafka et met à jour l'index Elasticsearch. |
| `notification-service` | Envoie les e-mails de notification à partir des événements de candidature. |

**Keycloak** est un service externe supplémentaire qui gère les comptes,
l'authentification et les rôles.

Le frontend Angular, PostgreSQL, Kafka, Elasticsearch et Mailpit complètent
la pile, mais ne sont pas des microservices métier développés pour JoliJob.
Avec Keycloak et les sept services Spring Boot, la configuration Compose
comprend **13 conteneurs**.

## Démarrage et configuration Docker

Docker est nécessaire, ainsi que Docker Compose v2. Depuis la racine du dépôt,
préparer les identifiants réservés au développement local puis construire et
lancer l'application :

```sh
cp .env.example .env
# Modifier les mots de passe locaux dans .env avant le premier démarrage.
docker compose up --build
```

Sous Linux, Elasticsearch peut également nécessiter `vm.max_map_count` réglé à
`262144` sur l'hôte. Le compte courant doit avoir accès au démon Docker.

Chaque service dispose de limites CPU et mémoire dans `compose.yaml`, appliquées
par Docker Compose sans nécessiter Swarm. Les sept microservices Spring Boot
sont limités chacun à 1 CPU et 512 Mio de RAM ; Elasticsearch à 1 CPU et
1536 Mio ; Keycloak à 1 CPU et 1024 Mio ; PostgreSQL et Kafka chacun à
0,5 CPU et 512 Mio ; le frontend et Mailpit chacun à 0,25 CPU et 128 Mio.
Le plafond cumulé est de 10,5 CPU et 7424 Mio de RAM (environ 7,3 Gio) :
ce sont des maxima, pas des ressources réservées. Le swap est désactivé pour
ces conteneurs afin de limiter la pression sur l'hôte ; un dépassement mémoire
peut entraîner l'arrêt du processus par Docker (OOM). Ces limites sont prévues
pour le développement local et peuvent nécessiter un ajustement sous charge.

Les limites des services ne s'appliquent pas à la construction des images.
Pour éviter les builds simultanés des microservices sur une machine limitée,
utiliser `docker compose --parallel 1 build`, puis `docker compose up -d --no-build`.

Le premier build génère les images des sept microservices Spring Boot et du
frontend Angular. Ouvrir ensuite `http://localhost:4200`; Keycloak est
accessible à `http://localhost:8081`, la gateway à `http://localhost:8080` et
Mailpit à `http://localhost:8025`. Le realm `jolijob` est importé au premier
démarrage. L'administration Keycloak utilise les identifiants `KC_BOOTSTRAP_*`
de `.env`.

Arrêter les conteneurs avec `Ctrl+C`, ou en arrière-plan avec
`docker compose down`. Les données PostgreSQL et Elasticsearch sont
conservées dans des volumes Docker ; les données Kafka de développement ne le
sont pas. Pour effacer les volumes persistants intentionnellement :
`docker compose down --volumes` (cette commande supprime les données locales).

Cette configuration est destinée au développement local : Keycloak utilise
HTTP, Elasticsearch n'a pas d'authentification et les mots de passe d'exemple
ne doivent jamais être réutilisés en production. Les services n'exposent que
leurs ports de développement sur l'interface locale.
