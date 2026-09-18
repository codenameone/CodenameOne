---
title: "One Java Model from the App to PostgreSQL"
slug: java-backend-shared-models
url: /blog/java-backend-shared-models/
date: '2026-09-19'
author: Shai Almog
description: "Build a small native Java service with Codename One's backend, generated controllers, shared validation, and an ORM that works with SQLite, PostgreSQL, and MySQL."
feed_html: '<img src="https://www.codenameone.com/blog/java-backend-shared-models.jpg" alt="One model from phone to database" /> Build a native Java backend with shared models, generated routes, and portable database access.'
series: ["release-2026-09-18"]
---

![One model from phone to database](/blog/java-backend-shared-models.jpg)

The annoying part of full stack development is finding the same rule in three places. The app checks a field, the server checks it slightly differently, and the database model has another idea about what it should contain.

[Yesterday's post](/blog/why-another-java-server/) explained why we brought Codename One's runtime to the backend. Today we'll build a reminder service and follow one model from Java code into the database. The backend is experimental, but the pieces here already include generated routing, pooled connections, an ORM, and transactions.

## Start with the generated backend module

Use a current project from the [Codename One Initializr](/initializr/). Its `backend` module has the server dependency and annotation-processing execution already configured. An older project needs that module added; the [current backend POM template](https://github.com/codenameone/CodenameOne/blob/2697dcfa2f0425170efd08655243032571b65869/maven/cn1app-archetype/src/main/resources/archetype-resources/backend/pom.xml) shows the complete wiring.

Run commands from the project root. The backend is activated with `-Dcodename1.platform=backend`, so it doesn't enter an ordinary client build.

The local loop runs on Java. Native packaging also needs a working JDK 8, selected through `JDK_8_HOME`, and a host C compiler with the development libraries needed by the enabled native features. Linux cross-packaging uses Docker or Podman. The [packaging implementation](https://github.com/codenameone/CodenameOne/blob/2697dcfa2f0425170efd08655243032571b65869/maven/codenameone-maven-plugin/src/main/java/com/codename1/maven/BackendPackageMojo.java) checks those dependencies and names what's missing.

## Give the model one rule

Create these two files under `backend/src/main/java/com/example/reminders/`. We'll move them into a shared module once the service works.

`Reminder.java`:

```java
package com.example.reminders;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

@Entity(table = "reminders")
public class Reminder {
    @Id public long id;
    @Column(nullable = false) public String title;
    public boolean done;
}
```

`ReminderRules.java`:

```java
package com.example.reminders;

public final class ReminderRules {
    private ReminderRules() {
    }

    public static String title(String value) {
        String title = value == null ? "" : value.trim();
        if (title.length() == 0 || title.length() > 120) {
            throw new IllegalArgumentException(
                    "Use a title between 1 and 120 characters.");
        }
        return title;
    }
}
```

There are no UI imports in either class. The client can call `ReminderRules.title()` before submitting a form. The server must call it too: a caller can bypass every client-side check.

The annotations are the same `com.codename1.annotations` types used by the client ORM. The backend build generates metadata and field access for its own DAO. It doesn't discover entity fields through reflection at startup.

## Put a controller over the DAO

Create `ReminderController.java` in the same directory:

```java
package com.example.reminders;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.*;
import com.codename1.backend.orm.Dao;
import com.codename1.backend.orm.EntityManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reminders")
public class ReminderController {
    private final Dao<Reminder> reminders;

    public ReminderController(EntityManager entities) {
        reminders = entities.dao(Reminder.class);
    }

    @GetMapping
    public List<Map> list() throws IOException {
        List<Reminder> rows = reminders.query()
                .eq("done", Boolean.FALSE)
                .orderBy("id", true)
                .limit(20)
                .list();
        List<Map> result = new ArrayList<Map>();
        for (Reminder reminder : rows) {
            result.add(view(reminder));
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map read(@PathVariable("id") long id)
            throws IOException {
        return view(reminders.findById(Long.valueOf(id)));
    }

    @PostMapping
    public HttpServer.Response create(@RequestBody Map body)
            throws IOException {
        Object value = body.get("title");
        String title;
        try {
            title = ReminderRules.title(
                    value instanceof String ? (String) value : null);
        } catch (IllegalArgumentException invalid) {
            return new HttpServer.Response(400, "text/plain; charset=utf-8",
                    invalid.getMessage().getBytes("UTF-8"));
        }
        Reminder reminder = new Reminder();
        reminder.title = title;
        reminders.insert(reminder);
        return HttpServer.Response.jsonValue(201, view(reminder));
    }

    private static Map view(Reminder reminder) {
        if (reminder == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("id", Long.valueOf(reminder.id));
        result.put("title", reminder.title);
        result.put("done", Boolean.valueOf(reminder.done));
        return result;
    }
}
```

The generated entry point opens an entity manager and passes it to the constructor. `insert()` assigns the generated key to `reminder.id`. The map returned by `view()` becomes JSON; returning `null` from `read()` produces a 404. Invalid titles receive a 400 response. This explicit response mapping keeps the example independent of reflection; the shared-contract path below can generate DTO codecs instead. The request creates a fresh entity, so a caller cannot assign its own `id` or mark a reminder complete by sneaking extra fields into the body.

The controller is a local tutorial service with no user accounts. Before exposing a multi-user version, authenticate requests and scope every read and write to the authenticated owner. A shared model doesn't make a client trustworthy.

## Run against a local database

Start the development profile:

```bash
CN1_PROFILE=dev mvn -pl backend \
    -Dcodename1.platform=backend cn1:backend
```

The development profile defaults to in-memory SQLite and creates missing ORM tables. In another terminal:

```bash
curl -i http://localhost:8080/reminders \
    -H 'Content-Type: application/json' \
    --data '{"title":"Renew the certificate"}'

curl http://localhost:8080/reminders
curl http://localhost:8080/reminders/1
```

The first request returns 201 and a reminder with an assigned ID. Restarting an in-memory service discards its rows. To keep them locally, create an optional `application-dev.properties` in the backend module's working directory:

```properties
cn1.datasource.url=reminders.db
```

For a packaged binary, place that file in the directory you run it from. Configuration also accepts system properties and environment variables; `CN1_DATASOURCE_URL` overrides the file value.

## Change databases through configuration

The later [data source and ORM work](https://github.com/codenameone/CodenameOne/pull/5827) is what makes this a useful server stack. The same DAO can use SQLite, PostgreSQL, MySQL, or MariaDB. Native PostgreSQL and MySQL clients speak their wire protocols directly; the native SQLite engine is linked into the executable. The JVM development path uses its corresponding Java implementations, including the SQLite driver supplied by the generated project.

{{< mermaid >}}
flowchart TD
    Entity[Reminder.java] --> ClientGen[Client build generates SQLite DAO]
    Entity --> ServerGen[Backend build generates entity metadata]
    ServerGen --> DAO[Backend DAO]
    DAO --> Pool[Configured data source and connection pool]
    Pool --> SQLite[(SQLite)]
    Pool --> Postgres[(PostgreSQL)]
    Pool --> MySQL[(MySQL / MariaDB)]
{{< /mermaid >}}

Supply `DATABASE_URL` through your deployment's environment, and run the binary without the development profile. The URL selects the database engine. The data source pools server connections and replaces dead connections instead of returning them to another request.

Production requires deliberate database configuration. It doesn't quietly start with an empty in-memory database, and it doesn't create tables by default. Apply your schema before starting the service. `createTable()` can create a missing table during development; it doesn't migrate an existing one.

When you need SQL directly, parameters keep the same spelling:

```java
db.execute("UPDATE reminders SET done = ? WHERE id = ?",
        new Object[] {Integer.valueOf(1), Long.valueOf(7)});
```

Here `db` is a backend `DataSource`. It converts portable `?` parameters to the selected engine's syntax. Values stay bound parameters, and the parameter count is checked before execution. SQL identifiers and operators belong in application-controlled SQL; don't concatenate user input into them.

## Keep a transaction on one connection

For operations that must succeed together, use the entity manager supplied to the transaction body:

```java
entities.transaction(new EntityManager.Work() {
    public Object run(EntityManager tx) throws Exception {
        Dao<Reminder> reminders = tx.dao(Reminder.class);
        Reminder current = reminders.findById(Long.valueOf(7));
        if (current == null) {
            throw new IllegalArgumentException("Reminder not found.");
        }
        current.done = true;
        reminders.update(current);

        Reminder next = new Reminder();
        next.title = ReminderRules.title("Check the new certificate");
        reminders.insert(next);
        return null;
    }
});
```

Both DAOs and SQL reached through `tx` use the transaction's connection. An exception rolls it back. Reaching outside that object for an unrelated data source would defeat the point.

The ORM keeps relationships explicit. Store another entity's ID as a scalar field and query it deliberately. It doesn't add a lazy-loading graph that might turn one loop into dozens of unexpected round trips.

## Share the source with the app

Once the service works, move `Reminder` and `ReminderRules` into a Java model module that both `common` and `backend` depend on. Add it to the parent reactor and add that module's Maven dependency to both consumers. Keep UI code out of it. The backend annotation processor also reads entities from compile dependencies, so a class doesn't have to live in the backend's own source directory to receive a DAO.

The client and server generate different DAO classes from that one entity definition. They can coexist on the build classpath. Sharing the definition doesn't automatically synchronize rows or resolve offline edits; your API decides which changes to send and how to handle conflicts.

A shared REST contract extends the same idea to the wire. This is the shape of a client contract using `com.codename1.annotations.rest`, `OnComplete`, and `Response`:

```java
@RestClient
public interface ReminderApi {
    @GET("/reminders/{id}")
    void read(@Path("id") String id,
              OnComplete<Response<Reminder>> callback);
}
```

The client build generates its asynchronous implementation. Enabling `-Dcn1.restServer=true` for server generation emits a synchronous `ReminderApiServer` interface and a `ReminderApiDispatcher`. Your server implements the former and connects the latter to its HTTP handler. The [shared-contract demo](https://github.com/codenameone/CodenameOne/tree/2697dcfa2f0425170efd08655243032571b65869/vm/backend/demo) includes that transport wiring.

This contract path is an alternative to declaring the controller routes above. Choose it when you want the API definition itself shared with the app. It lets the build catch signature drift before a user gets a response it cannot parse.

## Package the server

For a native binary on your development machine:

```bash
mvn -pl backend -Dcodename1.platform=backend cn1:backend-package
```

For a static Linux ARM64 executable, with Docker or Podman available:

```bash
mvn -pl backend -Dcodename1.platform=backend \
    -Dcn1.backend.target=musl-arm64 cn1:backend-package
```

The default output is `backend/target/<artifactId>`, using the backend module's artifact ID. Copy that executable to the matching deployment platform and supply its configuration there. The generated lifecycle drains active requests before closing the database on shutdown.

The native HTTP server uses virtual threads where supported. That is particularly useful for waiting connections: the request can suspend without dedicating an OS thread to it. The JVM development loop shares the HTTP implementation, while native TLS and HTTP/2 need the packaged runtime for testing.

## A small service with room to grow

We now have a controller, a persistent model, shared validation, and transactions without adding a second application language. The runtime can package that service for the same constrained deployments that made us look at Go.

The rest of this week's work addresses encryption, installation, and platform configuration. {{< post-link path="/blog/vault-encryption-browser-phone" text="Vaults" >}} keep synchronized records encrypted. {{< post-link path="/blog/invite-link-through-app-store" text="Invitations" >}} preserve attribution through installation. Our builders take responsibility for more platform configuration, while self-hosting gives the runtime another demanding application to run.

For secure-by-default development, the useful part is concrete: bound SQL values, explicit transactions, validation that runs on the server, and startup that refuses missing dependencies. That leaves the application's authorization rules visible in application code.

---

## Discussion

_Which part of your app and server drifts most often: models, validation, API contracts, or database behavior?_

{{< giscus >}}
