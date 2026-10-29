---
title: "Flutter Better in Java"
slug: flutter-better-in-java
url: /blog/flutter-better-in-java/
date: '2026-10-30'
author: Shai Almog
description: "Codename One now builds Flutter apps from their Dart source, and the result starts faster and uses less memory than Flutter's own release build on Android, macOS, Windows and Linux, and ships less code on all six platforms we measured. This week also covers Kotlin Multiplatform, Swing and JavaFX, classic Android apps, a hardened bank template, a streaming template and phones without Google Play."
feed_html: "<img src=\"https://www.codenameone.com/blog/flutter-better-in-java.jpg\" alt=\"The Flutter Gallery running in Flutter and in Codename One side by side\" /> Codename One now builds Flutter apps from their Dart source, and the result starts faster, uses less memory and ships less code than Flutter's own release build. This week also covers KMP, Swing and JavaFX, Android apps and more."
series: ["release-2026-10-30"]
---

![The Flutter Gallery running in Flutter and in Codename One side by side](/blog/flutter-better-in-java.jpg)

I usually blog about a feature the week it lands. This one landed a few weeks ago and was in development for months before that: Codename One can now take a Flutter app's Dart source and build it as a Codename One app. The results are amazing. I held this post back because I know how explosive these claims are. I wanted the numbers to come from a CI job that runs on every commit instead of a benchmark I tuned once on my laptop.

In the meantime we also added support for Kotlin Multiplatform with Compose, for a big part of Swing and JavaFX, and for classic Android apps on iOS. More is coming. The part that still surprises me is how well Codename One does against the originals where we measured them. The same Flutter app, built from the same Dart files, starts faster, uses less memory and ships less code when Codename One builds it. That holds on Android, macOS, Windows and Linux, and the sizes hold on iOS and the web too.

## The same Dart, two builds

This is the Flutter Gallery, the Flutter team's own showcase app. On the left it runs as Flutter built it. On the right, Codename One built it from the same Dart source, with no edits.

{{< guide-block >}}
<video controls playsinline preload="metadata" poster="/blog/release-2026-10-30/flutter-vs-codenameone-poster.jpg" style="width:100%;max-width:560px;height:auto;display:block;margin:0 auto" aria-label="The Flutter Gallery running side by side, built by Flutter on the left and by Codename One on the right">
<source src="/blog/release-2026-10-30/flutter-vs-codenameone.mp4" type="video/mp4">
<a href="/blog/release-2026-10-30/flutter-vs-codenameone.mp4">Download the side by side video</a>.
</video>
{{< /guide-block >}}

Look closely and you'll see it isn't pixel identical. In the Reply inbox the avatars are circles on the Flutter side and rounded squares on ours. Text wraps a few characters apart in places. The translation isn't perfect, and the [developer guide](/developer-guide/flutter-interop/) says so up front: Dart support is experimental, some callbacks and parameters are accepted and then ignored, and so far we have verified it against one large app. If you bring a Flutter app over, test every interaction your screens depend on.

But the hard part works. Layout, navigation, theming, text input, lists and the Material and Cupertino widgets all render from the original source, and what you get is a real Codename One app, not a picture of one.

Bringing a project over is one command in a Codename One project:

```bash
mvn cn1:import-flutter-project -Dcn1.flutter.import=/path/to/flutter_app
```

It copies `lib`, the assets and the fonts, adds the Flutter runtime to the build, writes the main class and tells you which of your pub dependencies are covered. You don't need the Dart or Flutter SDK to build the result. The transpiler is Java and runs inside the normal Maven build.

## Coming This Week

Today is the Flutter story and the numbers behind it. Then one post a day:

- **Saturday, October 31:** {{< post-link path="/blog/compose-on-more-screens" text="Your Compose Code, on More Screens and Less CPU" >}}. Kotlin Multiplatform and Compose, running on Codename One, and why the Java version of the same screen is faster still.
- **Sunday, November 1:** {{< post-link path="/blog/swing-and-javafx-on-phones" text="Your Swing and JavaFX Code on iPhone, Android and the Web" >}}. What carries over, what doesn't, and why you might not miss the parts that don't.
- **Monday, November 2:** {{< post-link path="/blog/bastion-hardened-bank-template" text="Bastion: A Hardened Bank Template, and Crypto Wallets in Core" >}}. A virtual bank and exchange with passkeys, signed requests and a double entry ledger, plus wallets for Ethereum, Bitcoin, and Solana.
- **Tuesday, November 3:** {{< post-link path="/blog/reel-streaming-and-courses-template" text="Reel: Your Own Streaming Service or Course Platform" >}}. Community uploads, courses, DRM and moderation in one Java project.
- **Wednesday, November 4:** {{< post-link path="/blog/android-apps-everywhere-even-without-google" text="GrapheneOS, LineageOS, /e/OS and Others Are Now Officially Supported" >}}. Room, WorkManager and document pickers in the Android layer, and apps that run on GrapheneOS as they are.
- **Thursday, November 5:** {{< post-link path="/blog/backend-caches-certificates-and-a-simulator-that-types" text="Caches, Certificates, Streaming Uploads and a Simulator That Types" >}}. Eight backend upgrades and a simulator with a virtual keyboard and video recording.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,150px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><a href="/blog/compose-on-more-screens/"><img width="1024" height="907" src="/developer-guide/img/kmp-keynote-schedule.png" alt="A Compose schedule app rendered by Compose Multiplatform and by Codename One" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Compose, on Codename One.</figcaption></figure>
<figure style="margin:0"><a href="/blog/bastion-hardened-bank-template/"><img width="420" height="860" src="/developer-guide/img/bastion-security-center.png" alt="The Bastion Security Center screen" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Bastion's Security Center.</figcaption></figure>
<figure style="margin:0"><a href="/blog/reel-streaming-and-courses-template/"><img width="1080" height="1920" src="/developer-guide/img/reel-viewer-home.png" alt="The Reel home screen" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Reel's catalog.</figcaption></figure>
<figure style="margin:0"><a href="/blog/android-apps-everywhere-even-without-google/"><img width="736" height="800" src="/developer-guide/img/android-interop-material.png" alt="An Android Material screen rendered by Android and by Codename One" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Android code, on every target.</figcaption></figure>
</div>
{{< /guide-block >}}

## The numbers

Every commit to Codename One builds the Flutter Gallery twice: once with Flutter's own release build and once with Codename One. CI measures both on the same machine in interleaved rounds, so a runner that slows down halfway through penalizes both builds equally. The figures below are from the run on October 29.

{{< guide-block >}}
<img width="760" height="364" src="/blog/release-2026-10-30/flutter-cold-start.svg" alt="Cold start to first frame. Android: 395 ms for Codename One against 1247 ms for Flutter. macOS: 609 against 722 ms. Windows: 126 against 205 ms. Linux: 123 against 198 ms." loading="lazy" style="width:100%;height:auto;margin:16px 0" />
<img width="760" height="364" src="/blog/release-2026-10-30/flutter-memory.svg" alt="Memory at rest. Android: 46 MB for Codename One against 79 MB for Flutter. macOS: 37 against 56 MB. Windows: 58 against 92 MB. Linux: 82 against 224 MB." loading="lazy" style="width:100%;height:auto;margin:16px 0" />
<img width="760" height="502" src="/blog/release-2026-10-30/flutter-code-size.svg" alt="Executable code. Android: 4.7 MB for Codename One against 20.6 MB for Flutter. iOS: 17.3 against 19.1 MB. macOS: 33.2 against 47.9 MB. Windows: 11.4 against 14.1 MB. Linux: 14.2 against 16.1 MB. Web: 12.9 against 25.2 MB." loading="lazy" style="width:100%;height:auto;margin:16px 0" />
{{< /guide-block >}}

CI collects 26 metrics: installed size, executable code and download size on six platforms, plus cold start and memory at rest on the four where we can measure both sides fairly. Codename One is ahead on all 26. The Android timings come from an emulator, and iOS start-up and memory aren't measured; both are explained below. The biggest gaps are on Android, where the app starts in a third of the time and carries a quarter of the code, and on Linux, where it settles at 82 MB against 224 MB.

The comparison is tilted toward Flutter, not away from it. The app is Flutter's own showcase, written by the Flutter team in Flutter's idioms, and Codename One runs it as it is. To do that, Codename One carries a runtime that reimplements Flutter's widget, element and layout model on top of Codename One components. That layer is slower than Codename One code written directly; the [Compose post](/blog/compose-on-more-screens/) shows the same effect, where the imperative Java version of a screen does a third of the work of the declarative one. A Codename One app never pays for that layer. We win while paying for it.

CI also runs eleven compute workloads (hash maps, recursion, string building, sorting, arithmetic and more) in Java and in Dart. On every native platform we measured, Codename One is faster overall, by 1.3x on Android up to 1.9x on Windows and Linux:

{{< guide-block >}}
<img width="760" height="574" src="/blog/release-2026-10-30/flutter-compute.svg" alt="Compute speedups for eleven workloads on Android, macOS, Windows and Linux. Codename One is faster or tied in 38 of the 43 measured cells. Flutter wins object allocation on Android (2.3x) and macOS (3.1x), value escape on Android (1.2x), and quicksort on macOS and long arithmetic on Windows by 2% and 6%. Geometric means: Android 1.3x, macOS 1.5x, Windows 1.9x, Linux 1.9x for Codename One." loading="lazy" style="width:100%;height:auto;margin:16px 0" />
{{< /guide-block >}}

Flutter takes 5 of the 43 cells. Two are within 6%. The real one is object allocation on Android and macOS, where Dart's allocator is quick at short lived objects; that's on our list.

The web is the exception, and it's the one place Flutter is faster. We leave it out of the comparison because Flutter's web build gets some of the workloads wrong: integer arithmetic, random array access and quicksort return different checksums than the same code on every other platform, because Dart on the web runs numbers as JavaScript doubles. A benchmark that gets the answer wrong can't be timed against one that gets it right. On the workloads where both sides agree, Flutter is ahead. The browser is asynchronous by nature, and that's the one environment where Flutter's model helps it rather than costing it. The sizes still favor us there.

Some things aren't measured, and the charts leave them out rather than guess. Flutter can't compile Dart ahead of time for the iOS simulator, so iOS start-up and memory would compare Flutter's debug engine against our release build. We report iOS sizes only. Web start-up needs a browser driver the benchmark doesn't have yet. Android timings come from an emulator, so they compare two builds on one machine rather than predicting a phone. The method, the harness and the raw numbers are in the [Flutter chapter of the developer guide](/developer-guide/flutter-interop/), and the benchmark lives in `scripts/flutter-bench` in the [repository](https://github.com/codenameone/CodenameOne).

## How a compatibility layer can be faster than the original

Each of these compatibility layers is a tool that runs at build time. None of them is a VM inside your app.

{{< mermaid >}}
flowchart LR
    D[Flutter / Dart] -->|transpiled to Java| J
    K[Kotlin Multiplatform / Compose] -->|kotlinc + Compose UI layer| J
    S[Swing / JavaFX] -->|bytecode remapped| J
    A[Android app] -->|resources compiled, bytecode remapped| J
    J[Java bytecode + Codename One core] --> P[ParparVM / native ports]
    P --> I[iOS]
    P --> N[Android]
    P --> M[macOS / Windows / Linux]
    P --> W[Web]
    P --> O[watchOS / tvOS]
{{< /mermaid >}}

The Dart transpiler reads your widget source during `generate-sources` and writes Java. A runtime library then implements Flutter's layout and composition model (constraints down, sizes up, elements reconciled against widgets) on top of Codename One containers. It does not reimplement Flutter's rasterizer. Painting is ours. Compose works the same way: kotlinc compiles your Kotlin, the Compose runtime ships unmodified, and only the UI layer is ours. Swing, JavaFX and Android apps compile against their usual APIs, and a remap step points the bytecode at our implementations.

So the overhead is real, and it's added on top of Codename One. The reason the result still wins is what's underneath. ParparVM compiles Java ahead of time to C and then to a native binary, and its dead code elimination drops whatever your app doesn't reach. There's no Dart VM, no Skia or Impeller engine and no second runtime in the bundle. That's how the Android build ends up with 4.7 MB of executable code against 20.6 MB.

Here is the Dart that calls into the platform. It's ordinary Flutter code:

```dart
final batteryChannel = MethodChannel('com.example.app/battery');

Future<int> getBatteryLevel() async {
  final level = await batteryChannel.invokeMethod('getBatteryLevel');
  return level as int;
}
```

And here is the other side of that channel, in plain Java, with no plugin and no per-platform Swift or Kotlin:

```java {class="cn1-no-try"}
MethodChannel channel = new MethodChannel("com.example.app/battery");
channel.setMethodCallHandler(new MethodChannel.MethodCallHandler() {
    @Override
    public void onMethodCall(MethodCall call, MethodChannel.Result result) {
        if ("getBatteryLevel".equals(call.method())) {
            result.success(87);
        } else {
            result.notImplemented();
        }
    }
});
```

You don't have to move the whole app either. `FlutterUI.wrap()` returns an ordinary `Container`, so a Dart screen can sit inside a Codename One form next to components you wrote in Java:

```java {class="cn1-no-try"}
Form dashboard = new Form("Dashboard", BoxLayout.y());
dashboard.add(new Label("Written in Codename One"));
dashboard.add(FlutterUI.wrap(new Text("Rendered from Dart")));
dashboard.add(new Button("Also Codename One"));
dashboard.show();
```

## Can't Flutter do the same?

No. Flutter could, in theory, run a Codename One app the way we run a Flutter app. It would be slow, because the two models point in opposite directions.

Flutter is asynchronous to the core. Dart runs in an event loop, platform calls are messages that come back as futures, and the engine draws on its own threads. Codename One is synchronous: your code calls the platform and gets an answer. Emulating a synchronous API on top of an asynchronous one means queuing, waiting and copying on every call. Going the other way is cheap: a future is a callback, and a callback is a method call.

The same reason explains why Flutter can't catch Codename One on most platforms. Operating systems, graphics APIs and hardware are synchronous. Codename One calls them directly. Flutter goes through a message channel and its own engine to reach them.

Android makes it obvious. The native platform on Android is Java. Codename One compiles to it and talks to Android the way Android expects. Flutter avoids it, shipping its own VM and engine beside the one the phone already has. That's why Android shows the biggest gap: a third of the start-up time and a quarter of the code.

## We can finally show the code

For years, every Codename One against Flutter discussion went the same way. I'd explain why ahead of time compiled Java with a lightweight UI should produce a smaller, faster app. Someone would explain why it shouldn't. Neither of us had the same app built both ways, so it came down to which camp you were already in.

Now there's one app, built both ways, measured on every commit. If you think a number is wrong, the harness is in the repository and you can rerun it.

The part I didn't expect is how much this made Codename One better. Each compatibility layer exposed problems we had missed for years:

- **Styles were parsed over and over.** Every component re-parsed its pressed and disabled styles from the theme. Caching them took component creation in the Gallery from 131 ms to 17 ms and resident memory from 247 MB to 144 MB.
- **Every string constant was created at startup.** ParparVM materialized every string literal before `main()` ran. Making the pool lazy cut that step from 119 ms to 46 ms.
- **Rounded images were copied.** Rounding an image's corners built a second bitmap, 32 times during the first frame. Doing it in the fragment shader removed the copies and stopped memory from swinging between runs.
- **Images decoded inside the first frame.** Deferring that decode took the first frame from 302 ms to 213 ms.
- **An idle app burned CPU.** The Compose work found, and we fixed, a bug where on phones with a notch, safe area padding kept queuing layout so an idle app sat at 14% to 26% CPU.

None of these fixes are specific to Flutter. If you write Codename One apps in plain Java and never touch a Dart file, your app gets these fixes too: faster start-up, lower memory and an idle CPU that stays idle.

## More This Week

### Your Compose code, on more screens

Kotlin Multiplatform is a good platform, and Compose Multiplatform is well built. Codename One now runs the same Compose code on more targets, used about a quarter of the CPU per frame on our benchmark screen, and lets you drop to Java where it counts.

{{< guide-block >}}
<figure style="margin:24px 0"><a href="/developer-guide/img/kmp-keynote-schedule.png"><img width="1024" height="907" src="/developer-guide/img/kmp-keynote-schedule.png" alt="The same Compose schedule screen rendered by Compose Multiplatform and by Codename One" loading="lazy" style="width:100%;height:auto;max-width:640px;display:block;margin:0 auto" /></a><figcaption>The same Compose screen: Compose Multiplatform on the left, Codename One on the right.</figcaption></figure>
{{< /guide-block >}}

```kotlin
class MyApp : Lifecycle() {
    override fun runApp() {
        ComposeForm { LayoutScreen() }.show()
    }
}
```

Saturday's {{< post-link path="/blog/compose-on-more-screens" text="Compose article" >}} has the numbers and their caveats.

### Swing and JavaFX on phones

A large share of real Swing and JavaFX code now builds for iOS, Android and the web: the samples we test include SwingSet2 and Notepad, the JDK's own demos. On Linux the native build is 4 to 6 times smaller zipped than the same app on the JVM, and starts faster in 7 of 8 apps even against a JVM tuned with AppCDS.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:minmax(0,1.43fr) minmax(0,1fr);gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><a href="/developer-guide/img/desktop-compat-swingset2.png"><img width="918" height="1300" src="/developer-guide/img/desktop-compat-swingset2.png" alt="SwingSet2, the JDK Swing demo, running as a Codename One app on an iPad" loading="lazy" style="width:100%;height:auto" /></a><figcaption>SwingSet2, the JDK demo, on an iPad.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/desktop-compat-fx2048.png"><img width="642" height="1300" src="/developer-guide/img/desktop-compat-fx2048.png" alt="The open source JavaFX 2048 game running as a Codename One app on an Android phone" loading="lazy" style="width:100%;height:auto" /></a><figcaption>The JavaFX 2048 game on a phone.</figcaption></figure>
</div>
{{< /guide-block >}}

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,140px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img width="589" height="1278" src="/developer-guide/img/desktop-compat-ios-swing-controls.png" alt="Swing controls running on an iPhone" loading="lazy" style="width:100%;height:auto" /><figcaption>Swing controls.</figcaption></figure>
<figure style="margin:0"><img width="589" height="1278" src="/developer-guide/img/desktop-compat-ios-swing-tables.png" alt="A JTable running on an iPhone" loading="lazy" style="width:100%;height:auto" /><figcaption>JTable.</figcaption></figure>
<figure style="margin:0"><img width="589" height="1278" src="/developer-guide/img/desktop-compat-ios-javafx-controls.png" alt="JavaFX controls running on an iPhone" loading="lazy" style="width:100%;height:auto" /><figcaption>JavaFX.</figcaption></figure>
<figure style="margin:0"><img width="589" height="1278" src="/developer-guide/img/desktop-compat-ios-javafx-fxml.png" alt="An FXML form running on an iPhone" loading="lazy" style="width:100%;height:auto" /><figcaption>FXML and CSS.</figcaption></figure>
</div>
{{< /guide-block >}}

Sunday's {{< post-link path="/blog/swing-and-javafx-on-phones" text="Swing and JavaFX article" >}} goes through what carries over and why you won't miss what doesn't.

### A bank you can study, and wallets in core

A large share of our users are banks and financial institutions. **Bastion** is a reference implementation for them: a virtual bank and small exchange with passkeys, TOTP, signed requests, device keys, a hash chained audit log, a double entry ledger and an auditor console. Codename One also gained wallet APIs for Ethereum, Bitcoin, and Solana.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,140px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img width="420" height="860" src="/developer-guide/img/bastion-dashboard.png" alt="The Bastion dashboard" loading="lazy" style="width:100%;height:auto" /><figcaption>Four currencies.</figcaption></figure>
<figure style="margin:0"><img width="420" height="860" src="/developer-guide/img/bastion-order-book.png" alt="The BTC/USD market with candles and the order book" loading="lazy" style="width:100%;height:auto" /><figcaption>The BTC/USD market.</figcaption></figure>
<figure style="margin:0"><img width="420" height="860" src="/developer-guide/img/bastion-security-center.png" alt="The Bastion Security Center" loading="lazy" style="width:100%;height:auto" /><figcaption>The Security Center.</figcaption></figure>
</div>
{{< /guide-block >}}

```java {class="cn1-no-try"}
Wallet wallet = Wallet.fromMnemonic(words, "");
EvmAddress eth = wallet.getEvmAddress(0, 0);
BitcoinAddress btc = wallet.getBitcoinAddress(BitcoinNetwork.MAINNET, Wallet.BIP84, 0, 0, 0);
SolanaAddress sol = wallet.getSolanaAddress(0);
```

Monday's {{< post-link path="/blog/bastion-hardened-bank-template" text="Bastion article" >}} covers the hardening, one layer at a time.

### Streaming and courses

**Reel** is a streaming service and its server in one Java project. Switch a preset and it becomes a subscription service, a creator platform in the style of YouTube, or an academy with courses, lessons, and quizzes. {{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,140px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img width="1080" height="1920" src="/developer-guide/img/reel-viewer-home.png" alt="The Reel catalog" loading="lazy" style="width:100%;height:auto" /><figcaption>The catalog.</figcaption></figure>
<figure style="margin:0"><img width="1080" height="1920" src="/developer-guide/img/reel-viewer-player-tracks.png" alt="The Reel player" loading="lazy" style="width:100%;height:auto" /><figcaption>The player.</figcaption></figure>
<figure style="margin:0"><img width="1080" height="1920" src="/developer-guide/img/reel-viewer-course.png" alt="A Reel course" loading="lazy" style="width:100%;height:auto" /><figcaption>A course.</figcaption></figure>
</div>
{{< /guide-block >}}

```java {class="cn1-no-try"}
StreamRequest req = new StreamRequest(manifestUrl)
        .setDrm(new DrmConfig(DrmScheme.WIDEVINE, licenseUrl));
```

Tuesday's {{< post-link path="/blog/reel-streaming-and-courses-template" text="Reel article" >}} walks through uploads, moderation, and DRM.

### Android code everywhere, including GrapheneOS

The Android compatibility layer now covers Room, WorkManager, location, sensors, foreground services and the document picker intents, all running on iOS too. And GrapheneOS, LineageOS, CalyxOS and /e/OS are now officially supported, with a boundary CI verifies: one APK checks for Play Services at run time, and push works through UnifiedPush.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,180px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img width="736" height="800" src="/developer-guide/img/android-interop-material.png" alt="A Material 3 screen on Android and on Codename One" loading="lazy" style="width:100%;height:auto" /><figcaption>Material 3, Android left, Codename One right.</figcaption></figure>
<figure style="margin:0"><img width="736" height="800" src="/developer-guide/img/android-interop-recycler.png" alt="A RecyclerView on Android and on Codename One" loading="lazy" style="width:100%;height:auto" /><figcaption>RecyclerView, same source.</figcaption></figure>
</div>
{{< /guide-block >}}

Wednesday's {{< post-link path="/blog/android-apps-everywhere-even-without-google" text="Android article" >}} has the details.

### A modern backend, and a simulator that types

{{< guide-block >}}
<video controls playsinline muted loop preload="metadata" poster="/blog/release-2026-10-30/simulator-virtual-keyboard-poster.jpg" width="540" height="960" style="width:100%;height:auto;max-width:280px;display:block;margin:16px auto"><source src="/blog/release-2026-10-30/simulator-virtual-keyboard.mp4" type="video/mp4">The simulated keyboard sliding up over a sign up form.</video>
{{< /guide-block >}}

Thursday closes the week with eight backend upgrades (a bounded cache, outbound OAuth tokens, PKI, JSON codecs, streaming uploads, error advice, `java.time` in the ORM and test slices) and a simulator that slides up a virtual keyboard and records video. See the {{< post-link path="/blog/backend-caches-certificates-and-a-simulator-that-types" text="backend and simulator article" >}}.

## The universal platform

Last week I made the case that [a Java word processor beats its Rust twin](/blog/rewrite-everything-in-java-not-rust/). This week the swing is bigger. Flutter, Compose, Swing, JavaFX and Android code all build into the same kind of app and run on iOS, Android, desktop, the web and the watch.

Instead of coding to an operating system, you code to Codename One, and Codename One becomes the operating system. The usual price for that kind of abstraction is performance. We don't charge it. You get more performance, more features, more compatibility and more platforms, and it's free and open source.

If you have a Flutter app, run the import on a copy and build it for the simulator. Tell us which screen breaks first; that's the most useful bug report we can get right now. If you're on Kotlin, Swing, JavaFX or Android, the rest of this week is for you. And if you think a number above is wrong, the benchmark is in the repository. Rerun it and open an issue.

{{< giscus >}}
