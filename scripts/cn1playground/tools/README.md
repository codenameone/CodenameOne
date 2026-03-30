# Tools

This directory contains local build-time helpers used to prepare the
playground runtime.

`generate-cn1-access-registry.sh` scans **release source jars** for the
configured `cn1.version` in `scripts/cn1playground/pom.xml` by default, then
runs a Java tool that scans those sources and emits a hardcoded CN1 access
registry for BeanShell.

You can force behavior with:

- `CN1_ACCESS_USE_LOCAL_SOURCES=true`  (scan local repo sources instead)
- `CN1_ACCESS_USE_LOCAL_SOURCES=false` (default; scan release source jars)

`run-playground-smoke-tests.sh` now regenerates the registry in release mode
and asserts a key set of `com.codename1.ui.*` classes (including `Component`,
`Form`, `Container`, `Dialog`, `Display`, etc.) exist in the generated output
before running the Java smoke harness. It also asserts known internal classes
(`com.codename1.ui.Accessor`, `com.codename1.io.IOAccessor`) are not emitted.
