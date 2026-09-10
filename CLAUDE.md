# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Constitution & principles

**Know who the output is for.**

| Output | Audience | Implication |
|---|---|---|
| Code review, analysis, plans | Senior engineers | Skip language and framework basics. Give the reasoning and the trade-off, not a tutorial. |
| `README.md`, `docs/` | Juniors and newcomers | Explain *why*, define terms, show a worked example. Assume no knowledge of this codebase. |
| Commit messages | Reviewers | What changed and why, including where a change fell short of its estimate. |

**Speed matters, understanding matters more.** A fast answer that leaves the reader unsure why it
works has failed. Length is not understanding either — do not pad.

**Comment the decision, not the code.** A comment restating what the code plainly does is noise;
delete it. One explaining a trade-off, a rejected alternative or a non-obvious constraint earns its
place permanently — see the `@BatchSize` javadoc on `User.departmentLinks` or `Role.permissions`.

**Rule of three for abstraction.** No abstraction, helper or interface until the pattern has appeared
**three times**; duplication is cheaper than the wrong abstraction. On the third case, extract it and
delete the duplicates in the same change.

**Report outcomes honestly.** If tests fail, show the output. If a refactor grew the code, say so
with the numbers. Never call work verified that has not been run.

## Tech stack & versions

| | |
|---|---|
| Java / Boot | 25 / Spring Boot 4.1.0 (Framework 7, Security 7.1, Hibernate 7) |
| JSON | **Jackson 3** — `tools.jackson.databind.ObjectMapper`, *not* `com.fasterxml.jackson.databind`. Annotations stay `com.fasterxml.jackson.annotation.*` |
| Persistence / auth | Spring Data JPA, H2 in-memory (`create-drop`); Spring Security + embedded UnboundID LDAP |
| Other | Resilience4j 2.4.0, SpringDoc 3.0.3, Lombok, JUnit 5, Mockito, Maven 3.9.11 (no wrapper) |

**Version traps.** Boot 4 test slices are separate modules — `@WebMvcTest` needs
`spring-boot-webmvc-test`, `@DataJpaTest` needs `spring-boot-data-jpa-test`, neither comes from
`spring-boot-starter-test`. Spring Security 7 tags password logins with a `FACTOR_PASSWORD`
authority; filter it out when asserting a principal has no authorities.

## Architecture & design patterns

Organised **by feature**, not by layer: `com.demo.feature.{user,department,computersystem,security}`
hold business capabilities, `com.demo.platform` holds `BaseEntity`, config and exceptions, and
`com.demo.integration` holds outbound adapters (mail).

**Cross-feature rule.** Entities and DTOs may reference each other directly, but never inject another
feature's repository. Go through its public service (`DepartmentService.resolveDepartments`,
`UserManagementService.resolveUser`, `RoleService.resolveRole`), which owns the 404 behaviour.

**Constructor injection only.** No field injection, no `@Autowired` on fields. Lombok's
`@RequiredArgsConstructor` with `private final` dependencies, or an explicit constructor.

**DTO boundaries.** Entities never leave the service layer; controllers speak DTOs. Associations
follow **ids-in / objects-out**: requests send `<x>Ids`, responses populate both `<x>Ids` and the
nested read-only `<x>` objects. Mappers never hit the database — the service resolves associations.

**Error handling.** All errors are RFC 9457 `ProblemDetail` from `GlobalExceptionHandler`. Throw the
platform exception and it maps itself: `ResourceNotFoundException` (404), `DuplicateResourceException`
and `ConflictException` (409), `InvalidRequestException` (400), `AccessDeniedException` (403).
Spring's own MVC exceptions implement `ErrorResponse` and pass through with their intrinsic status —
never let the catch-all turn them into 500s. Business exceptions sit on the `databaseQuery` circuit
breaker's `ignoreExceptions` list; a burst of 403s must not open it.

### Authentication and RBAC

**Read [docs/rbac.md](docs/rbac.md) before touching authorization** — the developer guide, with
diagrams. [docs/rbac-alternatives.md](docs/rbac-alternatives.md) records why this is hand-written
rather than delegated to Spring Security's built-ins or a library.

Two invariants:

1. **Authorization is data.** Roles, their `entity:field → operation` permissions, and users'
   Department:Role grants are rows managed through the API. The only exception is the `SuperAdmin`
   role and the bootstrap admin's global grant, seeded by `RbacBootstrap`.
2. **Enforcement lives in the service layer**, not in URL patterns or annotations, because only the
   service knows the target object *and* its departments. Every guarded operation follows the same
   order — `UserManagementService` is the canonical form:

   `scopeOf` → `requireAccess` → *(update only:* `retainUnreadable` → `FieldDiff.changedFields` →
   `requireFieldAccess`*)* → business logic → `filterReadable`

Authentication is HTTP Basic bound against an in-process LDAP server from `ldap-users.ldif`. The app
stores no passwords and never writes to the directory; a user's first login provisions their `users`
row with **no grants**, so they can log in and do nothing. To secure a new entity, declare one
`SecuredEntity` bean in that feature (copy `UserSecuredEntity`), apply the service pattern above and
add a `<Entity>RbacIT` — nothing in `feature.security.rbac` changes.

**DTO rules RBAC silently depends on** — get these wrong and masking fails quietly:
`@JsonInclude(NON_NULL)` on the class (masked fields are nulled; this is what omits them from the
JSON); **wrapper types, not primitives** (a `boolean` cannot be nulled, so it can never be masked);
and `@Schema(accessMode = READ_ONLY)` on derived properties (`departments`, `roleAssignments`) so
`FieldDiff` excludes them from the writable set and an echoed-back value is not read as a write.

### Fetching: entity graphs vs `@BatchSize`

Getting this backwards is a silent performance bug, not a failure. **Single-entity reads**
(`findById`, `findByUsername`) use `@EntityGraph` with collections included. **Paged reads**
(`findAll(Pageable)`) graph only to-one associations — a collection-fetching graph plus pagination
makes Hibernate paginate **in memory** (`HHH000104`), loading the whole result set; collections use
`@BatchSize` instead. `ComputerSystemDepartmentFetchIT` asserts query count does not grow with page
size.

### Departments and deletion

Deleting a department is **never blocked**. Each owner links through its own join entity
(`UserDepartment`, `ComputerSystemDepartment`) with `@OnDelete(CASCADE)` on *both* foreign keys, so
the database dissociates every owner type, including ones added later; role assignments cascade the
same way. Use `DepartmentLinks.replace(...)` to reconcile links and
`DepartmentSpecifications.assignedToDepartment` / `assignedToAnyDepartment` to filter; name the
collection exactly `departmentLinks` so those specifications resolve it.

> These cascades exist only because Hibernate generates the schema (`ddl-auto`). Adopting Flyway
> means writing `ON DELETE CASCADE` into the migrations or dissociation silently stops working.
> `DepartmentCascadeIT` asserts `DELETE_RULE = CASCADE` from `INFORMATION_SCHEMA`.

## Code conventions & style

- **Naming**: `camelCase` members, `PascalCase` types, `SCREAMING_SNAKE` constants. Test methods are
  sentences — `replacePermissions_makesTheSetExactAndCollapsesDuplicates`.
- **Import order**: non-`java` imports, blank line, `java.*`, blank line, static imports. Each group
  alphabetical. No wildcard imports in main code.
- **`@Bean` method names must not equal the enclosing `@Configuration` class name** in lower camel
  case — Spring registers the class under that name and the definitions collide.
- **Lombok**: `@Getter`/`@Setter`/`@RequiredArgsConstructor` freely; `@SuperBuilder` on `BaseEntity`
  subclasses; `@Builder.Default` on initialised collections.
- **Specifications over `@Query`** for dynamic filtering: extend `JpaSpecificationExecutor`, add an
  `<Entity>Specifications` class.

## Development commands

```bash
mvn verify                      # THE build gate: unit (*Test, surefire) + integration (*IT, failsafe)
mvn test                        # unit tests only
mvn test -Dtest=RoleServiceTest # one unit test class
mvn clean package -DskipTests   # build the jar
mvn spring-boot:run             # run on :8080 (or: java -jar target/api-demo-1.0.0.jar)
# one integration test class — surefire must be told to skip, or it fails on "no tests"
mvn verify -Dtest=NONE -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=UserRbacIT
```

`mvn test` alone proves nothing about the integration suite — **always finish with `mvn verify`.**

Manual calls need HTTP Basic credentials from `src/main/resources/ldap-users.ldif`:
`curl -u admin:admin123 http://localhost:8080/api/v1/roles`. Swagger UI at `/swagger-ui.html`, H2
console at `/h2-console` (`jdbc:h2:mem:testdb`, user `sa`, no password); `create-drop` means every
restart is a fresh schema with a re-seeded bootstrap admin. After editing a doc, re-render its
diagrams: `npx -y -p @mermaid-js/mermaid-cli mmdc -i docs/rbac.md -o <scratch>/out.md`

**Permission allow-list policy** (`.claude/settings.local.json`, personal and gitignored):
allow `mvn` (all goals), `git status`/`diff`/`log`/`add`, and read-only inspection. Never allow
`git push`, `git rm`, `git reset --hard`, force operations, or any direct database command — those
stay prompt-on-use.

## Project files & memory map

| Path | What it is |
|---|---|
| `CLAUDE.md` | This file. **Keep it 150–200 lines** — it loads into every session. |
| `learnings.md` | **Accumulated corrections.** Read at session start, append when corrected. |
| `README.md` | User-facing reference: endpoints, config keys, entity fields. |
| `docs/rbac.md` | RBAC developer guide with Mermaid diagrams. |
| `docs/rbac-alternatives.md` | Decision record: why RBAC is hand-written. |
| `bruno-api/` | Bruno API collection, collection-level Basic auth as `admin`. |
| `src/main/resources/ldap-users.ldif` | The directory: who can log in, with what password. |

## Memory enforcement rule

1. **Read `learnings.md` at the start of any non-trivial task.**
2. **When corrected — by a human, a failing test, or a discovered bug — append the learning** in the
   same session, while the context is fresh: what was believed, what is true, how to avoid it. Do
   not wait to be asked. Keep entries specific and verifiable; delete obsolete ones.
3. **Never re-introduce a documented mistake.** If a learning conflicts with what you are about to
   do, the learning wins unless you can show it is now wrong — then correct the entry.

## Workflow: plan → execute → verify

**1. Plan.** Break the problem into tasks, then each into sub-tasks. Prefer existing patterns to
invention. Merge outcomes into existing docs rather than creating parallel ones. State assumptions
explicitly so they can be vetoed.

**2. Execute** one step at a time.

**3. Verify** before moving on: check IDE diagnostics including SonarLint (`java:Sxxxx`), run
`mvn verify` (not just `mvn test`), hand to a review subagent if one is configured.

Repeat 2–3 per step. Do not batch verification to the end.

## Not yet adopted

Wanted, not present — do not assume these exist: **Testcontainers** (currently H2), **Flyway or
Liquibase** (currently `ddl-auto: create-drop`, see the cascade warning), **split documentation**,
**custom slash commands** (test-and-fix, adversarial diff review, commit), **subagents** (security,
review).
