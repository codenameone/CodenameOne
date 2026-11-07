---
title: "Port Your Spring Code With Fewer Edits, and Libraries That Bring Their Own Server"
slug: cn1libs-with-a-server-side
url: /blog/cn1libs-with-a-server-side/
date: '2026-11-11'
author: Shai Almog
description: "The Codename One backend now takes more Spring code as it is: structured logging, typed JSON without reflection, CORS, content negotiation, status exceptions and more of the JDK. And a cn1lib can now ship its server half too: controllers, beans, entities and migrations that wire themselves into the server that uses it."
feed_html: "<img src=\"https://www.codenameone.com/blog/cn1libs-with-a-server-side.jpg\" alt=\"A library package with a client and a server half\" /> A cn1lib can now ship its server half too: controllers, beans, entities and migrations that wire themselves in. Plus structured logging, typed JSON and HTTP/2 in the backend."
series: ["release-2026-11-06"]
---

![A library package with a client and a server half](/blog/cn1libs-with-a-server-side.jpg)

The Codename One backend compiles your server to a native executable that starts in 40 ms and idles in about 9 MB. The catch has always been porting: real Spring services lean on dozens of small conveniences, and every one that's missing is an edit. This round we took real services across and fixed what they tripped on, so most of this post is code that now moves over with nothing but its imports changed.

## A Spring controller, mostly unchanged

Here's a typical order endpoint. Every line of the body works on the backend now:

```java {class="cn1-no-try"}
@RestController
@CrossOrigin(origins = "https://shop.example.com", allowCredentials = true)
public class OrderController {
    private static final Logger LOG = Logger.getLogger(OrderController.class);

    @Autowired
    private OrderRepository orders;

    @PostMapping(value = "/orders", consumes = "application/json", produces = "application/json")
    public Order place(@RequestBody Order order) {
        if (order.lines.isEmpty()) {
            throw new ResponseStatusException(400, "An order needs at least one line");
        }
        MDC.put("customer", order.customer);
        try {
            LOG.info("Placing order with {} lines", order.lines.size());
            return orders.save(order);
        } finally {
            MDC.remove("customer");
        }
    }
}
```

What changed to make that work:

- **`consumes` and `produces`** are checked. A request with the wrong `Content-Type` gets a 415, and one whose `Accept` header can't be satisfied gets a 406, before your method runs.
- **`@CrossOrigin`** answers preflight requests and sets the CORS headers, with the same defaults as Spring: any origin, a 30 minute `max-age`, and credentials only when you ask.
- **`ResponseStatusException`** picks the status and the message of the error response, so validation code doesn't need a custom exception handler.
- **`Logger` and `MDC`** in `com.codename1.backend.log` use `{}` placeholders, levels from `logging.level.*` properties as in Spring Boot, and a per-thread diagnostic context for request and customer ids.

The imports point at `com.codename1.backend` instead of `org.springframework`, and that's the edit.

## JSON with your own classes, and no reflection

Controllers already turned plain classes into JSON. Now your own code can do the same anywhere, through `Json`:

```java {class="cn1-no-try"}
public class Order {
    public long id;
    public String customer;
    @JsonProperty("placed_at")
    public Date placedAt;
    public List<OrderLine> lines = new ArrayList<OrderLine>();
}

Order order = Json.read(body, Order.class);
List<Order> history = Json.readList(response, Order.class);
String text = Json.write(order);
```

The codecs are generated at build time for the classes you use, so there's no reflection at run time and nothing for the native build to strip away. `Json.convert` turns an already parsed map into a typed object, which helps when you only know the type after you've looked at a field.

## More of the JDK your code already uses

The backend runs on our own class library, so every missing JDK class used to be a rewrite. These are now there:

```java {class="cn1-no-try"}
BigDecimal total = BigDecimal.ZERO;
for (OrderLine line : order.lines) {
    total = total.add(line.price.multiply(BigDecimal.valueOf(line.quantity)));
}
YearMonth billing = YearMonth.now();
LocalDate due = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

if (!order.customer.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+")) {
    throw new ResponseStatusException(400, "Not an email address");
}

Semaphore gate = new Semaphore(4);                       // at most four calls out at once
ConcurrentLinkedQueue<Order> outbox = new ConcurrentLinkedQueue<Order>();
```

`String.matches`, `split` and `replaceAll` work, and so does `java.util.regex`, which the build moves to our own regex engine for you. `Flow` and `InetAddress` are there too.

The HTTP client can insist on HTTP/2 with `setRequireHttp2(true)`. A streaming XML pull parser reads feeds and SOAP replies, and it refuses `DOCTYPE`, so XML external entity attacks have nothing to hook into. XML property lists and an image header reader for PNG, JPEG, GIF and WebP round it out.

## Data and security

The ORM learned `@Lob` and the `insertable` and `updatable` flags on `@Column`, and `CrudRepository` gained more of Spring Data's methods. On the security side, a `{sha256}` password encoder lets you import a user table hashed by an older system without forcing every user to reset, and remember-me devices keep people signed in on the browsers they trust.

## Libraries that bring their own server

Most features need a server too. Comments need a table and an endpoint. Push needs a place to store tokens. A payment flow needs a webhook. Until now a cn1lib could give you the client half, and the README told you what to write on the server. Everyone wrote it a little differently, and a little wrong.

Now a library can ship both halves, and the server half wires itself in.

## What the library carries

{{< mermaid >}}
flowchart LR
    L[notelib.cn1lib] --> C[Client half<br/>UI, API calls]
    L --> Z[backend.zip<br/>controllers, services, entities, db/migration]
    C --> A[Your app]
    Z --> S[Your server build]
    S --> W[Routes, beans, data access and migrations<br/>wired as if the server compiled them]
{{< /mermaid >}}

The Maven cn1lib archetype now has a `backend` module. Put the server classes in `backend/src/main/java` and build. The `.cn1lib` then carries them as `backend.zip` next to the client parts. With Gradle the sources go in `src/backend/java` and publish as `<library>-backend`.

On the server that uses it, you add one dependency:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>notelib-backend</artifactId>
    <version>1.0</version>
</dependency>
```

That's it. The library's `@RestController` classes get routes, its `@Component` and `@Service` classes become beans your code can inject, its entities and repositories get their data access, and its `db/migration` scripts run with yours. All of it is wired at build time, like your own code, so there's no classpath scanning at startup.

Two rules keep this predictable. A jar only takes part if it carries the marker resource `META-INF/codenameone/backend-library`; any other jar is an ordinary dependency and nothing in it gets wired. And if your server defines a class with the same name as the library's, yours wins, so you can override any piece you don't like.

A library can also be server-only: a plain jar that depends on `codenameone-backend` with `provided` scope and carries the marker.

The [backend libraries section](/developer-guide/backend-beans/#backend-libraries) covers packaging, and the [backend web chapter](/developer-guide/backend-web/) covers JSON, the client and the rest.

Tomorrow: {{< post-link path="/blog/debug-dart-in-a-java-debugger" text="Debug Your Dart in a Java Debugger, and JFX Central on a Phone" >}}.

{{< giscus >}}
