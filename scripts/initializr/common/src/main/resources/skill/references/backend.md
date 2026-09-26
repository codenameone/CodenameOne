# The Backend Module — Spring-Style Server Code

The `backend/` module is the server side of the app, written in Java with Spring's
annotations under Codename One package names. `@RestController`, `@Service`,
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
- **Imports come from `com.codename1.backend.annotations`**, never
  `org.springframework.*`.

## Beans and injection

```java
package com.example.myapp;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.*;

@Repository
public class NoteStore {
    private final DataSource db;                    // built-in: the connection pool
    public NoteStore(DataSource db) { this.db = db; }
    // ...
}

@Service
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
| `@Component`, `@Service`, `@Repository` | A bean. Name defaults to the simple class name, decapitalized. |
| `@Configuration` + `@Bean` | Factory methods; parameters are injected. Calling one `@Bean` method from another does NOT share the instance — take the other bean as a parameter. |
| `@Autowired` | Constructor (needed only when there are several), field, or setter. `required = false` allowed. |
| `@Qualifier("name")`, `@Primary` | Choose between several beans of one type. Two candidates and neither is enough → build error. |
| `@Value("${key:fallback}")` | A configuration value, converted to String, a primitive or box, or an enum. |
| `@ConfigurationProperties("mail")` | Calls the bean's setters from `mail.*` keys (`setMaxSize` reads `mail.maxSize` or `mail.max-size`). |
| `@Scope("prototype")` | A new instance at each injection point. |
| `@RequestScope`, `@SessionScope` | One per HTTP request / session. Injected into a singleton through a generated subclass, so the class must not be final and needs a no-argument constructor. |
| `@Lazy` | Built on first use (same subclass rule). The stand-in runs the no-argument constructor once at start-up, so put expensive set-up in `@PostConstruct`, which runs only on the real instance. |
| `@Profile("dev")`, `@Profile("!prod")` | Only on that profile (`CN1_PROFILE`). |
| `@ConditionalOnProperty("feature.x")`, `@ConditionalOnMissingBean` | Conditional beans. |

Built-in injectables: `Config`, `DataSource`, `orm.EntityManager`,
`com.codename1.orm.session.Session` (the current transaction's session), and — in a
request or session bean only — `HttpServer.Request` and `HttpSession`.

**Parameter names do not survive compilation.** `@PathVariable`, `@RequestParam`,
`@RequestHeader` and `@McpParam` always need their name spelled out.

## Transactions

```java
@Service
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
- Unchecked exceptions and `Error`s roll back; checked exceptions commit.
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
@Service
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
  default) on a pool. **Rule of thumb: anything that talks to a database or makes an
  outbound HTTP call belongs on `PLATFORM`** — on this runtime those calls block the
  host thread under a virtual thread. `Future.get()` on a virtual thread yields its
  host instead of blocking it.
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
`cn1.session.store=jdbc` keeps sessions in the database so any replica can serve
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
- Management endpoints (always on in dev, otherwise `cn1.management.enabled=true`
  plus `cn1.management.token`): `/manage/health`, `/manage/metrics`,
  `/manage/prometheus`, `/manage/jobs`, `/manage/managed`, and
  `POST /manage/managed/{bean}/{operation}`.

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
start without it) and a browser `Origin` other than localhost is refused.

## Testing

- **Unit test** a service by constructing it: injection is constructor calls, so
  `new Notes(new FakeStore())` is the whole setup.
- **Integration test** the wired server — what `@SpringBootTest` does — by
  starting the generated wiring on a free port:

```java
Properties p = new Properties();
p.setProperty("cn1.server.port", "0");             // or a free port you picked
Backend server = Backend.builder(Config.of(p, "test"))
        .quiet()
        .application(new BackendWiring())           // generated into target/classes
        .start();
try {
    // HTTP calls against server.getServer().getPort()
} finally {
    server.stop();
}
```

The `test` profile is a development profile: in-memory SQLite, tables created.

## Build errors you will see, and what they mean

| Message fragment | Fix |
| --- | --- |
| `needs a X, and no bean has that type` | Annotate the implementation `@Service`/`@Component`, or add a `@Bean` method. |
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
