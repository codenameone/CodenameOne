---
title: "Kafka Listeners, Mail and Live Streams in Your Backend, With Records as Entities"
slug: kafka-mail-and-records-in-your-java-backend
url: /blog/kafka-mail-and-records-in-your-java-backend/
date: '2026-11-16'
author: Shai Almog
description: "The Codename One backend now sends and receives messages over Kafka, RabbitMQ, Redis Streams, SQS and Pub/Sub, accepting Spring's listener annotations as written. It sends transactional email through SMTP or Amazon SES, streams server-sent events, and maps Java records as ORM entities on the client and the server, something JPA doesn't allow."
feed_html: "<img src=\"https://www.codenameone.com/blog/kafka-mail-and-records-in-your-java-backend.jpg\" alt=\"Kafka, mail and records in the Codename One backend\" /> Kafka, RabbitMQ and Redis with Spring's annotations, transactional mail, server-sent events, and Java records as entities, which JPA still doesn't allow."
series: ["release-2026-11-13"]
---

![Kafka, mail and records in the Codename One backend](/blog/kafka-mail-and-records-in-your-java-backend.jpg)

On Friday I said that [one codebase pays twice](/blog/shopify-was-wrong-and-right-to-dump-react-native/) when it includes the server. That claim is worthless if the server can't do what a real service does: consume a queue, send a receipt email, push live updates and store data in the shape the language prefers. This week the Codename One backend does all four, and on the last one it does something JPA still forbids.

The backend compiles Java to C and then to a native executable. Listener wiring, repositories and builders are generated at build time, with no proxies and no reflection at run time. The annotations follow Spring's, because a controller that moves over with no edits is worth more than a clever new API.

![Java compiled to C to a native binary, and a request travelling through a host thread and a virtual thread to the handler](/developer-guide/img/backend-architecture.png)

## Records as entities: ahead of JPA

Records have been in Java since 16. They're the obvious shape for data: immutable, with `equals`, `hashCode` and `toString` for free. Yet the Jakarta Persistence spec still requires an entity to be a non-final class with a no-argument constructor that the provider can mutate. A record is none of those, so you can't make one a JPA entity. Hibernate accepts records as embeddables and stops there.

Codename One's ORM maps them, on the device and on the server:

```java {class="cn1-no-try"}
@Entity
public record Note(@Id long id, String title, int score,
        @Version long version, @DbTransient String scratch) {}
```

The build generates a builder for each record, including a copy builder for changes:

```java {class="cn1-no-try"}
Notes notes = Repositories.create(Notes.class, session);
session.beginTransaction();
Note saved = notes.save(NoteBuilder.create().title("Groceries").score(3).build());
// saved.id() is the generated key
Note changed = notes.save(NoteBuilder.of(saved).score(5).build());
session.commitTransaction();
```

`save` returns a managed copy that carries the generated key and the new version. The record you passed in is never changed, which is what immutability is supposed to mean. Records are read through their accessors and built through the canonical constructor, and nothing is woven into the class.

Relationships use `Ref<T>`, because a record can't have a field set after it's built:

```java {class="cn1-no-try"}
@Entity
public record Purchase(@Id long id, @ManyToOne Ref<Customer> customer,
        long amountCents) {}

Purchase p = session.find(Purchase.class, id);
long customerId = p.customer().id();          // no query
String name = p.customer().get().name();      // loads the customer
```

There are limits, and the build tells you when you hit one. Records need sources compiled at Java 17. A record that holds its own entity type, a `Map` relationship or a lazy `Optional` fails the build with an explanation, and relationships inside an embedded record aren't supported. Records run on iOS and every other target ParparVM translates.

I like this one because it's a case of us leading instead of following. JPA users have asked for record entities since records shipped.

## Kafka, RabbitMQ and Redis with the annotations you know

Messaging is one API over seven transports, selected by a URL:

```properties
cn1.messaging.url=kafka://kafka-1:9092,kafka-2:9092
```

`memory:`, `db:`, `redis://` (Streams), `amqp://` (RabbitMQ), `kafka://`, `sqs://` and `pubsub://` are supported, with TLS forms for each. Spring Boot's own properties, such as `spring.kafka.bootstrap-servers`, are read too. With a database and no URL, messages go through a table, which is often all a small service needs.

Sending a message inside a transaction writes it to an outbox, and it goes out after the commit. A rolled back order never announces itself:

```java {class="cn1-no-try"}
@Component
public class Checkout {
    private final DataSource db;
    private final MessageTemplate messages;

    public Checkout(DataSource db, MessageTemplate messages) {
        this.db = db;
        this.messages = messages;
    }

    @Transactional
    public void place(Order order) throws IOException {
        db.execute("INSERT INTO orders (id, customer) VALUES (?, ?)",
                new Object[]{Long.valueOf(order.id), order.customer});
        messages.send("orders.placed", order.customer, order);
    }
}

@Component
public class Shipping {
    @MessageListener(value = "orders.placed", group = "shipping", concurrency = 4)
    public void placed(Order order) {
        // ship it
    }
}
```

Code ported from Spring can keep `@KafkaListener(topics = ..., groupId = ...)`, `@RabbitListener(queues = ...)` and `@JmsListener` as written. Delivery is at least once, with consumer groups, retries and a dead letter destination per topic.

{{< mermaid >}}
flowchart LR
    T["@Transactional place()"] --> DB[(orders)]
    T --> OB[(outbox)]
    OB -->|after commit| BR[Kafka / RabbitMQ / Redis / SQS]
    BR --> L["@MessageListener<br/>group shipping"]
    L -->|fails repeatedly| DLQ[orders.placed.dlq]
{{< /mermaid >}}

Each broker protocol is written on the backend's own network layer, with no client library, and a protocol your configuration doesn't name isn't compiled into the executable. That has a cost: Kafka reads gzip but not snappy, lz4 or zstd yet, and doesn't do transactions or SASL. The [messaging chapter](/developer-guide/#backend-messaging) lists every limit.

The same release adds a shared key value store, `cn1.kv.url=redis://...`, which works with Redis, Valkey, and KeyDB. It backs the cache, sessions, rate limiting and the locks that keep a `@Scheduled` job from running on two servers at once.

## Email from the backend

Signup confirmations, receipts, password resets. Every service sends mail, and every service eventually sends a welcome email for an account whose transaction rolled back. Codename One stages mail sent inside a transaction and sends it on commit:

```java {class="cn1-no-try"}
@Transactional
public void register(String name, String email) throws IOException {
    db.execute("INSERT INTO accounts (name, email) VALUES (?, ?)",
            new Object[]{name, email});
    mail.send(Mail.to(email).subject("Welcome, " + name)
            .text("Your account is ready.").build());
}
```

```properties
cn1.mail.url=smtp://${SMTP_USER}:${SMTP_PASSWORD}@smtp.example.com:587
cn1.mail.from=Acme <no-reply@example.com>
```

SMTP with STARTTLS, SMTPS and Amazon SES are supported, plus `log:` and `memory:` for development and tests. Templates, attachments and inline images work. Failed sends retry with backoff for up to six attempts, permanent failures aren't retried, and mail that gives up lands in a table you can inspect. The built-in security module now uses it for email verification, password reset and mailed sign-in codes. Receiving mail and DKIM signing aren't supported; use your provider for those.

## Server-sent events

Prices, progress bars and LLM tokens all need a server that pushes. WebSockets are the heavy answer. Server-sent events are plain HTTP, pass through proxies, and reconnect by themselves:

```java {class="cn1-no-try"}
@GetMapping("/prices")
public HttpServer.Response prices(HttpServer.Request request) {
    return Sse.stream(request, events -> {
        long last = parse(events.lastEventId());
        for (int idle = 0; idle < 600; idle++) {
            long newest = PriceBoard.newest();
            if (newest > last) {
                events.event("price").id(Long.toString(newest))
                        .json(PriceBoard.since(last));
                last = newest;
            } else if (idle % 15 == 0) {
                events.ping();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
        }
    });
}
```

`lastEventId()` lets a reconnecting client resume where it stopped, and `HttpProxy` relays event streams unchanged, so a backend can forward an LLM provider's token stream to the app without buffering it.

## More of Spring, fewer workarounds

We keep porting real services to the backend, starting with our own build cloud, and every port finds small gaps. This round closed a long list: per call repository transactions, `@Lock` on derived finds and queries, the `Before`, `After`, `Between` and `IgnoreCase` query keywords, repeated `@RequestParam`, catch-all route variables like `{*path}`, inherited controller mappings, `CommandLineRunner` and `ApplicationRunner`, ready events, executor queue limits with a `TaskDecorator`, tagged gauges and histograms, and header rewriting in `HttpProxy`.

None of these is a headline. Together they decide whether a Spring controller moves over with no edits or with twenty.

## Learn more

The [messaging](/developer-guide/#backend-messaging) and [mail](/developer-guide/#backend-mail) chapters have the full configuration, server-sent events are in the web chapter, and record entities are in the ORM chapter under "Record entities." If you have a Spring service you'd like to move and something's missing, tell me which annotation.

{{< giscus >}}
