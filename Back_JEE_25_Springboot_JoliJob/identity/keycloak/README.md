# JoliJob Keycloak realm

This directory contains native Keycloak realm configuration and guarded
launchers for an existing Keycloak installation. It does not install Keycloak,
change application services, or create an administrator, user, password,
client secret, or signing key.

## Local development

Use an existing Keycloak installation and a persistent PostgreSQL database.
Both launchers refuse to start unless `KC_DB=postgres`,
`KC_DB_URL=jdbc:postgresql://...`, and non-empty `KC_DB_USERNAME` and
`KC_DB_PASSWORD` are supplied through the environment. Neither can fall back to
Keycloak's development-file database. Database credentials are never printed
by the launchers.
Keycloak's development mode uses HTTP and is suitable only for a local machine.
The provided launcher binds strictly to IPv4 loopback (127.0.0.1), fixes the
local issuer, and accepts no arguments that could override those safeguards.
The realm permits HTTP only for local development addresses; non-local requests
require HTTPS.

Set bootstrap administrator credentials in the shell or a secret manager; do
not put them in this repository. Replace every placeholder below with values
supplied privately before running the commands:

```sh
export KC_BOOTSTRAP_ADMIN_USERNAME='<local-admin-name>'
export KC_BOOTSTRAP_ADMIN_PASSWORD='<locally-supplied-secret>'
export KC_DB=postgres
export KC_DB_URL='jdbc:postgresql://localhost:5432/keycloak'
export KC_DB_USERNAME='<database-user>'
export KC_DB_PASSWORD='<database-secret>'
```

Import the realm into a fresh Keycloak database:

```sh
KEYCLOAK_HOME=/path/to/keycloak \
KC_DB=postgres \
KC_DB_URL='jdbc:postgresql://localhost:5432/keycloak' \
KC_DB_USERNAME='<database-user>' \
KC_DB_PASSWORD='<database-secret>' \
/path/to/keycloak/bin/kc.sh import --file identity/keycloak/jolijob-realm.json
```

After the offline import completes successfully, start Keycloak with the
guarded local launcher:

```sh
KEYCLOAK_HOME=/path/to/keycloak identity/keycloak/run-local.sh
```

The issuer is `http://localhost:8081/realms/jolijob`. The browser client is
`jolijob-web`; it uses Authorization Code with PKCE S256 and has exactly one
redirect URI, `http://localhost:4200/callback`, and one allowed web origin,
`http://localhost:4200`. Configure the browser app to use that callback URL.
The post-logout redirect is the origin root `http://localhost:4200/`. Do not
replace either redirect URI with a wildcard.

The realm has registration disabled. New accounts must be provisioned through
an administrator-controlled process. `CANDIDATE` is the only default realm
role; an administrator may assign `COMPANY` after review. Do not expose a
self-service role-selection flow. Access tokens are configured for five
minutes, and refresh-token reuse is disabled. `verifyEmail` is enabled.

The access token receives the `jolijob-api` audience. The standard OIDC subject
is Keycloak's stable user ID (`sub`); the standard `email` claim and
`realm_access.roles` are included through the built-in email and roles scopes.
These settings provide token claims, not service-side authentication or
authorization; each relying service must validate the issuer, signature,
expiration, audience, and required role.

## Application identity migration

The old identity module has been removed; the seven Spring services use the
external Keycloak issuer and must not expect the removed `/api/v1/auth` route
to exist. Their JWT resource-server configuration must validate the issuer
`http://localhost:8081/realms/jolijob` locally, the `jolijob-api` audience,
and required realm roles. Authentication code must use the verified JWT `sub`
as the Keycloak user identifier. Keycloak generates its normal UUID user ID
for `sub`; no custom mapper should replace it.

Do not assume a pre-Keycloak user UUID is the same as the Keycloak `sub`.
Preserve existing application records and UUID primary keys. Migrate identity
references deliberately by associating each legacy user UUID with the verified
Keycloak `sub` in an explicit `user_id` mapping/profile field (or a dedicated
unique mapping table), after a trusted account-linking or administrator-led
verification process. Backfill and reconcile this association as a separate,
reviewed data migration; do not silently rewrite foreign keys, delete profiles,
or infer account ownership from an unverified email. This realm import and its
launchers do not mutate application data or perform account linking.

For browser sign-in, use a maintained OIDC client library that supports the
Authorization Code flow with PKCE, and let the library manage `state`, `nonce`,
the PKCE verifier/challenge, callback validation, and token handling. Do not
implement the protocol by hand. Keep tokens out of `localStorage`: prefer
in-memory token storage and short lifetimes; use a backend-for-frontend with
Secure, HttpOnly, appropriately SameSite cookies when persistent browser
sessions are needed. Do not put a client secret in the browser.

## Signing keys and recovery

The realm import configures an enabled, generated RSA signing-key provider for
RS256 with a 4096-bit key (Keycloak non-FIPS supports 1024/2048/4096 only), explicit `active=true`, and higher priority than
Keycloak's default RSA provider. Keycloak selects the highest-priority enabled
provider with an active key pair for signing.
Keycloak generates and stores its private key in its database; this file
contains no key material. Do not use an ephemeral database or discard the
database volume: doing so loses the signing key and invalidates tokens.

Back up the Keycloak database using your normal encrypted backup process, and
protect backup access as production credential material. Test restoration
before relying on it. For rotation, activate a new signing key/provider, check
that Keycloak publishes its public key through the realm JWKS endpoint, and
keep old public keys available for signature verification until tokens signed
with them and any relevant sessions have expired. Do not delete an old key
while a relying service may still need it to validate an unexpired token.
Coordinate JWKS cache refresh and the rotation window with relying services.
An external KMS/HSM or Vault-backed signing integration is not configured by
these files; use one only after implementing and validating the corresponding
Keycloak provider and operational procedures.

## Production requirements

This realm file is not a complete production deployment. Use a supported
Keycloak release, PostgreSQL with encrypted, access-controlled backups, and
HTTPS with a valid certificate at a stable canonical hostname. Do not expose
the administration console or bootstrap-admin credentials publicly. Supply
all database and bootstrap-admin credentials outside this repository through
the platform's secret manager. Configure the deployment's hostname and TLS
termination consistently with the public issuer, and verify the resulting
issuer URL before configuring clients or services. `sslRequired: external`
allows local HTTP development but requires HTTPS for non-local clients.

Realm import should be used for initial provisioning into an empty database.
Do not re-import over a live realm without a reviewed backup and explicit
overwrite plan; realm import and overwrite behavior can replace existing
configuration. The frontend callback is intentionally restricted to the
localhost development origin. Set exact production redirect and web-origin
URLs before production use.

Use `run-production.sh` only with a preinstalled Keycloak distribution and
externally supplied production environment. It refuses to start unless
`KC_HOSTNAME` is a canonical `https://` hostname and TLS is configured by one
of these two explicit choices:

1. Direct TLS: set `KC_HTTPS_CERTIFICATE_FILE` and
   `KC_HTTPS_CERTIFICATE_KEY_FILE`. The PEM private key must be a readable
   regular file owned by the launcher user with no group or world permissions
   (for example mode `600`); certificates and private keys must be provisioned
   and rotated outside this repository.
2. Trusted TLS-terminating proxy: set `KC_HTTP_ENABLED=true`,
   `KC_PROXY_HEADERS` to the exact header family configured by the proxy
   (`forwarded` or `xforwarded`), and `KC_PROXY_TRUSTED_ADDRESSES` to the
   narrowly scoped proxy IPs/CIDRs. The proxy must reject untrusted incoming
   forwarding headers, terminate HTTPS, and be the only network path to
   Keycloak's HTTP listener. Do not expose that listener directly.

The launcher sets strict hostname handling and rejects ambiguous configurations
or extra command-line arguments. It cannot verify the TLS termination,
firewall, proxy header sanitation, certificate chain, database security, or
administrator access controls; operators must validate those deployment
controls before publishing the issuer.

```sh
KEYCLOAK_HOME=/path/to/keycloak \
KC_HOSTNAME=https://identity.example.com \
KC_DB=postgres \
KC_DB_URL='jdbc:postgresql://db.example.com:5432/keycloak' \
KC_DB_USERNAME='<database-user>' \
KC_DB_PASSWORD='<database-secret>' \
identity/keycloak/run-production.sh
```

Provide bootstrap-admin credentials only through a protected secret manager or
runtime environment. Never commit them, print them in logs, or expose the
administration console publicly. Neither launcher installs Keycloak nor
automatically creates an administrator.

The launcher guard tests use a temporary fake Keycloak executable and do not
need a Keycloak installation:

```sh
bash identity/keycloak/tests/launcher-guards.sh
```
