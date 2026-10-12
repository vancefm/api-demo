# Why this RBAC is hand-written — alternatives evaluated

A decision record. Before living with a bespoke authorization layer, two questions were asked:
**could stock Spring Security have given us this**, and **is there a library that would**? This
documents the answers, so the question is not re-litigated from scratch.

The implementation it justifies is described in [docs/rbac.md](rbac.md).

## The requirement that decides everything

Three things are required together:

1. **Dynamic** roles and permissions, stored in the database and managed through the API at runtime.
2. **Field-level** grants: a role may read `User.firstName` and update `User.lastName` and nothing
   else, on both reads and writes.
3. **Department-scoped** grants: a user holds a role *within a department*, and list endpoints must
   stay correctly paginated while scoped.

Requirement 1 alone is ordinary and well served by several options. **Requirements 2 and 3 are what
rule everything out.** No general-purpose authorization product models field-level permissions, and
none can scope a paginated SQL query — filtering after the fact corrupts page totals.

Field-level access was confirmed as a firm requirement. It is the single largest cost driver in the
implementation, and it is the reason this document ends where it does.

## Stock Spring Security

Spring Security 7.1 is already on the classpath and offers more than this codebase uses:

| Capability | Stock mechanism | Used here? |
|---|---|---|
| Roles/permissions from a database | `UserDetailsService` + `GrantedAuthority` | No — grants are resolved per request instead |
| Object-level check hook | `PermissionEvaluator` + `hasPermission()` SpEL | **Yes, now** — see `RbacPermissionEvaluator` |
| Field-level read masking | `@AuthorizeReturnObject` + `@HandleAuthorizationDenied` + `NullReturningMethodAuthorizationDeniedHandler` | No — `FieldAccessFilter` does it directly |
| Collection filtering | `@PostFilter` | No, deliberately |

**What it genuinely provides.** Loading permissions from the database into the `Authentication` is
free. So is an object-level hook: `PermissionEvaluator` is the designed extension point, and an
adapter over `AccessControl` now implements it, so `hasPermission(#dto, 'UPDATE')` works in SpEL for
anyone who prefers annotations.

Most notably, since 6.3 Spring Security **does** offer field-level read masking. `@AuthorizeReturnObject`
proxies a returned object and applies `@PreAuthorize` on its getters; `@HandleAuthorizationDenied`
with the built-in null-returning handler makes an unauthorized getter yield `null` instead of
throwing. The documentation's own recipe then pairs that with `non_null` JSON inclusion so the field
disappears from the response.

**That is exactly the pattern this code already implements**, by hand: `FieldAccessFilter` nulls the
unreadable properties and `@JsonInclude(NON_NULL)` omits them. Applying the annotation per DTO rather
than Spring's suggested global property is slightly better, because the global setting would also
strip nulls from `ProblemDetail` error bodies.

It was **not** adopted because it would require a `@PreAuthorize` on every getter of every secured
DTO, it proxies each returned object, and it covers reads only — writes and list scoping would still
be hand-written. The gain would be replacing ~47 lines with a larger annotation surface.

**What it does not provide at any version:**

- The permission **storage and management API**. That is ordinary domain CRUD and belongs to us
  under every option considered.
- Field-level rules on **writes** — which fields an update or create may touch.
- **Pagination-safe scoping**. `@PostFilter` filters in memory after the query, which the Spring
  Security documentation itself flags as expensive and which would make `Page.totalElements` wrong.
  Scoping has to be a `Specification` on the query, as it is here.

### Spring Security ACL

The wrong shape, on three counts. It stores an access-control entry per object **row**, with no
concept of a field. Its collection filtering is post-fetch, so pagination breaks. And it is not even
a dependency this project has. Its per-instance model would also mean writing an entry per user per
object, where the requirement is "everyone with this role in this department".

## Libraries

| Option | Fit | Verdict |
|---|---|---|
| **jCasbin** | Closest conceptual match. Its *RBAC with domains* model is literally "user has role in domain" — our Department:Role grant — and JDBC/Hibernate policy adapters exist | Replaces only the matcher (~124 lines of `EffectivePermissions`) and the storage. Masking, write-diffing and query scoping remain ours. Trades a clean REST/JPA model for an opaque `casbin_rule` table. Spring Boot 4 support in the starter is unclear; the core is framework-agnostic |
| **OpenFGA / SpiceDB / Permify** | Zanzibar-style relationship engines, excellent at hierarchy and scope | External service to run and keep consistent. No field-level concept. Wrong weight class for this application |
| **Keycloak Authorization Services** | Full resource/scope/policy server with an admin UI | Moves policy out of the database we deliberately keep it in, and adds a server to operate |
| **Open Policy Agent** | Policy as code, very expressive | Policy stops being data managed through our API, which is the core requirement |
| **Apache Shiro** | Its `WildcardPermission` string `entity:operation:field` is the same model invented here | Adopting Shiro alongside Spring Security is a step backwards. The *idea* was borrowed instead: permissions are values, not entities |

## Verdict

Nothing off the shelf removes the field-level and scope-filtering machinery, which is where the
complexity actually lives. The role and permission CRUD that makes up most of the line count is
ordinary domain code that every option would still require.

So the architecture stays, and the volume was cut instead:

- **Permissions became values rather than entities.** A permission has no identity of its own, so
  the id, the audit columns and the separate unique constraint were removed. `Role` holds them in an
  `@ElementCollection` written to the `role_permissions` collection table. Replacing a role's
  permissions is now a clear-and-add rather than a hand-written diff, which existed only to avoid
  tripping the unique constraint on re-submit.
- **One route to edit permissions.** `PUT /api/v1/roles/{id}/permissions` replaces the whole set;
  the add-one and remove-one routes it subsumed are gone.
- **A `PermissionEvaluator` was registered**, so the design reads as Spring-native and annotation
  style checks are available where they fit.

Along the way the unused `ErrorResponse` class, dead since the RFC 9457 migration, was deleted, and a
pre-existing bug was fixed: the catch-all exception handler turned Spring's own MVC exceptions into
500s with a critical-alert email, so a wrong HTTP method returned 500 instead of 405.

## Also considered, and rejected

- **Folding grants into `PUT /users/{id}`** as a `roleAssignments` list, reusing the
  `DepartmentLinks.replace` pattern, would delete a controller and much of a service. It loses
  per-grant scope checks, so a department-scoped grant manager could no longer be constrained the
  way `RbacManagementIT` requires.
- **PATCH instead of PUT** would delete `FieldDiff.retainUnreadable` and the "UPDATE needs READ"
  rule, the subtlest part of the design. It is an API contract change for every entity.
- **Centralising masking in a `ResponseBodyAdvice`** would remove about a dozen `filterReadable`
  call sites, but makes enforcement invisible in the service, against the design's stated invariant
  that every check is visible where the object is known.
- **A single opaque permission string** (`"User:firstName:READ"`, Shiro style) saves more code than
  the embeddable, at the cost of an unreadable schema and parsing at every use.
- **Dropping field-level permissions** would shrink this system dramatically and land it close to
  stock Spring Security. It is a firm requirement, so the cost is accepted deliberately.

## If the requirements change

- If field-level grants were dropped, revisit this: `hasAuthority` plus a small scope check would
  cover most of what remains, and `@AuthorizeReturnObject` would cover the rest.
- If scope grew into a hierarchy (departments within divisions, or ownership chains), the
  Zanzibar-style engines become genuinely attractive and this hand-rolled scope check does not.
- If policy needed to be shared across several services, an external engine starts to pay for its
  operational cost. Inside one application it does not.
