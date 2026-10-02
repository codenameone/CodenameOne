# The Backend Module — Spring-Style Server Code

The `backend/` module is the server side of the app, written in Java with Spring's
annotations under Codename One package names. `@RestController`, `@Component`,
`@Autowired`, `@Transactional`, `@Scheduled` and the rest mean what they mean in
Spring. The difference is **when** they are resolved: at build time, into plain
code. There is no container, no classpath scan, no proxy and no reflection when the
server runs, and a dependency that cannot be satisfied fails the **build** with a
message naming the injection point.

The same source runs two ways:

```bash
# Development: this JVM, starts in seconds, dev profile = in-memory SQLite + dev tools
CN1_PROFILE=dev mvn -pl backend -Dcodename1.platform=backend cn1:backend

# Deployment: one native binary, no JVM underneath
mvn -pl backend -Dcodename1.platform=backend cn1:backend-package
```

`-Dcodename1.platform=backend` is required: the module sits in a Maven profile.

## Rules that differ from a normal Java server

- **Java 8 language level, backend class library only.** The native build compiles
  `backend/` against the server runtime's Java API subset, not the JDK. No
  `java.time`, no reflection, no `Class.forName`. `backend/` does **not** depend on
  `codenameone-core` (no UI classes on a server).
- **Everything injectable is known at build time.** Annotate the class, or declare a
  `@Bean` method. There is no scanning of jars.
- **Configuration files live in the module ROOT**: `backend/application.properties`
  and `backend/application-<profile>.properties`, **not** `src/main/resources`.
  Environment variables override them (`cn1.datasource.url` is also read from
  `CN1_DATASOURCE_URL` and `DATABASE_URL`, `cn1.server.port` from `PORT`).
- **`application.properties` is compiled into the server** as the bottom layer of
  its configuration, so a single binary runs with no file beside it; a file or the
  environment present at run time still overrides every key. Profile files are not
  compiled in. Never put a token or password in it -- write `${NAME}` and set the
  variable.
- **Imports come from `com.codename1.backend.annotations`**, never
  `org.springframework.*`.

## Beans and injection

```java
package com.example.myapp;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.*;

@Component
public class NoteStore {
    private final DataSource db;                    // built-in: the connection pool
    public NoteStore(DataSource db) { this.db = db; }
    // ...
}

@Component
public class Notes {
    private final NoteStore store;
    @Autowired private Mailer mailer;               // private fields are fine
    @Value("${notes.max:100}") private int max;     // config, with a fallback

    public Notes(NoteStore store) { this.store = store; }   // one constructor: no @Autowired needed

    @PostConstruct void ready() { /* runs once, after injection */ }
    @PreDestroy void shutdown() { /* runs when the server stops */ }
}

@RestController
@RequestMapping("/api")
public class NotesApi {
    private final Notes notes;
    public NotesApi(Notes notes) { this.notes = notes; }

    @GetMapping("/notes/{id}")
    public Note get(@PathVariable("id") long id) { ... }
}
```

| Annotation | Meaning |
| --- | --- |
| `@Component` | A bean. Name defaults to the simple class name, decapitalized. |
| `@Configuration` + `@Bean` | Factory methods; parameters are injected. Calling one `@Bean` method from another does NOT share the instance — take the other bean as a parameter. |
| `@Autowired` | Constructor (needed only when there are several), field, or setter. `required = false` allowed. |
| `@Qualifier("name")`, `@Primary` | Choose between several beans of one type. Two candidates and neither is enough → build error. |
| `@Value("${key:fallback}")` | A configuration value, converted to String, a primitive or box, or an enum. |
| `@ConfigurationProperties("mail")` | Calls the bean's setters from `mail.*` keys (`setMaxSize` reads `mail.maxSize` or `mail.max-size`). |
| `@Scope("prototype")` | A new instance at each injection point. |
| `@Scope("request")`, `@Scope("session")` | One per HTTP request / session. Injected into a singleton through a generated subclass, so the class must not be final and needs a no-argument constructor. |
| `@Lazy` | Built on first use (same subclass rule). The stand-in runs the no-argument constructor once at start-up, so put expensive set-up in `@PostConstruct`, which runs only on the real instance. |
| `@Profile("dev")`, `@Profile("!prod")` | Only on that profile (`CN1_PROFILE`). |
| `@ConditionalOnProperty("feature.x")`, `@ConditionalOnMissingBean` | Conditional beans. |

Built-in injectables: `Config`, `DataSource`, `orm.EntityManager`,
`com.codename1.orm.session.Session` (the current transaction's session), and — in a
request bean only — `HttpServer.Request` and `HttpSession` (a session bean reads the
current session with `Backend.currentRequest().getSession(true)` instead).

**Parameter names do not survive compilation.** `@PathVariable`, `@RequestParam`,
`@RequestHeader` and `@McpParam` always need their name spelled out.

## JSON: return and accept your own classes

A controller may return an entity or DTO (or `List<Order>`, `Map<String, Order>`)
and take one as `@RequestBody`. The build writes a codec per class -- no
reflection -- in the same JSON form as the app's `@Mapped` mapper, so a class
shared by app and backend round-trips:

- Fields: non-static, non-`transient`; public directly, otherwise via
  `getX`/`isX` + `setX`. `@JsonProperty("name")` / `@JsonIgnore` from
  `com.codename1.annotations`.
- `Date` = epoch millis (reads millis or ISO-8601), `byte[]` = base64, enum = name.
- Unknown body members ignored, absent ones keep the default. A bad value is a 400
  naming its path (`$.lines[0].quantity: expected a whole number ...`).
- Objects that point back at each other: `@JsonIgnore` the back reference (a
  response nesting over 64 deep is a 500).
- Build errors: generic `Page<T>` fields (use a concrete subclass), a body class
  without a no-arg constructor, interfaces, arrays other than `byte[]`.

## Transactions

```java
@Component
public class Transfers {
    private final DataSource db;
    public Transfers(DataSource db) { this.db = db; }

    @Transactional
    public void move(long from, long to, long cents) throws IOException {
        db.execute("UPDATE account SET cents = cents - ? WHERE id = ?", new Object[] {cents, from});
        db.execute("UPDATE account SET cents = cents + ? WHERE id = ?", new Object[] {cents, to});
    }
}
```

- Everything done through the `DataSource` on the calling thread — directly, via an
  entity manager's DAOs, or via the injected `Session` — joins the transaction.
- Unchecked exceptions and `Error`s roll back; checked exceptions commit -- except
  `DataAccessException`, what every failed database operation throws (a subclass
  of `IOException`), which rolls back as Spring's unchecked one does. Another
  `IOException` (file, mail, HTTP) commits unless listed in `rollbackFor`.
  `rollbackFor` / `noRollbackFor` change that.
- Propagation: `REQUIRED` (default), `REQUIRES_NEW`, `NESTED` (savepoint),
  `SUPPORTS`, `MANDATORY`, `NOT_SUPPORTED`, `NEVER`. `readOnly = true` for readers.
- **The method itself is rewritten at build time**, so unlike Spring the
  transaction also applies to a call through `this`, to a private method, and to an
  object you built with `new`.
- `Transactions.setRollbackOnly()` in the method that began the transaction rolls it back without throwing; in a joined method it makes the outer commit throw `UnexpectedRollback`.
- The injected `Session` only works inside a `@Transactional` method.
- Use `?` placeholders; the same SQL runs on SQLite, PostgreSQL and MySQL.

## Background work: `@Async`, `@Scheduled`, threads

```java
@Component
public class Reports {
    @Async                                          // caller returns at once
    public Future<Report> build(String month) {
        return AsyncResult.of(compute(month));      // void is fine too
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Berlin")   // sec min hour dom mon dow
    public void nightly() { ... }

    @Scheduled(fixedRate = 60000, lock = "cleanup")  // one replica at a time, via the DB
    public void cleanup() { ... }
}
```

- Cron is Spring's **six** fields (seconds first) or `@daily`, `@hourly`, ... A
  literal expression is parsed at build time — a typo is a build error.
- A run never overlaps the previous run of the same job; a late fire is skipped.
- `@Async(thread = ThreadKind.VIRTUAL)` runs on a virtual thread; `PLATFORM` (the
  default) on a pool. A virtual thread parks on sockets, so PostgreSQL/MySQL
  queries, `Web` calls and TLS are fine on `VIRTUAL`. **Rule of thumb: SQLite and
  file work belong on `PLATFORM`** — those are local calls that block the host
  thread under a virtual thread. `Future.get()` on a virtual thread yields its host
  instead of blocking it.
- Size executors in config: `cn1.task.executor.<name>.threads`,
  `cn1.task.executor.<name>.kind=virtual|platform`; name one with
  `@Async("reports")`. One-off work: `Tasks.platform(runnable)`,
  `Tasks.virtual(runnable)`.

## Sessions

```java
@PostMapping("/login")
public String login(HttpServer.Request request, @RequestBody Map body) {
    HttpSession session = request.getSession(true);
    session.changeSessionId();              // always, on login
    session.setAttribute("user", userId);
    return "ok";
}
```

A session and its cookie exist only once something calls `getSession(true)`.
`cn1.session.store=db` keeps sessions in the database so any replica can serve
any client (attributes must then be JSON-able). Cookie is `HttpOnly`,
`SameSite=Lax`, and `Secure` under TLS.

## Metrics and management (JMX style, over OpenTelemetry)

```java
@Component
@ManagedResource(objectName = "cache")
public class Cache {
    @ManagedAttribute(unit = "{entry}") public int getSize() { ... }  // a gauge
    @ManagedOperation public void clear() { ... }                   // invocable
    @Timed @Counted public Value load(String key) { ... }            // histogram + counters
}
```

Custom instruments: `Metrics.counter(...)`, `Metrics.histogram(...)`,
`Metrics.gauge(...)` — create once, keep in a static field.

- `@OpenTelemetry(serviceName = "notes")` on any class (or
  `cn1.otel.enabled=true`) exports traces **and** metrics over OTLP/HTTP; point
  `OTEL_EXPORTER_OTLP_ENDPOINT` at a collector.
- Management endpoints: `/manage/health`, `/manage/metrics`, `/manage/prometheus`,
  `/manage/jobs`, `/manage/managed`, and `POST /manage/managed/{bean}/{operation}`.
  Always present in the `cn1:backend` dev run. A **packaged** binary contains them
  only if the build asked -- `cn1.management.enabled=true` in a properties
  file -- otherwise the code is not
  in the binary at all. Outside dev they also need `cn1.management.token` (env).
  `cn1.management.enabled=false` turns built-in endpoints off at start-up.

## MCP: the running backend as a tool server

On the dev profile, `cn1:backend` serves MCP at `http://127.0.0.1:8080/mcp` and
prints the URL at start-up. Register it once:

```bash
claude mcp add --transport http cn1-backend http://127.0.0.1:8080/mcp
```

(A stdio-only host can use the bridge in the backend jar:
`java -cp <codenameone-backend.jar> com.codename1.backend.mcp.StdioBridge http://127.0.0.1:8080/mcp`.)

| Tool | Use it to |
| --- | --- |
| `backend_routes` | See every route and the method behind it |
| `backend_beans` | Check what was injected where, and which conditional beans are active |
| `backend_call` | Send a request (`method`, `path`, `body`, `headers`) and read the response |
| `backend_requests` | See recent requests; `failuresOnly: true` shows 5xx answers with the exception |
| `backend_logs` | Read the server's console |
| `backend_sql` | Query the database (`write: true` to modify it) |
| `backend_schema` | See the entities, tables and columns |
| `backend_jobs`, `backend_run_job` | Inspect scheduled jobs; run one now |
| `backend_metrics`, `backend_managed`, `backend_invoke` | Read metrics; read/invoke managed beans |
| `backend_config` | The active profile and settings (secrets masked) |

The application can publish its own tools, which is how an agent in production
talks to the app's domain:

```java
@McpTool(description = "Finds orders by customer email. Use before refunding.")
public List<Order> findOrders(@McpParam(value = "email", description = "Customer email") String email) { ... }
```

Outside a dev profile the endpoint requires `cn1.mcp.token` (the server refuses to
start without it) and a browser `Origin` other than localhost is refused. On a dev
profile the token is optional, but a server without one listens on `127.0.0.1`
only: its tools reach the database. To test from a phone or another machine, set
`cn1.mcp.token` (the MCP client then sends it as a bearer token) or turn the
endpoint off with `cn1.mcp.enabled=false`. A packaged binary contains the MCP
endpoint only when it has an `@McpTool` method, or `cn1.mcp.enabled=true`
(or `cn1.mcp.path`/`cn1.mcp.allowedOrigins`) in a properties file; the dev tools are only in the dev run.

## Testing

Tests are JUnit 5 in `backend/src/test/java`, written against Spring Boot's test
API under `com.codename1.backend.test`. The module's pom already has the
dependencies and the goals.

```java
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;

@BackendTest                                  // = @SpringBootTest; the build wires it
class NotesTest {
    @Autowired MockMvc mvc;                   // in process, no socket

    @Test
    void readsANote() throws Exception {
        mvc.perform(get("/notes/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("first"));
    }
}
```

- **Unit test** a service by constructing it: injection is constructor calls, so
  `new Notes(new FakeStore())` is the whole setup.
- **Over a real port**: `@BackendTest(webEnvironment = BackendTest.WebEnvironment.RANDOM_PORT)`,
  then `@Autowired TestRestTemplate rest` (`getForObject`, `getForEntity`,
  `postForEntity`, `exchange`, `withBasicAuth`) and `@LocalServerPort int port`.
- **Replace a bean**: a static nested `@TestConfiguration` class with a
  `@Bean @Primary` method. Works on the JVM and compiled.
- **Mock a bean**: `@MockitoBean Store store;` with `org.mockito:mockito-core`
  added as a test dependency. JVM only.
- **Settings**: `@BackendTest(properties = {"key=value"})`. These beat the
  environment. The profile is `test` (a development profile): an in-memory
  SQLite database, tables created.
- Test classes with the same configuration share one running server; do not
  rely on test order.

Run:

```bash
mvn -pl backend -Dcodename1.platform=backend test
mvn -pl backend -Dcodename1.platform=backend test -Dcn1.backend.compiledTests=true   # also as a native binary
```

The compiled run translates the same tests into a native test binary (needs
clang, as `cn1:backend-package` does; skipped on Windows). Classes that import
Mockito are left out of it. Use only the JUnit 5 API the shim provides:
lifecycle annotations, `@Disabled`, `@DisplayName`, `@Tag`, `Assertions`
(including `assertThrows` with a lambda) and `Assumptions`. Results:
`backend/target/surefire-reports/TEST-*.xml` and `TEST-*-compiled.xml`.

## Build errors you will see, and what they mean

| Message fragment | Fix |
| --- | --- |
| `needs a X, and no bean has that type` | Annotate the implementation `@Component`, or add a `@Bean` method. |
| `could receive any of ...` | Mark one `@Primary` or inject with `@Qualifier("name")`. |
| `The constructors form a cycle` | Inject one side through an `@Autowired` field or setter. |
| `is final, so it can only be set by the constructor` | Take it as a constructor parameter. |
| `@Async method ... returns X` | Return `void` or `java.util.concurrent.Future` (`AsyncResult.of(...)`). |
| `the hour field of "..." names 25` | Fix the cron expression (six fields, seconds first). |
| `Parameter N of @McpTool ... has no @McpParam` | Name every tool parameter. |

## Loop before reporting "done"

1. `CN1_PROFILE=dev mvn -pl backend -Dcodename1.platform=backend cn1:backend`
2. `backend_routes` / `backend_beans` — the wiring is what you meant.
3. `backend_call` each changed endpoint; `backend_requests` with `failuresOnly`.
4. `backend_sql` to confirm what was written.
5. For a full-stack change, drive the app too — see `references/full-stack-loop.md`.
