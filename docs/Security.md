# Security Architecture

This document summarizes the authentication and authorization design for the API Demo project.

## Overview
- Authentication: Remote Active Directory for corporate users; MariaDB-backed internal admin accounts (BCrypt-hashed passwords) as a fallback; embedded LDAP (UnboundID) for local development and testing.
- Tokens: JWTs signed with an RSA-4096 private key (RS512). The public key is exposed via JWKS at `/.well-known/jwks.json` for verification.
- Gateway: Integrated Spring Cloud Gateway (same artifact) centralizes rate-limiting (per-instance), circuit breakers (Resilience4j), and response compression.
- Persistent API tokens and session store: Stored in MariaDB using `api_tokens` and `sessions` tables.

## Key handling
- The private signing key MUST NOT be stored in `application.yml` or checked into source control.
- The `security.jwt.private-key` property in `application.yml` is intentionally blank and serves only as documentation.
- Recommended deployment options:
  - Provide a PKCS#12 or JKS keystore and point the application to it using environment variables.
  - Provide the private key via a secure secret manager environment variable and ensure it is read at startup.
- Key requirements: RSA-4096 minimum for signing.

## JWKS endpoint
- The application exposes public keys at `/.well-known/jwks.json` so the gateway and other services can fetch verification keys.
- JWKS contains public key material only (no private key exposure).

## Roles and JWT claims
- When a user authenticates via `/api/v1/auth/login`, the issued JWT will include a `roles` claim: an array of role names assigned to the user (e.g., `["MY_APP_SUPERADMIN"]`).
- The integrated gateway and downstream services may use the `roles` claim to perform authorization checks. Roles are derived from the `User`'s `role.name` in the database, from Active Directory group mappings, or from embedded LDAP group mappings.
- Active Directory uses `sAMAccountName` for login. Group memberships are mapped directly to roles, and when no groups are returned the user receives the `MY_APP_USER` role.

## Embedded LDAP (Test / Local Development)
- An embedded UnboundID LDAP server is available for testing and local development. It is configured as a **test-scoped** dependency (`com.unboundid:unboundid-ldapsdk`).
- The embedded server is started via `EmbeddedLdapTestConfig` (`@TestConfiguration`) and seeded from `src/test/resources/test-ldap-users.ldif`.
- Base DN: `dc=demo,dc=com`. Users reside under `ou=people`, groups under `ou=groups`.
- LDAP group → application role mapping:
  - `GroupA-Users` → `ROLE_MY_APP_USER`
  - `GroupB-Admins` → `ROLE_MY_APP_ADMIN`
  - `GroupC-SuperAdmins` → `ROLE_MY_APP_SUPERADMIN`
- Seeded test users:
  - `user1` / `password1` — member of `GroupA-Users`
  - `user2` / `password2` — member of `GroupA-Users`
  - `admin1` / `admin123` — member of `GroupA-Users` and `GroupB-Admins`
  - `superadmin1` / `super123` — member of `GroupC-SuperAdmins`
- The `SecurityConfig` `AuthenticationManager` supports an optional `LdapAuthenticationProvider` via `ObjectProvider`. When the embedded LDAP test config is active, authentication follows the chain: LDAP → Active Directory (if enabled) → database fallback.
- Active Directory is disabled during tests (`security.active-directory.enabled: false` in `src/test/resources/application.yml`).

## Persistent API tokens
- Persistent tokens allow service accounts or developers to obtain a static API token for non-interactive use.
- Token format: `tokenId.secret` where `tokenId` is a UUID stored in the database and `secret` is a high-entropy random value shown only once at creation.
- Storage: the service stores only a BCrypt hash of the `secret` in the `api_tokens` table (column `token_hash`). The raw secret is returned to the caller only once.
- Validation: when an incoming Bearer token does not parse as a JWT, the app will attempt to interpret it as `tokenId.secret`, look up `tokenId`, verify the `secret` via BCrypt, check expiry and revoked flag, then authenticate the request as the token owner.
- Revocation: tokens can be revoked via DELETE `/api/v1/tokens/{tokenId}` which sets the `revoked` flag.

## One-time token display and security
- The API returns the raw persistent token value only at creation time. Store it securely; the server will not be able to show it again.
- The `api_tokens` table stores only a BCrypt hash of the secret. This protects against leakage of raw tokens if the DB is compromised.

## BCrypt and admin accounts
- Internal admin accounts will store a BCrypt password hash in the database.
- `security.password.bcrypt.strength` controls the BCrypt cost factor.
- Risks: storing password hashes increases attack surface; ensure DB is encrypted at rest, use strong access controls, and provide password rotation/recovery procedures.
- Alternatives: use Active Directory-only admin accounts or an external identity provider.

## Token revocation and persistent tokens
- Persistent API tokens are stored in `api_tokens` and checked during authentication to allow immediate revocation.
- Session records are stored in `sessions` if DB-backed session store is enabled.

## Rate limiting
- The integrated gateway uses per-instance rate limiting by default (no Redis). This is simple and fast, but not globally consistent across replicas.
- For distributed global limits, consider adding a shared datastore or dedicated rate-limiter service in the future.

## Deployment Notes
- Ensure `security.jwt.private-key` value is provided at deploy time via keystore or secret manager.
- Configure Active Directory connection details via environment variables: `AD_URL`, `AD_DOMAIN`, `AD_ROOT_DN`, `AD_USER_SEARCH_FILTER`, `AD_GROUP_SEARCH_BASE`, `AD_GROUP_SEARCH_FILTER`, `AD_MANAGER_DN`, `AD_MANAGER_PASSWORD`.
- Enable gateway features by setting `app.gateway.enabled=true` (default in this project).
- The embedded LDAP server is **test-scoped only** and not available in production builds. Tests that import `EmbeddedLdapTestConfig` will start the server automatically; no external LDAP infrastructure is required for running the test suite.
