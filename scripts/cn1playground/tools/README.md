# Tools

This directory contains local build-time helpers used to prepare the
playground runtime.

`generate-cn1-access-registry.sh` resolves Codename One *source jars* for the
configured `cn1.version` in `scripts/cn1playground/pom.xml`, then runs a Java
tool that scans those sources and emits a hardcoded CN1 access registry for
BeanShell.
