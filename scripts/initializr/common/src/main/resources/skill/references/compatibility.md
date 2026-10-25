# Bringing existing code in: the compatibility tools

Before you rewrite an existing application in Codename One, check whether it can
be **built as it is**. Codename One has build-time compatibility tools for four
ecosystems. Each one takes the original sources in a directory under
`common/src/main/` (Gradle: `src/main/`), turns them into ordinary Codename One
code during the build, and runs the result on every target -- iOS, Android,
desktop and web. Nothing interprets the original framework at run time.

| You have | Put sources in | Import goal (Maven) | Runtime / tool | Deep reference |
| --- | --- | --- | --- | --- |
| A classic Android app (Activities, XML layouts, `res/`, Java/Kotlin) | `src/main/android` | `mvn cn1:import-android-project -Dcn1.android.import=<path>` | `codenameone-android-compat` (the `android.*` API over CN1 components) | `references/android-to-cn1.md`, developer guide "Running Android applications" |
| A Swing / AWT / SwingX app | `src/main/desktop` | `mvn cn1:import-desktop-project -Dcn1.desktop.import=<path>` | Swing compatibility layer | `references/swing-comparison.md`, developer guide "Running Swing and JavaFX applications" |
| A JavaFX app (incl. FXML and JavaFX CSS) | `src/main/desktop` | same goal as Swing | JavaFX compatibility layer; FXML and CSS compiled at build time | developer guide "Running Swing and JavaFX applications" |
| Flutter / Dart widget code | `src/main/flutter` | `mvn cn1:import-flutter-project -Dcn1.flutter.import=<path>` | `transcode-flutter` goal compiles Dart to Java; `FlutterUI.wrap` / `FlutterUI.runApp` | `references/flutter-to-cn1.md` |
| A Unity 2D project | `src/main/unity` | `mvn cn1:import-unity-project -Dcn1.unity.import=<path>` | C# assembly translated to Java; needs the .NET SDK at build time | developer guide "Unity" chapter |

Gradle projects pick up the same directories automatically (the plugin adds the
runtime and the build steps when the directory exists). The import goals are
Maven goals; in a Gradle project copy the sources into the directory by hand.

## How to work with them

1. **Import, then build.** The build is the coverage check: an API the layer does
   not implement is a **build error** naming the original class/member and source
   line, not a run-time surprise. Read the errors as the porting to-do list.
2. **Fix gaps with Codename One code, not by stubbing the layer.** Replace an
   unsupported call with the CN1 equivalent (see `ui-components.md`,
   `java-api-subset.md`), or isolate desktop/phone-only behaviour behind a check.
3. **Mix freely.** The ported code and new Codename One code live in one app:
   an Android `View` exposes its CN1 component via `getPeer()` (see `android.view.View` in `maven/android-compat`);
   `SwingInterop.asComponent` / `FxInterop.asComponent` wrap Swing or JavaFX UI
   as a `com.codename1.ui.Component`; `FlutterUI.wrap` returns a `Container`.
   This is how to migrate a screen at a time.
4. **Verify behaviour, not just the build.** Run it in the simulator and drive
   the flows (see `mcp-agent-control.md`). Translation succeeding does not mean
   every callback is honoured -- Flutter in particular is experimental.

## Limits to tell the user up front

- No binary drop-in: third-party jars that use `java.awt`, `javax.swing`,
  `javafx` or Android APIs are not relocated and fail the compliance check.
- No reflection-driven frameworks, class loaders, `ServiceLoader`, OSGi.
- Android: Jetpack Compose is not covered.
- Desktop coordinates are logical pixels scaled to the device density.

## Samples to learn from

Small, complete inputs that CI builds through each tool (in the Codename One
repository, `https://github.com/codenameone/CodenameOne/tree/master/scripts/...`):

- `android-compat-samples/gallery` -- an Android Studio project (Java + Kotlin, `res/`).
- `desktop-compat-samples/swing-gallery`, `desktop-compat-samples/javafx-gallery`.
- `flutter-apps/field_notes`, `flutter-apps/pet_adoption` -- Flutter apps ported with the transcoder.
- `unity-compat-samples/*` -- Unity 2D scenes and games.

For idiomatic *native* Codename One code to evolve toward, see `references/demos.md`.
