# cn1-ai-mlkit-docscan

Codename One AI cn1lib -- **ML Kit Document Scanner**.

## Status

- Public Java facade: complete (`com.codename1.ai.mlkit.docscan.DocumentScanner`)
- `NativeDocumentScanner` platform interface: defined
- iOS / Android native bridges: **pending follow-up commits**
  (need device-testable bindings against the underlying SDK)

Apps can reference `DocumentScanner.*` today; every call returns
an `AsyncResource` that completes with a clear `LlmException`
until the platform bridges land. The build server's
`AiDependencyTable` already injects the right CocoaPod / Swift
Package / Android Gradle dep / `Info.plist` strings / Android
permissions on `import`.

## Versioning and release

This module inherits its version (`${cn1.version}`) from the
Codename One parent pom. It's part of the Codename One reactor
and ships to Maven Central alongside every Codename One
release; the dependency on `codenameone-core` is `provided`
scope so consuming apps see the cn1lib pull in core at the
version the app already declares via `cn1.version`.
