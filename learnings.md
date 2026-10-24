# Learnings

Mistakes made in this repository and corrected, so they are not repeated. See the **Memory
enforcement rule** in [CLAUDE.md](CLAUDE.md): read this before a non-trivial task, and append here
the moment a correction lands.

Format: what was believed → what is actually true → how to avoid it.

---

## JPA & Hibernate

### Hibernate flushes inserts before deletes
**Believed:** replacing an owned `@OneToMany` collection could be done by clearing it and adding the
new members.
**Actually:** within one flush, Hibernate issues the inserts first. Re-adding a member that already
exists violates the unique constraint before the delete of its old row runs.
**Avoid:** never clear-and-add an entity collection that has a unique key. Give the entity
business-key `equals`/`hashCode`, then reconcile with `retainAll(desired)` followed by add-if-absent.
Members that survive keep their row and id, so nothing is re-inserted. `Role.replacePermissions` is
the pattern, and `RoleRepositoryIT` guards it. An `@ElementCollection` of `@Embeddable` values also
avoids the problem, because Hibernate rewrites the whole table.

### `saveAndFlush` merges rather than flushing your instance
**Believed:** `saveAndFlush(managedEntity)` would flush and populate ids on the objects held in
memory.
**Actually:** `save` on a managed entity calls `merge`, which persists *copies*. The original
instances are left without ids, so a DTO mapped from them has `id: null`.
**Avoid:** when the entity is already managed inside a transaction, call `repository.flush()`, not
`saveAndFlush(entity)`.

### `@ElementCollection` has no key of its own
**Believed:** a collection table would get a primary key or uniqueness automatically.
**Actually:** Hibernate creates only the foreign key column plus the element columns. Duplicates are
possible at the database level.
**Avoid:** declare it explicitly on `@CollectionTable(uniqueConstraints = @UniqueConstraint(...))`,
and put `@OnDelete(CASCADE)` on the collection for the database-level cascade.

### Database cascades are invisible inside a `@Transactional` test
**Believed:** deleting a parent in an integration test would show its cascaded children gone.
**Actually:** `ON DELETE CASCADE` runs in the database. Inside a test transaction the statement has
not been sent, and the persistence context still holds stale objects.
**Avoid:** `entityManager.flush()` then `entityManager.clear()` around the delete, so the statement
reaches the database and the re-read comes back from it.

## Spring

### A `@Bean` method must not share its `@Configuration` class's name
**Believed:** naming the bean method after the concept it produces was harmless.
**Actually:** Spring registers the `@Configuration` class itself under its own name in lower camel
case. `class ComputerSystemSecuredEntity` with `@Bean computerSystemSecuredEntity()` throws
`BeanDefinitionOverrideException` at context startup, failing every integration test at once with a
message that does not name the real cause.
**Avoid:** name the method for what it returns, distinct from the class — `securedComputerSystem()`.

### A catch-all exception handler swallows Spring's own MVC exceptions
**Believed:** `@ExceptionHandler(Exception.class)` was a safe net for unexpected errors only.
**Actually:** it also catches `HttpRequestMethodNotSupportedException`, `NoResourceFoundException`
and friends, turning a 405 or 404 into a 500 — and, here, emailing the admin a critical alert for
every client typo.
**Avoid:** in the catch-all, check `instanceof org.springframework.web.ErrorResponse` first and
return that exception's intrinsic status without alerting. `GlobalExceptionHandler` does this.

### A single-type import shadows a same-package class
**Believed:** importing `org.springframework.web.ErrorResponse` into a package that already declares
its own `ErrorResponse` would be a compile error.
**Actually:** it compiles — the explicit import wins — but the IDE and any reader see two different
types with one name.
**Avoid:** fully qualify one of them, or delete the redundant class. `com.demo.platform.exception`
declares no `ErrorResponse`, so `GlobalExceptionHandler` can use Spring's without ambiguity.

### Spring Security 7 adds a `FACTOR_PASSWORD` authority
**Believed:** a principal with no roles would have an empty authority collection.
**Actually:** password logins are tagged with `FACTOR_PASSWORD` for multi-factor support.
**Avoid:** when asserting "no authorities", filter out `FACTOR_*` first — it is framework metadata,
not a directory group.

### A `PermissionEvaluator` bean does nothing without `@EnableMethodSecurity`
**Believed:** declaring a `PermissionEvaluator` `@Component` made `@PreAuthorize("hasPermission(...)")`
work, and its unit test proved it.
**Actually:** without `@EnableMethodSecurity`, `@PreAuthorize` is never processed — the annotated
method runs for everyone, with no error. A unit test that calls the evaluator directly bypasses the
annotation path, so it cannot catch the missing wiring. `RbacPermissionEvaluator` shipped this way
and was deleted unused.
**Avoid:** prove any annotation-driven security with an integration test that sends a request
through the annotated method and expects a 403. Enforcement here stays in `AccessControl`.

## Testing

### `jsonPath` on an array needs a collection matcher
**Believed:** `jsonPath("$.items[*].field", containsString("x"))` would work.
**Actually:** the expression yields a `JSONArray`, and `containsString` is a string matcher, so it
fails with a confusing type error.
**Avoid:** use `hasItem(...)`, `containsInAnyOrder(...)` or `hasSize(...)` for `[*]` expressions.

### MockMvc default headers only apply if the request has none
**Believed:** a `defaultRequest` with `httpBasic(...)` could be overridden per request.
**Actually:** MockMvc merges default headers only when the request does not already set that header,
and `httpBasic()` as a post-processor does not compose the way a raw header does.
**Avoid:** `AsAdminMockMvc` sets a plain `Authorization` header as the default, so
`asUser(...)` / `anonymous()` can remove and replace it per request. Follow that pattern.

## Process

### Do not promise a size reduction without measuring
**Believed:** a refactor's line savings can be estimated from what it deletes.
**Actually:** estimates made that way run several times too high. They assume a simpler design than
the one actually built, and they ignore what gets added in the same change, which can leave the code
net larger.
**Avoid:** measure the before state with `wc -l`, and count additions as well as deletions, before
quoting a number. If a refactor is a modelling improvement rather than a size win, say that up
front.

### Docs describe the current state, not how it got there
**Believed:** when a design changed, a doc's earlier description could stay, with a note like
"later reversed: …".
**Actually:** `docs/` is read by newcomers who need the design as it is. Change-over-time notes make
them reconstruct the present from a sequence of edits.
**Avoid:** rewrite the affected passage in the present tense. History belongs in commit messages and
the README's "Recent Updates" changelog. Deleted code is the exception: remove every mention of it
from `README.md` and `docs/`, old changelog bullets included, and write no removal note — the docs
read as if it was never there (corrected when `RbacPermissionEvaluator` was deleted).

### `cd` inside a Bash tool call persists
**Believed:** each Bash invocation started in the project root.
**Actually:** the working directory persists between calls, so an earlier `cd` into a subdirectory
made a later `mvn` fail with "no POM in this directory".
**Avoid:** use absolute paths, or `cd` to the project root at the start of each script.
