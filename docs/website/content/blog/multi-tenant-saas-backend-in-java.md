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

Here's the decision people will argue with, so let me be precise about it.

The moment your session maps even one entity with a `@TenantId` field, the session stops accepting native SQL. Calling `createNativeQuery` throws `TenantNativeSqlException`, and so does executing one that was created earlier. That holds for every native query in that session, including queries against tables that have no tenant column at all, because the ORM can't tell which tables your SQL touches.

Why not rewrite the SQL and add the tenant condition, the way the ORM does for its own queries? Because a rewrite that's right 99% of the time is a data leak the other 1%. The ORM writes its own queries, so it knows exactly where the condition goes. It didn't write your hand-tuned join, your CTE, your window function or your vendor extension, and guessing there is how a report ends up summing every customer's revenue.

So it refuses, and you opt in for one call, with the tenant condition written by you:

```java {class="cn1-no-try"}
Long open = (Long) TenantContext.allowNativeSql(() -> session
        .createNativeQuery("SELECT COUNT(*) FROM invoice WHERE tenant_id = :t", Long.class)
        .setParameter("t", TenantContext.get()).first());
```

The opt-in is checked twice, when the query is created and again when it runs, so a query built inside the block and kept in a field can't be executed outside it later. Nothing turns the restriction off for longer than that one call.

Two more doors, both explicit. Migrations, cross-tenant reports and provisioning jobs run inside `TenantContext.runUnscoped(...)`, which turns filtering off for that call only. And a statement you send straight to a `DataSource` never passes through the session, so it isn't checked at all. That's the escape hatch for code that really is tenant agnostic, and it's easy to find in a review because it doesn't look like ORM code.

The result: search for `allowNativeSql`, `runUnscoped` and `DataSource` and you have the complete list of code that can read across tenants. Hibernate's `@TenantId` and filters scope your entity queries too, then let native SQL straight through, so finding the one that forgot the tenant is your problem.

### A database per tenant, when the contract says so

Some customers won't share a database, whatever you promise. `TenantDataSources` gives each tenant its own pool from a URL template, keeps the pools in an LRU cache, and migrates every tenant's schema with one call:

```properties
cn1.tenancy.datasource.url=postgres://app:secret@db/app_${tenant}
```

`TenantContext.decorator()` carries the tenant into executor tasks, so background work runs against the right customer.

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

`@BatchMapping` resolves the author of every book in one call, which is the fix for the N+1 problem that sinks most GraphQL servers.

The interesting part is what happens at build time. The build connects every resolver to the schema, and each of these fails the build with the method's name:

- A resolver for a type or field the schema doesn't declare, with the list of fields the type does have.
- An `@Argument` the field doesn't take.
- A primitive parameter for a nullable argument, which has no way to hold the null.
- Two methods resolving the same field.
- A subscription that doesn't return a `GraphQLStream`.

In Spring for GraphQL most of those surface as a warning in a startup log, or as a null in production. Here they never get past your laptop, and nothing is looked up by reflection at run time.

Subscriptions run over WebSocket with the standard `graphql-transport-ws` protocol. A mutation sent over `GET` is refused. Queries nested deeper than `cn1.graphql.maxDepth` (15 by default) are rejected before a resolver runs, which shuts down the classic denial of service through a deeply nested query, and the GraphiQL page is off unless you turn it on. On the client, `cn1:generate-graphql` reads your backend's own schema and generates the typed client, so a field renamed on the server is a compile error in the app.

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

SAML has a long history of implementations that verified a signature and then read a different element than the one that was signed. So a response signs a user in only when all of these hold:

- An XML signature verifies under a certificate you configured. A key carried inside the document is never trusted, and SHA-1 is refused unless you allow it.
- The signature covers the element that is read. A signed assertion moved elsewhere in the document, or an unsigned one placed next to it, is refused.
- The destination, issuer, audience and recipient all name this server and this provider.
- The time is inside the validity window, within three minutes of clock skew.
- The assertion was never used before on this server, so a captured response can't be replayed.

The parser accepts no DTD and no custom entities, which closes XML external entity attacks before they start.

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

## Spring, where Spring got it right

If this reads like Spring, that's on purpose. Spring did a good job of leading the direction of backend Java, and most Java developers already think in its annotations: `@RestController`, `@Transactional`, `@QueryMapping`, `saml2Login`. We follow it wherever we can, so your knowledge carries over and so do your agent's.

We take a different route in two places.

**We fail fast.** Failing fast is core to Codename One, on the client and on the server. A dependency nobody can satisfy, a GraphQL resolver for a field that doesn't exist, a malformed cron expression or a constructor cycle fails the build, naming the line, instead of failing the first request in production. A configuration key nothing sets stops the server at startup with the key's name. A query with no tenant throws instead of answering every tenant. I believe that's simply better for reliability, and more so in today's serverless and Kubernetes deployments, where a container that starts and then misbehaves is worse than one that refuses to start at all.

**We build for performance and AOT.** The backend compiles to a native executable through the same ParparVM pipeline as our iOS port. There's no JVM underneath, no classpath scan, no reflection and no runtime proxies: the build writes the wiring out as plain `new` calls, and `@Transactional` rewrites the method itself, so it also applies to a call through `this`, where a Spring proxy silently skips it. Spring can't rewrite its core concepts for ahead-of-time compilation without breaking a decade of applications built on them. We started without that baggage, and it's an opportunity they don't have.

Next up on Monday: {{< post-link path="/blog/generate-and-view-pdfs-in-java" text="generating, viewing and printing PDFs" >}}, on the phone and on this same server.
