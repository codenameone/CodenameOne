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
