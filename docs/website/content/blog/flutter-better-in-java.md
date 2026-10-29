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
- **Wednesday, November 4:** {{< post-link path="/blog/android-apps-everywhere-even-without-google" text="Your Android App on iPhone, and on Phones Without Google" >}}. Room, WorkManager and document pickers in the Android layer, and apps that run on GrapheneOS as they are.
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

Some of this cuts in Flutter's favor and some doesn't. Our runtime ignores a few parameters Flutter honors (the guide lists them), so we aren't doing every bit of Flutter's work. On the other hand, the app is Flutter's own showcase, written in Flutter's idioms. To run it, Codename One carries a runtime that reimplements Flutter's widget, element and layout model on top of Codename One components. A Codename One app never pays for that layer. We win while paying for it.

CI also runs eleven small compute workloads (hash maps, recursion, string building, sorting, arithmetic and more) in both languages. Here the result is mixed, and I want to be clear about where we lose:

| Platform | Compute, geometric mean | Where Flutter is ahead |
| --- | --- | --- |
| Windows | Codename One 1.9x faster | Long arithmetic, slightly |
| Linux | Codename One 1.9x faster | Nowhere |
| macOS | Codename One 1.5x faster | Object allocation (3x) and quicksort, slightly |
| Android | Codename One 1.3x faster | Object allocation (2.3x) and value escape |
| Web | Flutter 3.6x faster | Nearly everything |

On the web we lose compute badly, by 3.6x, and I don't have a fix for that yet. The sizes still favor us there.

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

## The universal platform

Last week I made the case that [a Java word processor beats its Rust twin](/blog/rewrite-everything-in-java-not-rust/). This week the swing is bigger. Flutter, Compose, Swing, JavaFX and Android code all build into the same kind of app and run on iOS, Android, desktop, the web and the watch.

Instead of coding to an operating system, you code to Codename One, and Codename One becomes the operating system. The usual price for that kind of abstraction is performance. On the native targets we measured, we don't charge it.

## More This Week

### Kotlin Multiplatform, from a fan

If Codename One didn't exist, Kotlin Multiplatform is probably what I'd pick. Early on it leaned on heavyweight native widgets, much like React Native, and the team learned from that. Compose Multiplatform today is fast and well designed. Ours runs the same Compose code on more targets, used about a quarter of the CPU per frame on our benchmark screen, and lets you drop to Java where it counts.

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

### Swing and JavaFX, with the limits up front

We can't run every Swing or JavaFX app, and I won't pretend otherwise. There are no class loaders, no runtime reflection and no third party jars compiled against `java.awt`. What does carry over is a lot of real application code: the samples we test include SwingSet2 and Notepad, the JDK's own demos. On Linux the native build is 4 to 6 times smaller zipped than the same app on the JVM, and starts faster in 7 of 8 apps even against a JVM tuned with AppCDS. Sunday's {{< post-link path="/blog/swing-and-javafx-on-phones" text="Swing and JavaFX article" >}} goes through what works, what doesn't, and why the missing pieces matter less than you'd think.

### A bank you can study, and wallets in core

A large share of our users are banks and financial institutions. **Bastion** is a reference implementation for them: a virtual bank and small exchange with passkeys, TOTP, signed requests, device keys, a hash chained audit log, a double entry ledger and an auditor console. Codename One also gained wallet APIs for Ethereum, Bitcoin, and Solana.

```java {class="cn1-no-try"}
Wallet wallet = Wallet.fromMnemonic(words, "");
EvmAddress eth = wallet.getEvmAddress(0, 0);
BitcoinAddress btc = wallet.getBitcoinAddress(BitcoinNetwork.MAINNET, Wallet.BIP84, 0, 0, 0);
SolanaAddress sol = wallet.getSolanaAddress(0);
```

Monday's {{< post-link path="/blog/bastion-hardened-bank-template" text="Bastion article" >}} covers the hardening, one layer at a time.

### Streaming and courses

**Reel** is a streaming service and its server in one Java project. Switch a preset and it becomes a subscription service, a creator platform in the style of YouTube, or an academy with courses, lessons, and quizzes. Tuesday's {{< post-link path="/blog/reel-streaming-and-courses-template" text="Reel article" >}} walks through uploads, moderation, and DRM.

### Android code everywhere, and Android without Google

The Android compatibility layer now covers Room, WorkManager, location, sensors, foreground services and the document picker intents, all running on iOS too. And Codename One apps now run on de-Googled Android such as GrapheneOS out of the box: one APK checks for Play Services at run time. Wednesday's {{< post-link path="/blog/android-apps-everywhere-even-without-google" text="Android article" >}} has the details.

### A modern backend, and a simulator that types

Thursday closes the week with eight backend upgrades (a bounded cache, outbound OAuth tokens, PKI, JSON codecs, streaming uploads, error advice, `java.time` in the ORM and test slices) and a simulator that slides up a virtual keyboard and records video. See the {{< post-link path="/blog/backend-caches-certificates-and-a-simulator-that-types" text="backend and simulator article" >}}.

## Try it

If you have a Flutter app, run the import on a copy and build it for the simulator. Tell me which screen breaks first; that's the most useful bug report I can get right now. If you're on Kotlin, Swing, JavaFX or Android, the rest of this week is for you. And if you think a number above is wrong, the benchmark is in the repository. Rerun it and open an issue.

{{< giscus >}}
