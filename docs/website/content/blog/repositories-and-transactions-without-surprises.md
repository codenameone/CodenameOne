---
title: "Spring Data-Style Repositories Compiled to Native Code"
slug: repositories-and-transactions-without-surprises
url: /blog/repositories-and-transactions-without-surprises/
date: '2026-10-17'
author: Shai Almog
description: "Use Spring Data-style repositories and Hibernate persistence conventions with generated Java implementations, native compilation and Spring-style transaction boundaries."
feed_html: "<img src=\"https://www.codenameone.com/blog/repositories-and-transactions-without-surprises.jpg\" alt=\"Repository queries and transaction completion in one application\" /> Use Spring Data-style repositories and Hibernate persistence conventions with generated Java implementations, native compilation and Spring-style transaction boundaries."
series: ["release-2026-10-16"]
---

![Repository queries and transaction completion in one application](/blog/repositories-and-transactions-without-surprises.jpg)

If you use Spring Data, a method such as `findFirstByDriverAndStateIn` tells you what a repository does before you open its implementation. Codename One now brings that repository model to both the client and backend, with generated Java code that can compile directly into a native executable.

The advantage is familiar data access with the runtime setup removed. The build generates concrete repository implementations. Native packaging compiles them ahead of time, so startup doesn't need to scan for repositories or generate proxies. Direct calls also give the compiler code it can analyze and optimize.

This week's [Wayline application](/blog/create-an-uber-clone-in-1-minute/) uses these repositories for its ride-hailing service. Alongside them, Spring-style transaction isolation and completion callbacks let you control when another part of the system can act on a database change.

## Put the query where you can find it

Wayline's repository declares what it needs to read. This shortened interface comes from its [RideRepository](https://github.com/codenameone/CodenameOne/blob/master/scripts/wayline/backend/src/main/java/com/codenameone/examples/wayline/repository/RideRepository.java):

```java
import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Repository;
import com.codenameone.examples.wayline.api.RideState;
import com.codenameone.examples.wayline.domain.Ride;

@Repository(entity = Ride.class, key = String.class)
public interface RideRepository extends CrudRepository<Ride, String> {
    Ride findFirstByRiderAndStateIn(String rider, RideState[] states);
    Ride findFirstByDriverAndStateIn(String driver, RideState[] states);
}
```

The build generates the implementation. It makes direct calls into the session; it doesn't discover repositories by scanning classes at runtime. There is no Hibernate dependency or runtime proxy generation. The same repository API works with client ORM sessions and backend sessions.

Spring Data users will recognize the naming convention. Hibernate users will recognize managed entities and dirty checking. These are supported conventions under Codename One's packages, with a deliberately bounded API. Existing Spring or Hibernate dependencies don't become portable just because the method names look familiar.

The inherited methods include `insert`, `save`, `findById`, `findAll`, `count`, `existsById` and `deleteById`. There are a couple of details to remember: a missing `findById` returns `null`, and `save` returns the managed object produced by `merge`. Keep that returned value if you continue changing it.

## Static compilation does the setup work once

A repository interface becomes a concrete implementation during the build. With ParparVM, that Java implementation joins the rest of the application in the native binary. There is no reflective repository dispatch or runtime implementation factory to initialize.

That removes work from startup and from dispatching repository calls. It doesn't remove the cost of the database query: indexes, round trips and the number of rows still matter. `CASE` aggregation and pagination help keep that work in the database instead of loading unnecessary entities.

## A long method name isn't always the clearest query

Derived finders cover comparisons, `And`/`Or`, explicit ordering and limited results. A final `PageRequest(offset, size)` gives a list query a page without issuing a separate total-count query. Include a unique tie-breaker in the ordering so rows with equal scores don't wander between pages.

For a conditional aggregate, declare the query. This shortened example uses the guide's `RepositoryRecord` entity, which has a numeric `score`:

```java
@Query("select sum(case when r.score >= :floor then 1 else 0 end) "
        + "from com.codenameone.developerguide.snippets.repository.RepositoryRecord r")
Long qualified(@Param("floor") long floor);
```

`CASE` expressions let the database count qualifying rows instead of making the app fetch them all. Named values are bound parameters. Native SQL queries are available too, with a narrower scalar-result API and the portability costs of the SQL you choose.

The [repository reference](/developer-guide/annotation-sqlite-orm/#generated-orm-repositories) spells out the grammar. Unsupported method signatures fail generation. JPQL syntax and mapped names are checked when the method creates its query, before SQL execution. That distinction matters: this isn't a promise that every query error becomes a compile error.

## Keep the transaction in the service

Repositories don't open or commit transactions. The service owns that boundary. Here is Wayline's actual read path:

```java
@Transactional(readOnly = true)
public RideDto activeForRider(String rider) throws IOException {
    Ride ride = rides.findFirstByRiderAndStateIn(rider, ACTIVE);
    return ride == null ? none() : toDto(ride, rider, new HashMap<String, Profile>());
}
```

The snippet sits inside `Rides`, with its repository and mapping helpers. Methods that change several entities use a transaction around that unit of work. A repository's session participates in it, so changing a managed entity can be flushed when the transaction completes.

Bulk operations deserve separate attention. Derived deletes and declared modifying queries flush pending changes, execute bulk SQL and clear the persistence context. They bypass per-entity callbacks and cascades. Reload objects before continuing; references you kept still contain their old Java values.

## Choose isolation at the outer boundary

The annotation now accepts an isolation level:

```java
import com.codename1.backend.annotations.Isolation;
import com.codename1.backend.annotations.Transactional;

// Annotation for a service method that owns the transaction:
@Transactional(isolation = Isolation.SERIALIZABLE)
```

This is an annotation excerpt, to place on the method that starts the work. It requests a physical transaction with that level. It doesn't silently upgrade an outer transaction that already exists.

| Database | Explicit isolation levels |
| --- | --- |
| SQLite | `SERIALIZABLE` |
| PostgreSQL | `READ_COMMITTED`, `REPEATABLE_READ`, `SERIALIZABLE` |
| MySQL/MariaDB | Those three plus `READ_UNCOMMITTED`, with transactional tables |

A joined scope requesting an explicit level must match the outer scope's explicit level. Joining a `DEFAULT` scope with an explicit request fails. Choose the level at the start, or use `REQUIRES_NEW` when the operation really needs an independent transaction and connection.

## Notify after the database agrees

Inside a thread-bound transaction, register a `Transactions.Synchronization`. This is the guide's callback shape, with comments marking where application work belongs:

```java
Transactions.registerSynchronization(new Transactions.Synchronization() {
    public void beforeCommit(boolean readOnly) {
        // Validate before the ORM flush and commit.
    }
    public void afterCommit() {
        // Invalidate a cache or schedule an event.
    }
    public void afterCompletion(Transactions.Outcome outcome) {
        // Release resources for COMMITTED, ROLLED_BACK, or UNKNOWN.
    }
});
```

{{< mermaid >}}
sequenceDiagram
    participant Service
    participant DB as Database
    participant Hook as Completion callback
    Service->>DB: Change rows
    Service->>DB: Commit
    DB-->>Service: Commit acknowledged
    Service->>Service: Close session and release connection
    Service->>Hook: afterCommit
    Service->>Hook: afterCompletion
{{< /mermaid >}}

The connection has already returned to the pool when `afterCommit` runs. More database work needs a new transaction. If a callback fails, the original database change remains committed and the caller receives an exception saying so. Retrying the entire operation blindly can duplicate the work.

These callbacks are synchronous and in memory. They don't make event delivery durable across a process crash. For a message that must eventually arrive, write an outbox record in the original transaction and deliver it separately.

Wayline currently expresses a similar ordering with a transactional private method followed by its notification code. The compiler writes transaction handling into the method, so a call through `this` doesn't bypass it. Completion hooks give other services an explicit place for that post-commit work.

The [transaction guide](/developer-guide/backend-data/) covers nested scopes, failure outcomes and connection ownership. [Repositories and CASE expressions](https://github.com/codenameone/CodenameOne/pull/6007) and [isolation and callbacks](https://github.com/codenameone/CodenameOne/pull/6012) are separate additions that become more useful together: less query plumbing, with the important ordering still visible.

## Discussion

_Which Spring Data repository pattern would you want to bring to a native Java app first?_

{{< giscus >}}
