---
title: "Build a Multi-Tenant SaaS Backend That Can't Leak One Customer Into Another"
slug: multi-tenant-saas-backend-in-java
url: /blog/multi-tenant-saas-backend-in-java/
date: '2026-11-22'
author: Shai Almog
description: "The Codename One backend now filters every ORM query by tenant and refuses native SQL in a multi-tenant session unless you opt in. It also gained a GraphQL server, SAML single sign-on with LDAP and Active Directory, object storage on S3, R2, GCS and Azure, and MongoDB, DynamoDB and Elasticsearch through the same entity classes."
feed_html: "<img src=\"https://www.codenameone.com/blog/multi-tenant-saas-backend-in-java.jpg\" alt=\"A multi-tenant SaaS backend in Java\" /> Tenant filtering on every query, native SQL blocked by default, GraphQL, SAML and LDAP sign-in, object storage on four clouds, and MongoDB, DynamoDB and Elasticsearch through the ORM."
series: ["release-2026-11-20"]
---

![A multi-tenant SaaS backend in Java](/blog/multi-tenant-saas-backend-in-java.jpg)

The worst bug a SaaS company can ship is one customer seeing another customer's invoices. It doesn't crash anything and no test fails. A support ticket arrives, then a lawyer. It almost always comes from the same place: one query, somewhere, that forgot the `WHERE tenant_id = ?`.

This week the Codename One backend refuses to run that query. It also picked up the rest of what an enterprise customer asks about in the first meeting: GraphQL, SAML single sign-on, LDAP and Active Directory, object storage on every major cloud, and document and search databases. Friday's {{< post-link path="/blog/why-my-kids-are-learning-to-program" text="post" >}} argued that the baseline for an app moved. This is the baseline for its server.

## One annotation, and every query is scoped

Mark the field that holds the tenant:

```java {class="cn1-no-try"}
@Entity
public class Invoice {
    @Id @GeneratedValue long id;
    @TenantId String tenant;
    String customer;
}
```

From then on the ORM stamps the tenant on insert and treats it as immutable. Finds, criteria queries, JPQL, counts, bulk updates and bulk deletes are all filtered to the current tenant. Another tenant's row doesn't throw an access error. It reads as missing, which is exactly what you want an attacker probing ids to see. Saving an entity that belongs to a different tenant throws `TenantMismatchException`, and running a query with no tenant set throws `MissingTenantException` instead of quietly returning everything.

The tenant comes from the request. A `TenantFilter` sets `TenantContext` for each request, with resolvers for the common layouts:

```java {class="cn1-no-try"}
new TenantFilter(TenantFilter.subdomain("example.com"), true); // acme.example.com -> "acme"
new TenantFilter(TenantFilter.header("X-Tenant-ID"), true);
new TenantFilter(TenantFilter.authenticated(auth -> accounts.tenantOf(auth.getName())), true);
```

Use the last one in production. A header or a host name is whatever the client says it is. The authenticated user's tenant is whatever your identity provider says it is.

{{< mermaid >}}
flowchart LR
    R[Request<br/>acme.example.com] --> F[TenantFilter]
    F -->|TenantContext = acme| S[ORM session]
    S -->|find, query, count, update| Q[SQL with tenant condition added]
    S -.->|native SQL| X[TenantNativeSqlException]
    Q --> DB[(Shared database)]
{{< /mermaid >}}

### Native SQL is blocked, on purpose

Here's the decision people will argue with, so I'll spell out exactly what it does.

The ORM rewrites the queries it generates. It adds the tenant condition to every find, criteria query, JPQL statement, count, bulk update and bulk delete, because it builds that SQL itself and knows where every table appears. It doesn't rewrite native SQL. A string you wrote by hand goes to the database as written, and a rewrite that's right 99% of the time leaks data the other 1%. The ORM can't reliably understand your hand-written join, your CTE, your nested select or your vendor-specific function.

So the rule is blunt. If a session maps **any** entity with a `@TenantId` field, `createNativeQuery` on that session throws `TenantNativeSqlException`:

- It doesn't matter what the statement touches. A native query that only reads a lookup table with no tenant column is refused too, because the session can't tell the difference without parsing your SQL.
- It covers every kind of native statement: `SELECT`, `UPDATE`, `DELETE` and DDL.
- It throws when you **create** the query, not when you run it, so the stack trace points at the line that built the statement. A native `SELECT` that quietly returned every tenant's rows would point at nothing.
- A session that maps no tenant entities isn't affected. Neither is a database-per-tenant setup that doesn't use `@TenantId`, because in that setup the connection itself is the boundary.

You have two ways out, and both are scoped to one call. When the statement is tenant data, opt in and add the condition yourself:

```java {class="cn1-no-try"}
Long open = (Long) TenantContext.allowNativeSql(() -> session
        .createNativeQuery("SELECT COUNT(*) FROM invoice WHERE tenant_id = :t", Long.class)
        .setParameter("t", TenantContext.get()).first());
```

When the work really spans every tenant, as with a migration, a cross-tenant report or the job that provisions a new customer, switch filtering off for that one call:

```java {class="cn1-no-try"}
TenantContext.runUnscoped(() -> session
        .createNativeQuery("UPDATE invoice SET currency = 'EUR' WHERE currency IS NULL", Object.class)
        .executeUpdate());
```

No switch turns filtering off for longer than one call, and no global setting does either. Those two blocks are the only places a reviewer has to read carefully. Search for `allowNativeSql` and `runUnscoped` and you have the complete list of statements that can leak. Hibernate's `@TenantId` scopes your entity queries too, then lets native SQL straight through, so finding the one that forgot the tenant is your problem.

### The details that make it hold

Filtering is the easy part. These are the parts that usually break:

- **Thread reuse.** A request thread serves the next request after this one. `TenantFilter` clears the tenant when the request ends, and `TenantContext.runAs(tenant, work)` restores whatever was current before, so one customer's tenant never carries into another customer's request.
- **Background work.** A task submitted to an executor runs on another thread with no tenant. `TenantContext.decorator()` carries the submitting thread's tenant into the task. A task submitted from inside `runUnscoped` doesn't inherit that. Each piece of work turns filtering off for itself.
- **Mixed sessions.** A session that has loaded rows for `acme` refuses to serve `globex` afterward and throws `TenantMismatchException`. A cached entity can't cross over through the session's identity map.
- **Hostile identifiers.** A tenant id is 1 to 64 characters of `[A-Za-z0-9_.-]`, checked when it's set. It ends up in SQL and in database and schema names, so `acme'; DROP` is rejected before anything sees it.

### A database or a schema per tenant, when the contract says so

Some customers won't share a database, whatever you promise. `TenantDataSources` is the counterpart of Spring's `AbstractRoutingDataSource`. It gives each tenant its own pool from a URL template, opens it on first use, keeps the open pools in an LRU cache, and migrates every tenant's schema with one call:

```properties
# A database per tenant
cn1.tenancy.datasource.url=postgres://app:secret@db/app_${tenant}
```

A schema per tenant is the same pool with an init statement, `SET search_path TO tenant_${tenant}` on PostgreSQL or `USE tenant_${tenant}` on MySQL. The template must put a prefix in front of `${tenant}`. A bare `${tenant}` would let a tenant named `postgres` or `public` open a database you never meant to hand out, so it fails at startup.

You can combine the two. Keep the big customers on their own databases and everyone else on a shared table with `@TenantId`, from the same code.

## GraphQL, in the Spring style you already know

Drop a schema into `src/main/resources/graphql` and write a controller:

```graphql
type Query { book(id: ID!): Book  books: [Book!]! }
type Mutation { addBook(input: BookInput!): Book! }
type Book { id: ID!  title: String!  author: Author }
```

```java {class="cn1-no-try"}
@Controller
public class BookController {
    @QueryMapping public List<Book> books() { return repository.findAll(); }
    @MutationMapping public Book addBook(@Argument("input") BookInput input) { return repository.save(input.toBook()); }
    @BatchMapping public Map<Book, Author> author(List<Book> books) { return authors.forBooks(books); }
}
```

`@BatchMapping` resolves the author of every book in one call, which is the fix for the N+1 problem that sinks most GraphQL servers. Subscriptions return a `GraphQLStream` over WebSocket with the standard `graphql-transport-ws` protocol. Queries deeper than `cn1.graphql.maxDepth` (15 by default) are rejected before they run, and GraphiQL and introspection are off unless you turn them on. On the client, `cn1:generate-graphql` reads your backend's own schema and generates the typed client, so a renamed field is a compile error in the app.

## SAML, LDAP and Active Directory

Enterprise buyers don't ask whether you support single sign-on. They ask which identity provider you've tested with. The backend is a SAML 2.0 service provider with single logout, and works with Okta, Entra ID, ADFS, Ping, Google Workspace, Keycloak and Shibboleth:

```properties
cn1.security.saml2.relyingparty.registration.okta.assertingparty.metadata-uri=https://corp.okta.com/app/abc123/sso/saml/metadata
cn1.security.saml2.relyingparty.registration.okta.entity-id=https://app.example.com/saml2/service-provider-metadata/okta
cn1.security.saml2.cookie-secret=${SAML_COOKIE_SECRET}
```

```java {class="cn1-no-try"}
http.saml2Login(Customizer.withDefaults())
    .saml2Logout(Customizer.withDefaults());
```

For the companies still running a directory on premises, `LdapAuthenticationProvider` binds against LDAP and `ActiveDirectoryLdapAuthenticationProvider` against AD, reporting locked, expired and disabled accounts as distinct errors so your login screen can say something useful:

```java {class="cn1-no-try"}
BindAuthenticator bind = new BindAuthenticator(directory);
bind.setUserDnPatterns("uid={0},ou=people");
http.authenticationProvider(new LdapAuthenticationProvider(bind,
        new DefaultLdapAuthoritiesPopulator(directory, "ou=groups")))
    .formLogin(Customizer.withDefaults());
```

If you've written Spring Security, you've already read this code. That's deliberate.

## Object storage on any cloud, with no SDK

`ObjectStorage` is one interface, and a URL picks the provider:

| `cn1.storage.url` | Provider |
| --- | --- |
| `s3://us-east-1` | Amazon S3, plus MinIO, Ceph, Backblaze B2 and Wasabi with `cn1.storage.s3.endpoint` |
| `r2://<account id>` | Cloudflare R2, with an EU jurisdiction option |
| `gs://<project>` | Google Cloud Storage |
| `azblob://<account>` | Azure Blob Storage |
| `memory:` | In memory, the default for tests |

There's no cloud SDK in your server. The build links only the provider you named, so switching from S3 to R2 to cut egress costs is a property change, not a dependency migration. Uploads don't pass through your server at all:

```java {class="cn1-no-try"}
@Component
public class Avatars {
    private final ObjectStorage storage;
    public Avatars(ObjectStorage storage) { this.storage = storage; }

    public String uploadUrl(long userId) throws IOException {
        return storage.presignPut("avatars", userId + ".png", 600);
    }
}
```

The app gets a signed URL, does a plain HTTP `PUT` straight to the bucket, and your server never pays for the bandwidth.

## MongoDB, DynamoDB and search, with the same entities

The entity classes you already have work against document databases too. Point `cn1.document.url` at `mongodb://...` or `dynamodb://us-east-1` and query with the ORM's own API:

```java {class="cn1-no-try"}
DocumentSession session = DocumentSession.current();
session.persist(order);
List<Order> open = session.query(Order.class)
        .eq("status", "open").orderBy("created", false).limit(20).list();
```

`@SortKey` marks the second half of a DynamoDB composite key. Full-text search is two annotations:

```java {class="cn1-no-try"}
@Entity @Searchable(index = "products")
public class Product {
    @Id public long id;
    @SearchField public String name;
    String supplierCode;
}
```

Rows are indexed after the transaction commits, so search never shows a product the database rolled back. `cn1.search.url` picks Elasticsearch, OpenSearch, Amazon OpenSearch Service or OpenSearch Serverless. The default, `database:`, uses PostgreSQL text search or MySQL `FULLTEXT`, so you get search on day one and move to a cluster when the traffic says so, without touching a line of code.

## Why it looks like Spring, and where it doesn't

If most of this reads like Spring, that's on purpose. Spring did a good job of leading the direction of backend Java, and its names are what your team already knows: `@TenantId`, `AbstractRoutingDataSource`, `saml2Login`, `@QueryMapping`. We follow it unless we have a reason not to.

We take a different route in two places.

**We fail fast.** That's core to Codename One, and I think it's better for reliability, especially in today's serverless and Kubernetes environments. A misconfigured instance should die at startup or at the line that's wrong, not limp along serving traffic until a customer notices. That's why a query without a tenant throws instead of returning every row, and why native SQL is refused instead of trusted.

**We build for performance and AOT.** The backend runs on the JVM and on our native runtime, compiled ahead of time. Spring can't rewrite its core concepts for AOT, because too much of the ecosystem depends on runtime reflection and proxies. We started with that constraint, and we have an opportunity they don't. That's why the object storage build links only the provider you named, and why none of this needs reflection at runtime.

Next up on Monday: {{< post-link path="/blog/generate-and-view-pdfs-in-java" text="generating, viewing and printing PDFs" >}}, on the phone and on this same server.
