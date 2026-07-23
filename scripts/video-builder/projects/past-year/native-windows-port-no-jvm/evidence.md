# Evidence map

Source: `docs/website/content/blog/native-windows-port-no-jvm.md`
Canonical: https://www.codenameone.com/blog/native-windows-port-no-jvm/

## Thesis

Compiling Java to a small native Win32 executable through ParparVM

## Supported beats

- **How this differs from GraalVM:** GraalVM native-image is a remarkable piece of engineering and the comparison is worth making precisely, because it is not "GraalVM ships a JVM". It doesn't, and it doesn't support reflection everywhere either; it compiles ahead of time against a closed world, much like we do.
- **A real rendering stack, not a port of a port:** The desktop details that make an app feel native are in place too. Mouse-wheel scrolling maps onto Codename One's own tensile scrolling physics through a new shared core API (the JavaSE simulator was refactored onto the same code path, so every desktop port scrolls identically).
- **How complete is it?:** The port is new, and it should be treated as such; we expect to be shooting down teething issues over the coming weeks, and we want to hear about every one of them.
- **Building one:** A regular build returns x64 and arm64 release executables; set the windows.debug build hint for a single x64 debug build. The build runs on our Linux build servers, which cross-compile the Windows PE directly, and if you're on a Windows machine with the toolchain installed, local-windows-device does the same build locally.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5144
- https://github.com/codenameone/CodenameOne/pull/5209
