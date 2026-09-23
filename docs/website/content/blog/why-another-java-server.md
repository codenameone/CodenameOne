---
title: "We Didn't Want to Build Another Java Server"
slug: why-another-java-server
url: /blog/why-another-java-server/
date: '2026-09-18'
author: Shai Almog
description: "Why we brought Codename One's mobile runtime to the server: small native Java executables, Go comparisons, and shared code from the app to its database."
feed_html: '<img src="https://www.codenameone.com/blog/why-another-java-server.jpg" alt="Small servers, one Java stack" /> Small native Java servers, shared application code, and the engineering behind this week in Codename One.'
series: ["release-2026-09-18"]
---

![Small servers, one Java stack](/blog/why-another-java-server.jpg)

## Why Build Another Java Server?

The last thing we wanted to build was another Java server framework. We have Spring, Micronaut, Quarkus, and enough ways to answer an HTTP request to fill a conference schedule.

Yet we know Java developers who pick Node.js or Go for their newer projects. Why?

The answers varied, but two kept coming up: size and performance, especially for microservices and serverless deployments; and full stack development. A process that starts for one request has a different budget from a server that stays up for a month. For full stack development, Codename One lets the app and server share entity classes and validation rules. One REST contract generates the app client and server dispatcher, while the same ORM annotations generate data access objects (DAOs) for local SQLite and the server database.

We've been using GraalVM in our backend for a while. It's an amazing tool for Spring development. But our CI takes about 30 minutes for a server that isn't huge. The resulting binary is hundreds of megabytes, and the process uses hundreds of megabytes of RAM. That's our application and build pipeline, but it's the bill we have to pay.

So we started benchmarking Go to see whether we could do better. We can. More interestingly, we already had much of what we needed to make a small server useful: a native Java runtime, generated REST clients, shared models, and an ORM. Bringing those pieces together gives us something we would want to use for a new project.

**Codename One now has an experimental native backend.** Write a Java controller, run it on the JVM while developing, and compile it to a native executable for deployment. The database layer supports SQLite, PostgreSQL, MySQL, and MariaDB. The app and backend can share their business rules, entity classes, and API contracts.

## Coming This Week

Today we're introducing the native backend and the thinking behind it. Over the next six days, we'll work through the shared stack and the other features in this release:

- **Saturday, September 19:** {{< post-link path="/blog/java-backend-shared-models" text="One Java Model from the App to PostgreSQL" >}}. Build a reminder service with shared validation, generated DAOs, and database transactions.
- **Sunday, September 20:** {{< post-link path="/blog/vault-encryption-browser-phone" text="One Vault, from Your Phone to the Browser" >}}. Encrypt synchronized records and choose how users unlock them, including browser passkeys.
- **Monday, September 21:** {{< post-link path="/blog/invite-link-through-app-store" text="The Hard Part of Invite a Friend Is the Install" >}}. Follow an invitation through installation to the activity and purchases it brings into your app.
- **Tuesday, September 22:** {{< post-link path="/blog/xcode-27-build-settings" text="Xcode 27: The Build Settings That Can Stop a Release" >}}. Remove obsolete build hints and understand the deployment and launch checks in the updated builder.
- **Wednesday, September 23:** {{< post-link path="/blog/native-desktop-themes-experiment" text="A Desktop Theme Has to Know About the Mouse" >}}. Try the experimental native desktop themes, including hover behavior and light and dark appearances.
- **Thursday, September 24:** {{< post-link path="/blog/parparvm-compiles-itself" text="The Java Compiler That Became Its Own Test Case" >}}. See what self-hosting exposed in our runtime and where we're still chasing HotSpot.

## A server is another constrained device

Most Java server work starts with a large runtime and asks how much of it can be removed. We started with the runtime we use on phones.

ParparVM translates Java bytecode to C, then compiles and links the reachable program into a native executable. It already has to care about startup time, memory pressure, and code size. Taking that runtime to a small server is a fairly natural move once you stop treating the server as an unlimited machine.

Our runtime is deeply optimized for ARM. Years of running on phones and Apple Silicon have made that architecture central to our work, including [NEON paths for UTF-8 conversion](https://github.com/codenameone/CodenameOne/blob/2697dcfa2f0425170efd08655243032571b65869/vm/ByteCodeTranslator/src/nativeMethods.m) and [ARM64 assembly for virtual-thread switching](https://github.com/codenameone/CodenameOne/blob/2697dcfa2f0425170efd08655243032571b65869/vm/ByteCodeTranslator/src/cn1_virtual_thread_asm.S). As ARM becomes more important on servers, with offerings such as [AWS Graviton](https://aws.amazon.com/ec2/graviton/), that investment has another place to pay off.

GraalVM makes a large Java application much smaller relative to what it does. We want to start lower: a small native process that grows as you add the behavior your service needs.

{{< mermaid >}}
flowchart LR
    Shared[Java models and business rules] --> App[Mobile and desktop app]
    Shared --> Server[Java controllers and ORM]
    Contract[Shared REST contract] --> Client[Generated app client]
    Contract --> Router[Generated server dispatcher]
    Client --> App
    Router --> Server
    Server --> Bytecode[Java bytecode]
    Bytecode --> C[ParparVM generated C]
    C --> Binary[Native server executable]
    Binary --> DB[(SQLite / PostgreSQL / MySQL)]
{{< /mermaid >}}

That gives us a different starting point for full stack development too. A validation rule can be the same Java method on a phone and on the server. The phone uses it to give immediate feedback. The server runs it again before storing the data. Sharing the rule removes duplication; running it on the server preserves the trust boundary.

## What we measured against Go

The [backend guide's recorded comparison](https://github.com/codenameone/CodenameOne/blob/2697dcfa2f0425170efd08655243032571b65869/docs/developer-guide/Backend.asciidoc) runs a plaintext HTTP handler on two pinned cores with 64 connections. These are medians of three interleaved runs. The Go reference here is **fasthttp**.

| Runtime | Requests per second | Median latency | p99 latency | Spawn to first connection |
| --- | ---: | ---: | ---: | ---: |
| Codename One native, static musl build | 595,610 | 0.090 ms | 0.249 ms | 0.77 ms |
| Codename One native, dynamic glibc build | 547,761 | 0.065 ms | 4.06 ms | 2.88 ms |
| Go with fasthttp | 496,293 | 0.104 ms | 2.63 ms | 2.39 ms |
| Same Java handler on the JVM | 187,745 | 0.260 ms | 1.60 ms | 82.5 ms |

![Recorded plaintext HTTP throughput and process startup](/blog/backend-http-comparison.svg)

*The static ParparVM server handles about 20% more requests than the Go reference in this run and accepts its first connection in under a millisecond. Startup measures the process, not a cloud provider's complete cold-start path.*

The static server binary is **7.95 MB**, with recorded resident memory of **10 to 40 MB under load**. The dynamically linked build is 3.19 MB and used 14 MB under load. Go's static binary is 5.63 MB and used 6.3 MB; the JVM handler used 190 MB. Its 0.13 MB JAR also needs a Java runtime installed.

Those are useful numbers for a small service. The throughput tells us we can compete with Go. The startup and footprint tell us where this becomes interesting for deployments that pay for every process.

The handler matters. This plaintext path pools its response and allocates about 0.1 bytes per request. A handler that constructs a map for each response gives the collector more work. Our build generates DTO writers that write fields directly to the response sink, so application code has a practical way to avoid that intermediate map. The [benchmark sources](https://github.com/codenameone/CodenameOne/tree/2697dcfa2f0425170efd08655243032571b65869/vm/backend/benchmarks) include the different response paths.

Java 25's [AOT cache](https://docs.oracle.com/en/java/javase/25/migrate/significant-changes-jdk-25.html) is another useful approach to faster startup, but it still runs on HotSpot. A cache file, a native executable, and a JAR plus its runtime are different deployment artifacts. The table above compares working HTTP servers; our Spring CI experience explains why we started looking.

## How small is a Java Hello World?

We also wanted a baseline with almost no application in it:

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello, world!");
    }
}
```

We built that program three ways on an Apple M4 Max running macOS 26.6.2. The table reports 31 interleaved launches after five warm-up launches per runtime, with the files already in the OS cache. Elapsed time includes process launch, printing, exit, and the common measurement wrapper. The host was not isolated from other activity: its recorded one-, five-, and fifteen-minute load averages were 3.75, 6.17, and 4.94. These elapsed times describe that run; a quiet machine can produce different results.

| Build | Files to deploy | Median elapsed | Median peak RSS |
| --- | ---: | ---: | ---: |
| ParparVM, native release build | 0.39 MB | 3.40 ms | 2.51 MB |
| GraalVM Native Image 21.0.11 | 8.18 MB | 4.91 ms | 8.40 MB |
| JDK 25 with AOT cache and a minimal runtime | 55.54 MB | 18.02 ms | 40.68 MB |

The ParparVM executable is **392 KB**. The GraalVM executable is **8.18 MB**. For Java 25, the deployment includes a `jlink` runtime containing only `java.base`, the Hello World JAR, and its trained AOT cache. All three executables run as ARM64; both native builds use `-O3`, with ThinLTO enabled for ParparVM.

![Hello World deployment size and peak resident memory across three Java runtimes](/blog/java-hello-footprint.svg)

That's the lower starting point we mean when we say we think about the server as a constrained device. Adding HTTP, TLS, and database support grows the binary, as the server measurements above show. Starting with 392 KB leaves a lot of room.

The [source, commands, and individual measurements](/blog/java-hello-benchmark.zip) are included so you can repeat the comparison. These are local process measurements with the runtime versions shown, separate from both the Linux HTTP experiment and our Spring application's CI time.

## Start with one endpoint

We tried to make the API feel familiar to Spring developers. You'll recognize `@RestController`, `@GetMapping`, and the idea of declaring routes on ordinary Java methods. The annotations live in Codename One's packages, and the build generates the routing code.

A freshly generated Codename One project includes a `backend` module. Put this class in `backend/src/main/java/com/example/backend/Health.java`:

```java
package com.example.backend;

import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.RestController;

@RestController
public class Health {
    @GetMapping("/healthz")
    public String health() {
        return "ok";
    }
}
```

From the project root, run:

```bash
mvn -pl backend -Dcodename1.platform=backend cn1:backend
```

Then call it from another terminal:

```bash
curl http://localhost:8080/healthz
# ok
```

The build generates the router and entry point. There is no application server to configure and no handwritten listener to keep alive. The `codename1.platform` property activates the backend module in the Maven reactor.

For native packaging, use:

```bash
mvn -pl backend -Dcodename1.platform=backend cn1:backend-package
```

The {{< post-link path="/blog/java-backend-shared-models" text="backend tutorial" >}} expands the setup, database code, and deployment commands.

## One model reaches the database

The ORM arrived after the original backend work, and it changes what you can build with this first release. We now have pooled connections, portable SQL parameters, generated data access objects, queries, and transactions across the supported database engines.

A shared entity uses the same annotations as the client ORM:

```java
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

On the client, the generated DAO works with local SQLite. In the backend module, the generated DAO uses the server's data source and SQL dialect. A controller can declare an `EntityManager` constructor argument, and the generated entry point supplies it.

```java
Dao<Reminder> reminders = entities.dao(Reminder.class);
List<Reminder> open = reminders.query()
        .eq("done", Boolean.FALSE)
        .limit(20)
        .list();
```

That query names a Java field. The ORM resolves its column and binds the value for the selected database. You don't need a PostgreSQL branch next to every SQLite query.

This is still a small backend. Relationships use explicit foreign-key fields, and production schema migrations belong in your deployment process. But a service with persistent objects, transactions, and a shared app contract is already a useful place to start. Tomorrow's {{< post-link path="/blog/java-backend-shared-models" text="One Java Model from the App to PostgreSQL" >}} builds that service step by step.

## Where Is This Going?

We're committed to keeping Codename One backend agnostic. If Spring, Node.js, Go, or another service works for your app, keep using it. You will never have to adopt our backend to build a Codename One app. That choice stays yours.

We see the backend as an important extension of what we already offer. We've spent years making Java work in small, constrained environments. A service that needs to start quickly and fit into a small deployment belongs in that world. And we can bring the integration with us: shared models and validation, generated REST clients and server dispatchers, and DAOs built from the same entity definitions. That's less code for an app team to duplicate and keep in sync.

We also see potential in embedded Linux systems: a gateway collecting sensor readings, a local service inside an appliance, or an industrial device that needs an API without a large runtime. A small ARM64 executable with HTTP and database access is a useful starting point for those applications. The same shared Java models could connect that device to its mobile app.

We also want our runtime to handle more demanding workloads. A busy server gives us another way to find costs that matter on a phone, even when the two targets need different features.

We added virtual threads to the backend because thousands of connections need somewhere to wait. Giving every waiting connection its own OS thread brings stack and scheduling costs. Giving it a resumable stack lets a small number of host threads keep serving other connections.

These are ParparVM's own virtual threads. On the supported native backend builds, the HTTP server selects them automatically. Application handlers can keep their ordinary blocking style while the socket machinery parks and resumes them.

We aren't bringing that server concurrency model to mobile. It doesn't solve a useful mobile workload for us, and mobile system restrictions make its implementation harder. Other improvements travel very well. The backend work exposed collector buffers that grew during a busy period and retained their peak allocation afterward. The runtime now trims those buffers. A phone also benefits when yesterday's burst stops occupying today's memory.

That's why we want to keep investing in the backend. It gives developers a useful deployment target, makes a shared Java application easier to build, and puts more real work through the compiler and runtime our client apps depend on.

---

The rest of this week's release covers client APIs, platform updates, and the compiler itself. Let's start with encrypted storage on phones and in the browser.

## A vault that reaches the browser

Encrypted storage gets awkward when the same user needs their data on a phone and in a browser. Putting an encryption key beside the encrypted records doesn't answer that problem.

The new `Vault` API gives each vault a random data key and stores wrapped copies of that key. A password can unlock it on a new device. A remembered device can reopen it through its local key mechanism. In supported browsers, a passkey can derive the material needed to unwrap it after user verification.

```java
VaultOptions options = new VaultOptions()
        .policy(UnlockPolicy.SESSION_ONLY)
        .autoLockAfter(5 * 60 * 1000);

Vault vault = Vault.named("notes").configure(options);
vault.enroll(password, options).ready(ok -> {
    vault.seal("note-7", noteBytes)
            .ready(sealed -> upload("note-7", sealed));
});
```

Here `password` is a `char[]`, `noteBytes` contains the note, and `upload` is your application's ciphertext transport. The sync server stores the sealed record. It doesn't need the vault password or data key.

The browser implementation uses authenticated encryption and non-extractable key handles, with a WebAuthn PRF path for passkey unlock. That lets us offer useful protection on a target where handing JavaScript a raw key used to be the tempting shortcut. Sunday's {{< post-link path="/blog/vault-encryption-browser-phone" text="One Vault, from Your Phone to the Browser" >}} walks through enrollment, synchronization, and choosing the unlock policy. [PR #5821](https://github.com/codenameone/CodenameOne/pull/5821) contains the implementation.

## An invitation has to survive the app store

You spend $100 on ads for your app. Did you get your money's worth? A click count won't tell you whether those people installed the app, used it, or bought anything. If you can't follow that path, it's easy to keep paying for ads that don't work.

“Invite a friend” has the same attribution problem. Someone shares a link, a friend installs the app, and that friend might eventually become a paying customer. You want to know which invitation led to that activity. Entire startups have been built around keeping those connections intact.

The difficult part is carrying the invitation code through every handoff. The link leaves one app, opens a browser, and may send the recipient through an app store before your app exists on their phone. Keeping that code in play is a game of volleyball with several courts and very little agreement about the rules.

Codename One now handles that journey through `com.codename1.analytics.invite`:

```java
Invite invite = Invites.create(InviteRequest.create()
        .campaign("team-launch")
        .channel("share_sheet")
        .title("Join our team")
        .description("Use the app with us.")
        .build());
Invites.share(invite, "Come and try this with me");
```

Android carries the code through the Play install referrer. On iOS, a generated App Clip receives the link and passes the code to the full app through an App Group. An already installed app receives the link directly. These are exact code handoffs, so the system doesn't need to guess which click belongs to which installation.

When the user grants analytics consent, later conversion and purchase events carry the invitation's campaign and channel. You can follow an invitation beyond the installation to the activity it brought into your app. Monday's {{< post-link path="/blog/invite-link-through-app-store" text="The Hard Part of Invite a Friend Is the Install" >}} covers the Java API and the platform setup that makes those handoffs work. The implementation is in [PR #5751](https://github.com/codenameone/CodenameOne/pull/5751).

## Xcode 27, and the next round of iOS work

Our builders support Xcode 27. As of this release, it isn't on our cloud build servers; we're waiting for Apple's updates before deciding how to proceed with that rollout.

Some developers have already encountered submission problems tied to older minimum versions in their build settings. [PR #5788](https://github.com/codenameone/CodenameOne/pull/5788) reads the selected SDK's deployment floor and raises generated targets as needed. [PR #5855](https://github.com/codenameone/CodenameOne/pull/5855) also handles the iOS 27 launch-screen and scene-lifecycle requirements. It rejects three removed hints, `ios.generateSplashScreens`, `ios.uiscene`, and `ios.launchStoryboardName`, on every SDK and regardless of their values. Delete those hints entirely before rebuilding.

If Apple rejects your submission for minimum-version or launch configuration reasons, let us know and include the rejection text and build hints. We raised the minimums, but the range of existing project configurations makes your reports useful.

Now that iOS 27 is out, we'll work on device theme fidelity over the next couple of weeks. We'd also like to add deeper support for iPhone Duo's folding behavior as Apple's tooling arrives. Tuesday's {{< post-link path="/blog/xcode-27-build-settings" text="Xcode 27: The Build Settings That Can Stop a Release" >}} explains what changed and what to check in an older project.

## Desktop themes need desktop behavior

Native desktop themes are available in a deeply experimental mode. JavaSE can select Fluent on Windows, Aqua on macOS, and Adwaita on GNOME. The native macOS port can select Aqua too.

```properties
codename1.arg.desktop.themeMode=native
codename1.arg.macos.themeMode=native
```

These settings opt in. Native macOS keeps its current modern theme by default; JavaSE preserves its existing desktop selection. We're keeping the defaults while this work develops.

The work reaches beyond colors. A pointer needs hover behavior that doesn't leak into touch input. Desktop text needs the right font metrics. Light and dark variants need the same component vocabulary so your CSS overrides remain useful across platforms.

Wednesday's {{< post-link path="/blog/native-desktop-themes-experiment" text="A Desktop Theme Has to Know About the Mouse" >}} shows the reference captures and the available controls. [PR #5845](https://github.com/codenameone/CodenameOne/pull/5845) adds the themes and fidelity tests.

## ParparVM compiles ParparVM

Our translator is now self-hosting. ParparVM can translate the Java code of its own translator into a native executable, and that executable can perform another translation.

That is a useful milestone toward building Codename One with Codename One. It's also a demanding application test. The translator works through bytecode, collections, strings, files, exceptions, and a large amount of allocation. Running it outside HotSpot found bugs our existing test workloads hadn't exposed.

One was wonderfully mundane: generated C labels inherited ASM's identity-based names. A negative identity hash could put a minus sign inside a C label. Another was expensive: a constant-pool lookup scanned an ever-growing list for every insertion. Self-hosting gave us both an output comparison and a workload worth profiling.

There are still performance and memory gaps against HotSpot, and we're working through them. Thursday's {{< post-link path="/blog/parparvm-compiles-itself" text="The Java Compiler That Became Its Own Test Case" >}} follows the fixes and the remaining work from [PR #5766](https://github.com/codenameone/CodenameOne/pull/5766).

## Three smaller changes you'll notice

**Routes can name their stops.** [PR #5853](https://github.com/codenameone/CodenameOne/pull/5853) adds place and route-stop labels to vector maps. A delivery route can say “Warehouse,” “Customer,” and “Depot” instead of leaving the user to infer which marker means what.

```java
request.setOriginLabel("Warehouse")
        .addWaypoint(customerLocation, "Customer")
        .setDestinationLabel("Depot");
```

Here `request` is a `RouteRequest`, and `customerLocation` is a `LatLng`. The labels are display text; they don't alter the coordinates sent to the routing service.

**The repository has less history to build around.** [PR #5820](https://github.com/codenameone/CodenameOne/pull/5820) removes the old J2ME, BlackBerry, and retrolambda build paths. Those retired targets no longer need to distract from the ports we maintain.

**Older SVG projects get the missing build step.** A project created before the SVG transcoder could have the artwork but lack the Maven execution that turns it into drawing code. [PR #5778](https://github.com/codenameone/CodenameOne/pull/5778) detects this during a build, runs the transcoder, and adds the missing execution to `common/pom.xml`, retaining `pom.xml.bak`. It's an intentionally active repair for a failure that otherwise looked like a missing image.

## More of the application, fewer handoffs to maintain

This week takes Codename One into the server without taking away your choice of backend. It also makes the browser a more useful place to keep encrypted data, carries invitations across installation, and prepares native builds for another round of platform changes.

The security work connects those pieces. A vault can refuse an unlock policy the device cannot provide. A referral can carry an exact code without fingerprinting a visitor. A shared validation rule can run again on the server, where it matters. The builders can reject a broken launch configuration before it reaches App Review.

That's where we're continuing to push secure-by-default programming: moving repeated, security-sensitive work into APIs and build steps we maintain across the whole stack. You still decide who may read a record or redeem a reward. You should have less platform plumbing to get wrong on the way there.

---

## Discussion

_If you've chosen Go or Node.js for a project after years of Java, what decided it: deployment size, startup, the development loop, or sharing code with the client?_

{{< giscus >}}
