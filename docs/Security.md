# Security Architecture

This document summarizes the authentication and authorization design for the API Demo project.

## Overview
- Authentication: Remote Active Directory (LDAP) for corporate users; MariaDB-backed internal admin accounts (BCrypt-hashed passwords) as a fallback.
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

## BCrypt and admin accounts
- Internal admin accounts will store a BCrypt password hash in the database.
- `security.password.bcrypt.strength` controls the BCrypt cost factor.
- Risks: storing password hashes increases attack surface; ensure DB is encrypted at rest, use strong access controls, and provide password rotation/recovery procedures.
- Alternatives: use LDAP-only admin accounts or an external identity provider.

## Token revocation and persistent tokens
- Persistent API tokens are stored in `api_tokens` and checked during authentication to allow immediate revocation.
- Session records are stored in `sessions` if DB-backed session store is enabled.

## Rate limiting
- The integrated gateway uses per-instance rate limiting by default (no Redis). This is simple and fast, but not globally consistent across replicas.
- For distributed global limits, consider adding a shared datastore or dedicated rate-limiter service in the future.

## Deployment Notes
- Ensure `security.jwt.private-key` value is provided at deploy time via keystore or secret manager.
- Configure LDAP connection details via environment variables: `LDAP_URLS`, `LDAP_BASE_DN`, `LDAP_USER_SEARCH_FILTER`.
- Enable gateway features by setting `app.gateway.enabled=true` (default in this project).
