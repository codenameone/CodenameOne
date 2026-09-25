---
title: "JPA Inspired ORM from SQLite to PostgreSQL"
slug: orm-session-from-phone-to-server
url: /blog/orm-session-from-phone-to-server/
date: '2026-09-27'
author: Shai Almog
description: "Use managed ORM sessions, relationships, lazy loading, JPQL queries, and optimistic locking with shared Java entities on the client and backend."
feed_html: '<img src="https://www.codenameone.com/blog/orm-session-from-phone-to-server.jpg" alt="Relationships and one unit of database work" /> Use managed ORM sessions, relationships, lazy loading, JPQL queries, and optimistic locking with shared Java entities on the client and backend.'
series: ["release-2026-09-25"]
---

![Relationships and one unit of database work](/blog/orm-session-from-phone-to-server.jpg)

Changing a customer's name should not require remembering which DAO to call after every assignment. Loading twenty orders should not accidentally fetch each customer's entire history. Those are the kinds of problems that appear once an application grows beyond a few independent tables.

[Last week's ORM example](/blog/java-backend-shared-models/) introduced shared entities and generated DAOs. This release adds a managed session: an object that keeps track of the entities involved in one unit of work, detects their changes, and coordinates persistence. Relationships now belong in the model too.

The client and backend use the same mapping annotations. The client stores data in SQLite; the backend supports SQLite, PostgreSQL, MySQL, and MariaDB. This is a JPA-inspired layer, not a JPA provider or a way to run arbitrary Hibernate code unchanged.

## Model a customer and their orders

These are two separate source files in the same package. Existing `Entity` and `Id` annotations stay in `com.codename1.annotations`; the new relationship and version annotations live in `com.codename1.annotations.db`.

```java
// Customer.java
package com.example.model;

import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;
import com.codename1.annotations.db.OneToMany;
import com.codename1.annotations.db.Version;
import java.util.ArrayList;
import java.util.List;

@Entity(table = "customers")
public class Customer {
    @Id public long id;
    @Version public long version;
    public String name;

    @OneToMany(mappedBy = "customer")
    public List<Order> orders = new ArrayList<Order>();
}
```

```java
// Order.java
package com.example.model;

import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;
import com.codename1.annotations.db.JoinColumn;
import com.codename1.annotations.db.ManyToOne;

@Entity(table = "purchase_orders")
public class Order {
    @Id public long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    public Customer customer;

    public long totalCents;
}
```

`Order.customer` owns the foreign key. `Customer.orders` describes the other direction through `mappedBy`. Application code maintains both sides when it changes the in-memory relationship:

```java
order.customer = customer;
customer.orders.add(order);
```

Those two assignments describe one relationship from two viewpoints. They don't persist either object on their own. This mapping doesn't request cascading, so the transaction below explicitly persists both.

## Commit the unit of work

On the client, import `com.codename1.orm.EntityManager`. On the backend, use `com.codename1.backend.orm.EntityManager`. Both expose `openSession()` and the shared `com.codename1.orm.session.Session` interface.

For a development database, call `session.createTables()` before beginning a transaction to create missing mapped tables. Production schema changes belong in your migration process.

The following code takes an entity manager named `entities`, with the schema already created:

```java
import com.codename1.orm.session.Session;

Session session = entities.openSession();
try {
    session.beginTransaction();

    Customer customer = new Customer();
    customer.name = "Alice";
    session.persist(customer);

    Order order = new Order();
    order.customer = customer;
    order.totalCents = 4900;
    customer.orders.add(order);
    session.persist(order);

    session.commitTransaction();
} finally {
    session.close();
}
```

The build generates metadata and enhances entity access. The session uses that metadata to manage the object graph without runtime reflection or proxy generation. Generated IDs are filled in as inserts occur.

Closing a session rolls back an active transaction. It never commits. That makes the `finally` block meaningful when an insert or commit fails. A session belongs to one thread and one unit of work; don't put it in a shared backend controller field. A pooled backend entity manager can be shared while each request opens its own session.

{{< mermaid >}}
flowchart TD
    Open[Open session] --> Begin[Begin transaction]
    Begin --> Load[Find or persist entities]
    Load --> Change[Change fields and relationships]
    Change --> Flush[Flush detects changes and issues SQL]
    Flush --> Commit[Commit transaction]
    Flush -->|failure| Rollback[Roll back active transaction]
    Commit --> Close[Close session]
    Rollback --> Close
{{< /mermaid >}}

The session keeps one object instance per entity identity. Finding the same row twice within that session gives application code one managed object to work with. Updating `customer.name` changes that object; a flush detects the difference and issues the update. Queries inside a transaction flush pending changes too, so a query can trigger database writes before your explicit commit.

## Fetch the screen you intend to show

A relationship is also a loading decision. To-one mappings default to eager loading; to-many mappings default to lazy loading. You can override either default.

This query selects at most twenty orders and explicitly fetches their customers:

```java
List<Order> orders = session.query(Order.class)
        .eq("customer.name", "Alice")
        .orderBy("id", true)
        .limit(20)
        .fetch("customer")
        .list();
```

The query uses Java field paths. The ORM resolves them into the SQL joins and parameters for the database in use. Pagination applies to the root entities before fetched collections are loaded.

A direct to-one fetch with a single-column key can be batched. So can an inverse foreign-key collection fetch in that case. Ordered, map, composite-key, and join-table collections currently use individual loaders. An ORM doesn't make query count irrelevant; inspect the access pattern for the mapping you actually use.

To load a relationship deliberately before leaving the session:

```java
Customer customer = session.find(Customer.class, customerId);
if (customer != null && !session.isLoaded(customer, "orders")) {
    session.initialize(customer, "orders");
}
```

An unloaded relationship read after detachment or session close throws `LazyInitializationException`. Already-loaded data remains readable. Generated `@Mapped` serialization also refuses an unloaded association instead of quietly fetching it during output. Load what the response needs, or map to a smaller response object while the session is open.

On a phone, do this work off the event dispatch thread (EDT). Fetching is synchronous. The UI should receive ready data, not discover a database trip while painting or handling a tap.

## Query in the vocabulary of the model

The supported JPQL subset gives more complex queries a familiar form:

```java
List<Order> orders = session.createQuery(
        "select o from Order o "
        + "where o.customer.name = :name order by o.id",
        Order.class)
        .setParameter("name", "Alice")
        .list();
```

It includes relationship joins, named parameters, scalar projections, aggregates, grouping and `HAVING`, plus supported nested queries and bulk mutations. Unsupported syntax is rejected. It is deliberately described as a subset; a query accepted by another persistence provider still needs to fit this implementation.

Bound parameters carry values into SQL. They don't make a field name supplied by an untrusted caller safe to use as a query structure. Keep the query shape under application control.

Bulk update and delete operations need a transaction. They flush pending changes, bypass per-entity callbacks and cascades, then clear the persistence context so later reads don't reuse stale objects. Use them when you intend those semantics, not as a shorter spelling of a per-entity operation.

## Detect the edit that happened elsewhere

`Customer.version` is an optimistic-lock field. An update checks the previous version, and a conflicting update or deletion produces `OptimisticLockException`.

Imagine two administrators opening the same customer. Both start with version 4. The first saves an edit and advances the version. The second cannot quietly overwrite that newer row with a change based on version 4.

The application still decides what the user should do next: reload, compare changes, or retry an operation that is safe to repeat. After an optimistic-lock or database failure, roll back an active transaction before continuing. `isTransactionActive()` lets you check whether the underlying transaction is still open.

A detached object has another important rule: `session.merge(detached)` returns the managed copy. Keep that return value. Calling `merge` doesn't attach the object you passed in.

## How much of JPA does this bring?

The new layer includes all four relationship kinds, cascades, orphan removal, composite assigned IDs, generated ID strategies, embedded values, converters, lifecycle callbacks, and supported inheritance mappings. That is a substantial step beyond scalar DAOs.

The old scalar DAO API still writes immediately. Models using managed features require a session; asking the legacy DAO to handle them fails rather than silently losing relationship or version behavior. Production schema migration remains a deployment responsibility. Shared entity definitions also do not synchronize a phone database with a server database or resolve offline conflicts for you.

Start with one relationship and one explicit transaction. Check what gets loaded before adding more cascading or fetch behavior. The [ORM guide](/developer-guide/annotation-sqlite-orm/) lists the supported mappings and query restrictions, and [PR #5885](https://github.com/codenameone/CodenameOne/pull/5885) contains the implementation. Tomorrow's {{< post-link path="/blog/follow-a-tap-with-opentelemetry" text="tracing article" >}} explains how to see the backend's database work inside a request trace.

---

## Discussion

_Which ORM behavior has cost you more debugging time: unexpected queries, detached objects, or a write that happened earlier than you expected?_

{{< giscus >}}
