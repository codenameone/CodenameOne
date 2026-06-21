# Tools

This directory contains local build-time helpers used to prepare the
playground runtime.

`generate-cn1-access-registry.sh` runs a Java tool that scans Codename One
sources and emits a hardcoded CN1 access registry for BeanShell. By default it
scans **release source jars** for the configured `cn1.version` in
`scripts/cn1playground/pom.xml`. The registry is no longer pinned one release
behind — the playground now ships the local ParparVM `local-javascript` build,
so the registry tracks the current API directly.

You can force behavior with:

- `CN1_ACCESS_USE_LOCAL_SOURCES=true`  (scan local repo sources instead)
- `CN1_ACCESS_USE_LOCAL_SOURCES=false` (default; scan release source jars)

When the build runs against the local workspace (`-Dcn1.localWorkspace=true`),
the `cn1-local-workspace` Maven profile sets `cn1.accessRegistry.useLocalSources`
to `true`, which the `common` module passes through as
`CN1_ACCESS_USE_LOCAL_SOURCES=true` so the registry is generated from the repo's
own CN1 sources (the 8.0-SNAPSHOT has no source jars on Maven Central).

`run-playground-smoke-tests.sh` now regenerates the registry in release mode
and asserts a key set of `com.codename1.ui.*` classes (including `Component`,
`Form`, `Container`, `Dialog`, `Display`, etc.) exist in the generated output
before running the Java smoke harness. It also asserts known internal classes
(`com.codename1.ui.Accessor`, `com.codename1.io.IOAccessor`) are not emitted.

`compare-javascript-bundles.sh` compares a legacy JavaScript playground output
with a ParparVM JavaScript-port artifact and reports total bundle size plus key
payload file sizes. This is intended as the first regression tool for keeping
the new port in the same size scale before hard thresholds are added.
