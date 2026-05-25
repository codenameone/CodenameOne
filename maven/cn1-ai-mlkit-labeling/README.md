# cn1-ai-mlkit-labeling

Codename One AI cn1lib -- **ML Kit Image Labeling**.

## Status

Scaffold. The public Java surface is in place so apps can compile
against it today, but every method currently completes with
`UnsupportedOperationException("not yet implemented")` until the
per-platform native bridges land.

## Build-time wiring

The Codename One build server's `AiDependencyTable` recognises
references to `com.codename1.ai.mlkit.labeling.*` and automatically injects the right
iOS Pod / Swift Package, Android Gradle dependency, and any
required `Info.plist` usage descriptions or Android permissions.
See `AiDependencyTable.java` in the core repo for the full
mapping.
