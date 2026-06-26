# Native-Backend Simulator (RPC / Catalyst) — Spike Notes

> **Status: paused spike, preserved on branch for future resumption.**
> This branch captures an exploration into replacing the Swing `JavaSEPort`
> simulator with one whose rendering and native services come from the *real*
> iOS port compiled for Mac Catalyst, while the app + CN1 port logic keep
> running on the JVM (so hot-reload and the debugger are preserved). It is a
> working spike, **not** production-ready. Nothing here ships; it is knowledge
> capture so we can decide later whether and how to continue.

## Why this exists / the goal

The current simulator is `Ports/JavaSE/src/com/codename1/impl/javase/JavaSEPort.java`
— a ~17.5k-line Swing monolith that is its own re-implementation of every
platform behavior. Divergence from the real device ports (iOS especially) is a
recurring source of "works in the sim, breaks on device" bugs.

**Idea:** keep the JVM running the app + the CN1 core + the *real* port's Java
logic, but get pixels and native services from the actual iOS port (already
known to compile for Mac Catalyst in this fork). Two backends were explored:

1. **JNI bridge (Workstream B in the plan):** load the iOS port's `nativeSources`
   as `libcn1sim.dylib` and call the ParparVM-convention C functions through
   generated JNI shims. Generator + runtime + macOS view-controller exist under
   `maven/cn1-sim-native/{generator,runtime,macos}`.

2. **RPC pivot (what actually got built out and demoed):** run the real Catalyst
   app (`SimRelayService.app`) as a separate process that owns one Metal surface
   and the native services, and drive it from the JVM over a localhost socket.
   This sidesteps the hardest JNI problem (object-lifetime / GC bridge) and is
   what the screenshots in this spike came from.

Both share the same decorator/tool-proxy decomposition of `JavaSEPort`
(Workstream A), which is independently useful and the most "landable" piece.

## Architecture (RPC pivot — the live path)

```
JVM process (CN1PureSimulator)
├─ Shell universe  ── simulator chrome as Codename One UI (skin, menus, sidebar
│                     tools) — SimulatorShell. Its own CN1 Display.
├─ App universe    ── the user's app + CN1 core + BridgedSimImplementation
│                     (extends the iOS CodenameOneImplementation surface).
│                     Isolated via child-first classloaders.
├─ BridgeRegistry  ── loaded ONCE by the parent; package is delegated upward so
│                     its statics are shared across both universes. The
│                     rendezvous for every cross-universe bridge + simulated
│                     state flag.
└─ RpcRenderBridge ── socket client/server (port 17995). Serializes draw ops +
                      native calls to the relay; receives input + lifecycle
                      events back.
        │  localhost:17995
        ▼
SimRelayService.app  (Mac Catalyst build of the iOS port)
├─ One shared Metal surface (the whole window: skin chrome + app screen + tools)
├─ Native services: camera, native text editing, share sheet, window control...
└─ Captures pointer/key/scroll input, forwards to the JVM.
```

Key invariants and hard-won facts:

- **Two universes, shared BridgeRegistry.** Capability toggles that are *read on
  demand* (biometric, IAP, NFC) are set directly on `BridgeRegistry` from the
  shell menu. Toggles that change *rendering* (larger text, dark mode) go through
  the child-control channel and call `Form.refreshTheme()/forceRevalidate()/repaint()`.
- **One Metal surface, region offsets.** The relay renders the entire window from
  one op stream; `RpcRegionBridge` offsets app vs shell draws. Input is routed by
  rectangle: app-screen viewport → app universe, else → shell Display.
- **Catalyst window quirks:** `NSWindow setContentSize/setLevel` via
  `[uiWindow valueForKey:@"_nsWindow"]` KVC are *ignored* (scene-managed). Resize
  works via `windowScene.sizeRestrictions.minimumSize == maximumSize` then relax.
  Solid dark titlebar via `NSAppearanceNameDarkAqua` + opaque window.
- **macOS Automatic Termination kills backgrounded Catalyst apps** (silent
  EOFException, no crash report). Fixed with `NSSupportsAutomaticTermination=false`
  in Info.plist (the relay build script patches this post-build).
- **Window-ID capture is the only reliable screenshot** during interactive work
  (full-screen grabs catch the user's other windows). See `/tmp/winid.swift`:
  `CGWindowListCopyWindowInfo` → `screencapture -l<id>`.

## Workstream A — JavaSEPort decomposition (the landable part)

This is largely independent of the native backend and could be merged on its own.

- `Ports/JavaSE/src/com/codename1/impl/CodenameOneImplementationDecorator.java`
  — generated full-forwarding decorator over `CodenameOneImplementation`
  (~620 overrides). Generator: `scripts/javase/generate-impl-decorator.sh`.
  Drift guard: `maven/javase/src/test/java/com/codename1/impl/DecoratorCoverageTest.java`
  fails the build when core gains a method.
- `Ports/JavaSE/src/com/codename1/impl/javase/simulator/` — extracted SPI +
  tool proxies (network monitor, network conditions, performance monitor,
  location) + chrome host/backend skeleton.
- Gated behind `cn1.simulator.decorators` (set by `Simulator.main`); identity
  wrap is the parity-risk step — verify with the screenshot suite on/off.

## File map

```
maven/cn1-sim-native/
  host/src/com/codename1/impl/ios/sim/
    CN1PureSimulator.java        — entry point; boots shell + app universes
    CN1SimHost.java              — native input dispatch + surface lifecycle
    IsolatedAppRunner.java       — boots the user app in its own universe; routePointer
    shell/SimulatorShell.java    — the entire simulator chrome as CN1 UI (menus,
                                    sidebar tools, inspector, network monitor, etc.)
    child/BridgedSimImplementation.java — app-universe impl; overrides base methods,
                                    mirrors JavaSEPort behaviors (getLargerTextScale,
                                    getBiometrics, getInAppPurchase, inspector, ...)
    child/SimBiometrics.java, SimPurchase.java — simulated native services
    bridge/BridgeRegistry.java   — shared statics rendezvous (see above)
    bridge/{ToolsBridge,ChildControl,RenderBridge,InputSink,...}.java — bridge ifaces
    rpc/RpcRenderBridge.java     — the socket protocol (ops + events)
  generator/  — JNI shim generator (ParparMangler, ShimEmitter, symbol scanner)
  runtime/    — cn1jni runtime (ParparVM C API over JNIEnv): arrays, strings, arenas
  macos/      — CN1SimViewController.m, CN1SimWindow.m, uikit compat shim
  target/     — BUILD OUTPUT (gitignored): host jars, classes, libcn1sim.dylib

scripts/cn1-sim-native/
  build-host.sh                  — builds the JVM host jar. MUST run with JDK 17
                                    (JAVA_HOME=$(/usr/libexec/java_home -v 17)) or the
                                    jar is class 69 and won't load on the sim runtime.
  build-libcn1sim-mac.sh, build-cn1sim-windows.sh, generate-*-shims.sh, build-win-host.sh

scripts/hellocodenameone/common/src/main/java/
  com/codenameone/examples/hellocodenameone/SimRelayService.java — the relay app
  com/codename1/ui/SimRelayGfx.java — relay-side graphics access shim

Ports/iOSPort/nativeSources/ (modified for the sim/Catalyst path)
  CodenameOne_GLAppDelegate.m — Mac window control, solid titlebar, auto-termination
                                opt-out, and (newest) trackpad/wheel scroll recognizer
  CN1Metalcompat.{h,m}, METALView.m, DrawImage.m, IOSNative.m, CN1Camera.m
Ports/iOSPort/src/com/codename1/impl/ios/{IOSImplementation,IOSNative}.java
CodenameOne/src/com/codename1/impl/CodenameOneImplementation.java — base no-op
  setMacWindowAlwaysOnTop/ContentSize hooks
maven/codenameone-maven-plugin/.../MacNativeBuilder.java + scripts/build-mac-native-app.sh
scripts/sign-notarize-mac-app.sh — sign/notarize the relay .app for distribution
Ports/JavaSE/src/*.res — bundled native themes for the shell (iOSModernTheme, etc.)
```

## How to build & run (from this branch, on this Mac)

```bash
# 1. Host jar (JVM side) — JDK 17 is mandatory
JAVA_HOME=$(/usr/libexec/java_home -v 17) bash scripts/cn1-sim-native/build-host.sh

# 2. Relay (Catalyst app) — only needed when nativeSources/*.m change.
#    See /tmp/build-relay-r26.sh in the working notes; it runs
#    scripts/build-mac-native-app.sh then xcodebuild into /tmp/relay-ddNN, and
#    patches NSSupportsAutomaticTermination=false. Uses a LOCAL maven repo
#    (-Dmaven.repo.local=...) so other builds don't pollute 8.0-SNAPSHOT.

# 3. Launch: JVM listens on 17995, relay dials in. See /tmp/run-relay-dd40.sh:
#    java -Dcn1.sim.rpc=17995 -Dcn1.sim.shell=true -Dcn1.sim.skin=...GooglePixel.skin \
#         -cp <CN1+JavaSE+CLDC11+host.jar+appclasses> \
#         com.codename1.impl.ios.sim.CN1PureSimulator <AppMainClass>
#    then: open SimRelayService.app

# Verify a frame (reliable capture):
swift /tmp/winid.swift                 # prints the window id
screencapture -l<id> /tmp/shot.png
```

## What works (verified in this spike)

- Boots the user app in an isolated universe, rendered natively via the relay's
  single Metal surface; solid dark titlebar; resizable window; survives
  backgrounding.
- Simulator chrome as CN1 UI: Device/Simulate/Tools/Help native menu bar
  (mirrors PR #5211), sidebar tool sections.
- JavaSEPort parity features demoed: Larger Text (Dynamic Type scales),
  Biometric Simulation, In-App Purchase, Share (text/URL/file), Network Monitor,
  Performance Monitor, Test Recorder scaffold, native camera capture, native
  text editing, **interactive Component Inspector** (tree → click highlights the
  live component with a glass-pane overlay + property readout).
- Most recent polish (this session):
  - Sidebar font size bump (`sidebarFontPx` 26 → 31).
  - **Editable inspector** — UIID/Text detail lines are TextFields; committing
    pushes `inspectSet:<prop>=<value>` to the app universe → `setUIID`/`setText`
    + revalidate + re-send detail. Verified the field opens the native editor and
    is bound to the live component; the keystroke-commit couldn't be driven by
    synthetic input but the path is simple correct code.
  - **Native trackpad / mouse-wheel scrolling** — `UIPanGestureRecognizer`
    (`allowedScrollTypesMask = All`, `maximumNumberOfTouches = 0`, indirect-only)
    in `CodenameOne_GLAppDelegate.m`, gated `#if TARGET_OS_MACCATALYST`,
    synthesizes CN1 drags at the cursor. Needs a real trackpad to fully confirm.

## Known issues / rough edges (where to resume)

- **Sidebar resize-coordinate desync (biggest UX blocker).** Toggling a tool on
  does not widen the window — the skin squeezes — and shell input coordinates
  drift from the render, so tree rows become hard to click. `Device > Fit Window
  to Skin` re-syncs coordinates but then triggers a stale post-resize repaint
  (skin blanks until the next interaction). Unify window/Display/surface dims so
  the sidebar toggle resizes correctly and repaints in one shot. (Pending tasks
  "Full-bleed canvas / unify dims" and "scroll/drag framebuffer corruption".)
- **Native Catalyst text editor + synthetic input.** The relay's native
  UITextField overlay ignores scripted keystrokes/Return and *cancels* on
  defocus (only Return/Done commits). Fine for humans, blocks scripted E2E tests.
- **Scroll wheel** needs human-trackpad confirmation (cliclick can't synthesize
  indirect-scroll gestures).
- **JNI backend (Workstream B)** is scaffolded but the object-lifetime/GC bridge
  (per-downcall ref arenas + audited pins, `-Xcheck:jni`) is the unproven core;
  the RPC pivot was chosen to avoid it for now.
- Intermittent boot hang (~1/3) at `op=11 getRGB`; restart clears it.

## The decision to pause

This is a large, ambitious effort whose biggest near-term wins (the JavaSEPort
decorator/tool-proxy decomposition in Workstream A) are separable from the
native backend. Whether to continue — and whether RPC or JNI is the right
backend — is a strategic call for later. Everything needed to resume is on this
branch plus the project memories (see `project_sim_*` and `reference_sim_*`).
```

The full original plan lives at (outside the repo):
  ~/.claude/plans/codename-one-has-ports-tidy-starlight.md
and is summarized above. Session memories carry the operational landmines.
```
