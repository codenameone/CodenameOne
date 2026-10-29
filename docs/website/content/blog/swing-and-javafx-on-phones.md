---
title: "Your Swing and JavaFX Code on iPhone, Android and the Web"
slug: swing-and-javafx-on-phones
url: /blog/swing-and-javafx-on-phones/
date: '2026-11-01'
author: Shai Almog
description: "Codename One now builds a large share of Swing, SwingX and JavaFX application code for iOS, Android, the web and native desktop. It can't run everything, and this post says exactly what it can't. What you get in return is a native build 4 to 6 times smaller and APIs desktop Java never had."
feed_html: "<img src=\"https://www.codenameone.com/blog/swing-and-javafx-on-phones.jpg\" alt=\"Swing and JavaFX code running as a native app\" /> Codename One now builds a large share of Swing and JavaFX application code for phones, the web and native desktop. What works, what doesn't, and what you get in return."
series: ["release-2026-10-30"]
---

![Swing and JavaFX code running as a native app](/blog/swing-and-javafx-on-phones.jpg)

There's a lot of Swing and JavaFX code in the world. Much of it is internal tooling and line of business software that works fine and that nobody wants to rewrite. Then someone asks for it on an iPad, or in a browser, and the answer has always been "rewrite it."

Codename One can now build a large share of that code for iOS, Android, the web and native desktop, as a native app with no JVM inside. SwingSet2 and Notepad, the JDK's own demos, build and run. This post shows what carries over, what it costs in size and start-up (less than the JVM), and why the parts that don't carry over have better replacements in a client app.

## What it does

You drop the project's `src/main` into a Codename One project, or import it:

```bash
mvn cn1:import-desktop-project -Dcn1.desktop.import=/path/to/MyDesktopApp
```

Your code keeps compiling against the JDK's own Swing, or against our JavaFX API under the real `javafx.*` names. A remap step after javac points the bytecode at our implementations of `java.awt`, `javax.swing`, `java.beans`, `javax.imageio`, a SwingX subset and a JavaFX subset, all written on top of Codename One. FXML files compile to Java classes at build time, and JavaFX CSS compiles to a table. Nothing parses XML on the device.

{{< mermaid >}}
flowchart LR
    S[Your Swing / JavaFX source] --> J[javac against the usual APIs]
    J --> R[remap: java.awt, javax.swing, javafx to Codename One]
    F[.fxml and .css] --> X[compiled at build time]
    R --> C[compliance check: unsupported API is a build error]
    X --> C
    C --> P[ParparVM native builds]
{{< /mermaid >}}

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,150px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><a href="/developer-guide/img/desktop-compat-ios-swing-controls.png"><img width="589" height="1278" src="/developer-guide/img/desktop-compat-ios-swing-controls.png" alt="Swing controls running on an iPhone" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Swing controls.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/desktop-compat-ios-swing-tables.png"><img width="589" height="1278" src="/developer-guide/img/desktop-compat-ios-swing-tables.png" alt="A JTable and a JXTable running on an iPhone" loading="lazy" style="width:100%;height:auto" /></a><figcaption>JTable and JXTable.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/desktop-compat-ios-javafx-controls.png"><img width="589" height="1278" src="/developer-guide/img/desktop-compat-ios-javafx-controls.png" alt="JavaFX controls running on an iPhone" loading="lazy" style="width:100%;height:auto" /></a><figcaption>JavaFX controls.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/desktop-compat-ios-javafx-fxml.png"><img width="589" height="1278" src="/developer-guide/img/desktop-compat-ios-javafx-fxml.png" alt="An FXML form styled with CSS running on an iPhone" loading="lazy" style="width:100%;height:auto" /></a><figcaption>FXML with its CSS.</figcaption></figure>
</div>
{{< /guide-block >}}

Unmodified desktop code on an iPhone, captured by our screenshot tests.

Our test set includes two JDK demos, SwingSet2 and Notepad, along with a Swing gallery, a falling blocks game, a JavaFX gallery, an open source 2048 game, an address book and a Mandelbrot explorer.

## The numbers

On Linux, the same app as a `jlink` image against a Codename One native build:

| Application | JVM image, zipped | Native, zipped | Smaller by |
| --- | --- | --- | --- |
| SwingSet2, the JDK demo | 34.1 MB | 8.2 MB | 4.2x |
| Notepad, the JDK demo | 32.8 MB | 6.5 MB | 5.0x |
| JavaFX gallery | 39.1 MB | 7.1 MB | 5.5x |
| Address book (JavaFX) | 39.1 MB | 6.5 MB | 6.0x |

{{< guide-block >}}
<img src="/developer-guide/img/desktop-compat-linux-startup-cold.svg" alt="Cold start on Linux: each sample on the default JVM, a tuned JVM and the Codename One native build" loading="lazy" style="width:100%;height:auto;margin:16px 0" />
{{< /guide-block >}}

On Linux, warm start-up is faster than the default JVM in all 8 apps, by 1.4x to 6.0x. Against a JVM tuned with the serial GC, C1 only and an AppCDS archive, the native build is faster in 7 of 8, by up to 4.1x. The falling blocks game is the exception: 74 ms native against 70 ms tuned. The JavaFX gallery starts in 100 ms native against 603 ms on the default JVM. The same apps build for iOS at 25 to 29 MB and as 2.8 to 4.4 MB Android APKs.

Memory is closer than you might expect. At idle the native Swing gallery uses 64.5 MB against 84.2 MB on a tuned JVM. After a scripted session of clicks and drags, though, it's 81.8 MB against 80.5 MB. The tuned JVM wins that one by a hair, and I'd rather tell you than have you find it.

## What isn't supported

This is the important part, so here it is without softening:

- **No binary drop in.** A third party jar that was compiled against `java.awt`, `javax.swing` or `javafx` fails the build. We don't relocate other people's jars. If the library ships source, you can bring the source.
- **No class loaders, `ServiceLoader`, modules or OSGi.**
- **No runtime reflection.** `java.lang.reflect` and `Class.getMethod` are build errors. `Class.forName` by string doesn't work on Android or the web.
- **No Java serialization, `HttpURLConnection`, `java.net.http` or raw sockets** through these layers.
- **`ProcessBuilder` doesn't exist on a phone.** Check `ProcessBuilder.isSupported()`.
- **Only a subset of the API.** Swing is big. When you call something we don't have, the build tells you exactly what and where:

```
com/example/OrdersPanel#restyleTree()V -> javax.swing.JTree.getUI() is not
supported by the Codename One Swing compatibility layer
(com/example/OrdersPanel.class) at OrdersPanel.java:48
```

That error is the point. You find out at build time, on your machine, with a file and line number. You don't find out from a user on an iPad.

## Why pay for what you don't need?

Look at that list again and ask where those features show up in a typical desktop app. Reflection usually powers a binding framework, a serialization library or a dependency injector. Class loaders power plugins. Serialization powers a cache or a save file.

Codename One does those jobs at build time instead. Binding a form to a model is generated code, with no reflection and no listener bookkeeping:

```java {class="cn1-no-try"}
@Bindable
public class LoginModel {
    @Bind(name = "userField", attr = BindAttr.TEXT)
    @Required
    private String user;
    public String getUser()         { return user; }
    public void   setUser(String u) { this.user = u; }

    @Bind(name = "rememberMe", attr = BindAttr.SELECTED)
    public boolean remember;
}
```

The same goes for JSON and XML mapping, REST and GraphQL clients and the SQLite ORM. They're all generated by the build, so they're type checked, they survive dead code elimination, and they cost nothing at startup. Reflection is what makes a JVM app big and slow to start. Removing it is a large part of why the native build is 4 to 6 times smaller.

## What desktop Java never gave you

Once the app is a Codename One app, it can use everything Codename One has, mixed into the same screens:

- **A real browser component** with two way calls between Java and JavaScript.
- **Camera, barcode scanning, Bluetooth LE, NFC, location and push.**
- **REST, GraphQL and WebSocket clients** generated from annotations.
- **SQLite with an ORM** whose mapping mistakes fail the build.
- **CSS styling**, light and dark themes, and live reload in the simulator.
- **A Java backend** in the same project, sharing your types.

`SwingInterop.asComponent()` and `FxInterop.asComponent()` wrap a Swing component or a JavaFX node as a Codename One component, so you can migrate one panel at a time and put a Codename One map or camera view next to your existing table.

## Try it

The [desktop compatibility chapter](/developer-guide/desktop-interop/) has the full list of what's covered, the import steps and the benchmark method. Start with the screen your users ask for on mobile, run the import and read the build errors. They'll tell you how far you are.

Tomorrow: {{< post-link path="/blog/bastion-hardened-bank-template" text="Bastion: A Hardened Bank Template, and Crypto Wallets in Core" >}}.

{{< giscus >}}
