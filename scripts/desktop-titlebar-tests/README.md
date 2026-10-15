# Native desktop title-area checks

These harnesses compile production native code and run **locally only**. Do not
add them to CI, dispatch native-test jobs, or use the banned `native-test` label.
They do not replace native application acceptance on each supported desktop.

From the repository root:

```sh
python3 scripts/desktop-titlebar-tests/check.py macos
python3 scripts/desktop-titlebar-tests/check.py linux
python3 scripts/desktop-titlebar-tests/check.py windows
```

Use `--output /path/to/a/new/unique-directory` to retain binaries and Linux
screenshots. Without it, the runner creates and removes a unique temporary
directory. No Maven repository is used by these harnesses.

* **macOS:** requires Xcode command-line tools and AppKit. Checks actual native
  window-button geometry, backing scale, copied metric snapshots, and hidden
  controls, asynchronous change notifications (without repeat notifications for
  unchanged geometry), and handled/unhandled Dock reopen decisions without launching a document editor.
* **Linux:** requires GCC, GTK 3 and Pango development headers, `pkg-config`,
  `xvfb-run`, Openbox, and `xdotool`. Checks left/right system decoration layouts,
  title/body coordinates, interactive exclusions, touch ownership across caption/control
  boundaries and cancellation, native-peer click/focus/typing and reparenting,
  double-click maximize, mode switching, and the real GTK close button. This is
  X11 validation; acceptance under a Wayland compositor remains separate.
* **Windows:** requires Clang, an x86_64 MinGW sysroot/linker, and Wine. Compiles
  the production chrome, window, and menu sources. Checks hit testing, system
  commands, resizing regions, DWM margin refresh on DPI changes with event
  fallthrough, and the native-frame fallback. Use `--build-only`
  to copy the executable to a separate Windows/Wine environment. Wine without
  DWM cannot verify caption rendering or Windows 11 Snap hover behavior; the
  executable prints this limitation explicitly. Test these on Windows with
  composition enabled before claiming native visual acceptance.

The Linux and Windows harnesses include the production native implementation
and supply only the Java bridge/environment pieces that the harness doesn't
execute. The macOS harness extracts the production inset routines and supplies
a minimal host backed by a real `NSWindow`.

The portable Toolbar layout regressions live in `DesktopChromeTest` in
`maven/core-unittests`. They cover additive physical insets, RTL, changes to
system geometry, restored application padding, and mobile/legacy behavior.

`WindowsDpiTest` in `maven/windows` checks DPI conversion and density updates before
requesting layout refresh, including unchanged DPI and ordinary appearance events.
It does not replace a physical multi-monitor Windows check.
