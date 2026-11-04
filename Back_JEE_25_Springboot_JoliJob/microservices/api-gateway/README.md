# API gateway

Standalone Spring Boot 4.0.6 / Java 25 infrastructure service using Spring Cloud Gateway. Routing is configured in `src/main/resources/application.yaml`; no business domain or database belongs here.

| Incoming path | Default backend | Environment override |
| --- | --- | --- |
| `/api/v1/candidates/**`, `/api/v1/companies/**` | `http://localhost:8083` | `PROFILE_SERVICE_URL` |
| `/api/v1/job-offers/**` | `http://localhost:8084` | `JOB_OFFER_SERVICE_URL` |
| `/api/v1/applications/**` | `http://localhost:8085` | `APPLICATION_SERVICE_URL` |
| `/api/v1/search/**` | `http://localhost:8086` | `SEARCH_SERVICE_URL` |

Paths, query strings, request methods/bodies and bearer authorization headers are preserved. Backend errors pass through without write retries or success-shaped fallbacks. Connections time out after 5 seconds, responses after 30 seconds. Unknown paths, including `/internal/**`, return 404.

## Keycloak authentication

The homemade Identity service has been retired. `/api/v1/auth/login` and `/api/v1/auth/register/*` are no longer routed. The frontend uses Keycloak's public client `jolijob-web` with Authorization Code + PKCE S256, then sends the resulting **access token**, not an ID token, to the gateway.

Keycloak is contacted directly for discovery, browser authentication, code exchange, refresh and logout. Its local issuer is `http://localhost:8081/realms/jolijob`; no Keycloak/OAuth route is proxied by this gateway. See [`../../identity/keycloak/`](../../identity/keycloak/README.md) for realm import, key rotation and native startup.

The gateway does not validate JWTs; protected downstream services validate signature, issuer, audience and required identity claims independently. Configure the same canonical HTTPS issuer throughout a production deployment. Changing a backend's transport URL does not change its token issuer.

No permissive CORS policy is added. Use a same-origin frontend or explicitly configure a trusted-origin policy. Do not expose internal lookup endpoints directly outside a trusted network.

## Local build and run

From the repository root, with JDK 25:

```sh
./mvnw -f microservices/api-gateway/pom.xml verify
./mvnw -f microservices/api-gateway/pom.xml spring-boot:run
```

Listener defaults to port 8080 (`SERVER_PORT` override). Route tests use ephemeral loopback backends; no running sibling service is required. Containerization remains deferred.
