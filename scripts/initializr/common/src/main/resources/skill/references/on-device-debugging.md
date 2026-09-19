# Debugging on a Real Device — Android and iOS

`references/debugging.md` attaches `jdb` to the **simulator**, which is a plain JVM on your own machine. This file is the other case: the bug only shows up in a build running on an Android phone, an iPhone, or one of the native emulators, and you need a Java debugger — and optionally an MCP session — against *that* process.

**Reach for the simulator first.** It is faster, it has no device-side moving parts, and every debugger feature works there. Come here only when the behaviour genuinely differs on a device: ParparVM threading, a native interface, iOS layout under the modern theme, memory pressure, or timing around UIKit and the Android lifecycle.

Both platforms need a build made with an on-device-debug build hint, because the flag is baked into the binary. A release build cannot be attached to.

## Android

Android's runtime already exposes a JDWP socket per debuggable process, so there is no proxy — the Maven goal just drives `adb`.

**Prerequisites.** Android platform-tools installed, with `adb` reachable through `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or `PATH`. The device needs USB debugging enabled and the per-machine RSA prompt accepted.

**1. Turn the hint on** in `common/codenameone_settings.properties`:

```properties
codename1.arg.android.onDeviceDebug=true
```

That flips the generated `AndroidManifest.xml` to `debuggable="true"` and disables R8/proguard so symbols and locals survive.

**2. Build a debuggable APK**, either through the cloud builder or entirely locally:

```bash
# Cloud build. Forces the hint for this one build, so you can skip step 1.
mvn cn1:buildAndroidOnDeviceDebug

# Or fully local: generate the Gradle project and assemble it yourself.
mvn cn1:buildAndroidGradleProject
cd android/target/*-android-source && ./gradlew assembleDebug && cd -
```

**3. Install, launch, forward JDWP, and tail logcat** — one goal does all four:

```bash
mvn cn1:android-on-device-debugging
```

It prints the adb it picked, the target device, the app's PID, and a banner confirming `localhost:5005` is forwarded. Logcat, filtered to that PID, then streams through the same terminal prefixed with `[device]`.

**4. Attach a debugger** from a second terminal (or point your IDE's remote-JVM config at the same port):

```bash
jdb -attach localhost:5005 \
    -sourcepath common/src/main/java
```

By default the goal runs `am set-debug-app -w`, so the app sits paused at startup until you attach. Everything the simulator debugger does works here — this is the real Android runtime, not a proxy.

**Flags worth knowing:**

| Flag | Use it when |
| --- | --- |
| `-Dcn1.android.onDeviceDebug.deviceSerial=<serial>` | More than one device is online (`adb devices` lists serials). |
| `-Dcn1.android.onDeviceDebug.wireless=<ip:port>` | Run `adb connect <ip:port>` first — covers both the Android 11+ *Wireless debugging* pairing flow and the older `adb tcpip` one. |
| `-Dcn1.android.onDeviceDebug.apk=<path>` | Skip APK autodetection and install this file. |
| `-Dcn1.android.onDeviceDebug.jdwpPort=<port>` | Something else already holds 5005. |
| `-Dcn1.android.onDeviceDebug.skipInstall=true` | The build is already on the device. |
| `-Dcn1.android.onDeviceDebug.waitForAttach=false` | You want the app to boot immediately instead of blocking for a debugger — the right choice when what you actually want is the MCP session below, not a breakpoint. |

## iOS

iOS has no JDWP socket of its own, so Codename One adds a listener thread to the ParparVM-generated binary and a desktop proxy speaks JDWP on the IDE's behalf. **The app dials out to the proxy**, which is why the build has to know where your machine is.

**1. Turn the hints on** in `common/codenameone_settings.properties`:

```properties
codename1.arg.ios.onDeviceDebug=true
codename1.arg.ios.onDeviceDebug.proxyHost=127.0.0.1
codename1.arg.ios.onDeviceDebug.proxyPort=55333
# Optional: hold the app at startup until the debugger attaches.
codename1.arg.ios.onDeviceDebug.waitForAttach=true
```

**`waitForAttach=true` blocks an MCP-only session.** It does not merely show a "waiting for debugger" overlay: the app delegate defers the VM callback that boots Codename One until the proxy reports an IDE attached, so `start()` never runs and a `MCP.startSocketServer` call inside it never fires. Set it to `false` whenever the point of the session is driving the app rather than a breakpoint. The Android goal has the same trap and the same answer (`waitForAttach=false`).

`proxyHost` stays `127.0.0.1` for the **native iOS simulator**, which shares your machine's loopback. For a **physical iPhone** it must be your machine's LAN address (`ifconfig` / `ipconfig`), reachable from the phone's Wi-Fi network — the phone opens the connection, so a host that only answers on loopback will never be reached.

**2. Build:**

```bash
# Cloud build for a physical device. Forces the hint for this one build.
mvn cn1:buildIosOnDeviceDebug

# Or generate a local Xcode project (macOS with Xcode only) and run it from there.
mvn cn1:buildIosXcodeProject
```

**3. Start the proxy** and leave it running:

```bash
mvn cn1:ios-on-device-debugging
```

**4. Launch the app**, then wait for the proxy to print that the device connected and the symbols loaded. The binary carries its own compressed symbol table and streams it over on connect, so there is no sidecar file to find.

**5. Attach** once the handshake lines appear:

```bash
jdb -attach localhost:8000 \
    -sourcepath common/src/main/java
```

**Two ports, and mixing them up is the usual mistake:** the *app* dials the proxy on **55333**; the *debugger* attaches to the proxy on **8000**. Override them with `-Dcn1.onDeviceDebug.devicePort` and `-Dcn1.onDeviceDebug.jdwpPort` if either is taken.

**iOS limits you will hit.** Breakpoints, stepping, stack walking, locals, instance fields, arrays, threads, and method invocation on framework and user classes all work. These do not: constructor invocation (`new Foo(...)`), hot-swap, and static field reads. Method invocation is also skipped for `java.io.*`, `java.net.*`, `java.nio.*`, and `com.codename1.impl.*`. If an expression silently refuses to evaluate, that list is usually why — report it rather than concluding the value is wrong.

## Driving the app on the device over MCP

The MCP server from `references/mcp-agent-control.md` is not simulator-only: it binds a loopback port anywhere the platform can bind one, which includes a build running on a device. That gives you `ui_snapshot` / `ui_find` / `ui_activate` / `ui_set_text` against the real thing — stable semantic identifiers instead of guessed screen coordinates.

Start it from your own code, gated so it cannot reach a shipped build:

```java
// In MyAppName.start()
if (Display.getInstance().isDebuggableBuild()) {
    MCP.startSocketServer(8765);
}
```

The port is on the **device's** loopback, not yours, so it needs a forward:

- **Android device or emulator** — `adb forward tcp:8765 tcp:8765`, then connect to `127.0.0.1:8765` on your machine. Tear it down with `adb forward --remove tcp:8765` when you are done. With more than one device online `adb` refuses both commands rather than guessing, so pass the same serial the debug goal used: `adb -s <serial> forward tcp:8765 tcp:8765` and `adb -s <serial> forward --remove tcp:8765`.
- **Native iOS simulator** — it shares your machine's network stack, so `127.0.0.1:8765` already *is* the app's port. Nothing to forward.
- **Physical iPhone** — the port is on the phone's own loopback and there is no Codename One goal that tunnels it. The route is a usbmux TCP relay such as `iproxy 8765 8765` from libimobiledevice. If you do not have that tooling, use the iOS simulator for the MCP loop and keep the physical device for the JDWP session above.

**Set `waitForAttach=false` on both platforms.** It is a convenience on Android and a requirement on iOS, where the VM callback that boots the app is itself deferred until a debugger attaches, so with it left on, the starter above never runs and nothing is listening to forward to.

**Security, unchanged from the simulator case:** loopback is not authentication. Everything on the device can reach the port, which is exactly why `startSocketServer` refuses on a release build. Do not lift that gate to make a device session work, do not leave the starter in shipping code, and remove the forward when the session ends.

## Say what you could not check

Both flows need hardware, an SDK, and — on iOS — a machine on the same network as the phone. If you do not have the device, the platform tools, or a build with the hint, say so plainly instead of reporting that the behaviour was verified.
