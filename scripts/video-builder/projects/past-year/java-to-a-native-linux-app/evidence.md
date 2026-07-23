# Evidence map

Source: `docs/website/content/blog/java-to-a-native-linux-app.md`
Canonical: https://www.codenameone.com/blog/java-to-a-native-linux-app/

## Thesis

Compiling Java to a self-contained Linux ELF with no target JVM

## Supported beats

- **What renders it:** The native layer is real, not a set of stubs: 173 native methods across window, graphics, text, image, networking, sockets, services, text editing, browser, media, peers, GL, and printing.
- **The hard part is not rendering:** Rendering was the easy part. The two genuinely hard parts of shipping a Linux desktop binary are packaging and dependencies, and they are worth explaining because they shaped the whole design.
- **Building one:** Most projects build Linux the way they build every other port: on the build cloud, with nothing to install locally. New projects from the Initializr already carry the Linux build configuration, so the target is there from the first build.
- **Native Linux versus the executable jar:** Before this port, the way to run a Codename One app on Linux was to package it as an executable jar that runs on a JVM. That path still exists and still works.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/issues

## Independent problem evidence

- glibc ABI List: https://sourceware.org/glibc/wiki/ABIList — The glibc project tracks exported symbol versions because binaries rely on the C library ABI available on the target system.
- Zig Cross Compilation: https://ziglang.org/learn/overview/#cross-compiling-is-a-first-class-use-case — Zig's build tooling can select architecture, operating system, and ABI so one host can produce binaries for another Linux target.

## Product proof

- `docs/website/static/blog/java-to-a-native-linux-app/chatview-linux.png`
- `docs/website/static/blog/java-to-a-native-linux-app/gltf-3d-linux.png`
- `docs/website/static/blog/java-to-a-native-linux-app/css-gradients-linux.png`
