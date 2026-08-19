# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Codename One is a cross-platform mobile development framework that compiles Java/Kotlin bytecode to native OS executables for iOS, Android, and other platforms. The repository includes:

- **Core framework** (`CodenameOne/src/`) - The main UI framework and APIs
- **ParparVM** (`vm/`) - iOS VM that translates Java bytecode to C code for native iOS compilation
- **Platform ports** (`Ports/`, `maven/android/`, `maven/ios/`) - Platform-specific implementations
- **Build tools** (`maven/codenameone-maven-plugin/`) - Maven plugin for building apps
- **Designer** (`CodenameOneDesigner/`) - Visual design tool
- **Tests** (`tests/`) - Test suites and samples

## Build System

The project is transitioning from Ant to Maven. **Maven is the preferred build system.**

### Building from Source

**Requirements:**
- JDK 8 (required for the core framework build)
- JDK 11 through 25 (required at *runtime* for the simulator and "Run as desktop app")
- JDK 17 (required for Android port)
- Apache Maven 3.6+
- macOS with Xcode (for iOS port only)

**Quick Start:**

```bash
# Setup workspace (downloads JDKs, builds core, installs archetypes)
./scripts/setup-workspace.sh -DskipTests
source tools/env.sh

# Build everything
cd maven
mvn install -Plocal-dev-javase
```

**Build Individual Components:**

```bash
# Core modules only
cd maven
mvn install -Plocal-dev-javase -DskipTests

# Android port (requires JAVA17_HOME set)
./scripts/build-android-port.sh -DskipTests

# iOS port (macOS only)
./scripts/build-ios-port.sh -DskipTests
```

**Important Build Notes:**
- The `-Plocal-dev-javase` profile is necessary for building the javase port
- Artifacts are installed to local Maven repository at `~/.m2/repository`
- The build requires `cn1-binaries` repository (automatically cloned to `../cn1-binaries` by setup script)
- Build client is installed to `~/.codenameone/CodeNameOneBuildClient.jar`

### Testing

```bash
# Run JavaSE unit tests (Ant)
ant test-javase

# Run Maven tests
cd maven
mvn test -Plocal-dev-javase

# Run samples application
ant samples
```

### Legacy Ant Build

While Maven is preferred, Ant builds are still supported:

```bash
ant                    # Build core
ant core              # Build Codename One core
ant ios               # Build iOS port
ant android           # Build Android port
ant javase            # Build JavaSE port
ant test-javase       # Run tests
ant samples           # Launch sample runner
```

## Project Architecture

### Core Framework Structure

The framework is organized into these main packages under `CodenameOne/src/com/codename1/`:

- **`ui/`** - UI components, layouts, and rendering
- **`io/`** - Networking, storage, and I/O operations
- **`components/`** - High-level UI components (e.g., SpanLabel, InfiniteProgress)
- **`charts/`** - Charting library
- **`maps/`** - Mapping support
- **`util/`** - Utilities (e.g., StringUtil, MathUtil)
- **`l10n/`** - Localization support
- **`impl/`** - Platform implementation interfaces
- **`db/`** - Database APIs
- **`push/`** - Push notification support
- **`media/`** - Media playback
- **`properties/`** - Property binding framework

### ParparVM (iOS Translation)

Located in `vm/`, ParparVM is Codename One's iOS VM that translates Java bytecode to C code:

- **`ByteCodeTranslator/`** - Translates bytecode to C
- **`JavaAPI/`** - Minimal Java runtime for iOS

**Key characteristics:**
- Translates Java bytecode → C code → native iOS binary via Xcode
- Concurrent garbage collector (non-blocking)
- Generates standard Xcode projects
- No JNI overhead - direct C code invocation
- Targets Java 5 with Java 8 syntax via retrolambda

**Build output:** Valid Xcode project that can be opened, debugged, and profiled with native tools.

### Maven Module Structure

- **`core/`** - Framework core (compiled with `-source 1.5 -target 1.5`)
- **`factory/`** - Factory interfaces for platform implementations
- **`javase/`** - JavaSE simulator port
- **`javase-svg/`** - SVG support for JavaSE
- **`android/`** - Android port
- **`ios/`** - iOS port resources
- **`parparvm/`** - ParparVM resources
- **`designer/`** - Visual designer tool
- **`codenameone-maven-plugin/`** - Maven build plugin
- **`sqlite-jdbc/`** - SQLite support
- **`java-runtime/`** - Java runtime utilities

### Platform Implementations

Each platform provides implementation of interfaces in `com.codename1.impl`:

- **JavaSE** (`Ports/JavaSE/`) - Desktop simulator
- **Android** (`maven/android/`) - Android native implementation
- **iOS** (`maven/ios/`, `Ports/iOSPort/`) - iOS native implementation via ParparVM

## Development Workflow

### Creating a Test Project

Use the Codename One initializr to generate a Maven project:
```bash
# Visit https://start.codenameone.com
# Or use Maven archetypes after setup-workspace.sh
```

To use locally-built version, edit the generated `pom.xml`:
```xml
<properties>
    <cn1.version>8.0-SNAPSHOT</cn1.version>
    <cn1.plugin.version>8.0-SNAPSHOT</cn1.plugin.version>
</properties>
```

### Java Version Constraints

- **Core framework**: Must use Java 5 source/target for backward compatibility
- **Tooling/Plugins**: Can use Java 8+
- **Tests**: Can use Java 11+
- **Android build**: Requires JDK 17 in JAVA17_HOME
- **Main JAVA_HOME (for building the framework)**: Must be JDK 8
- **Runtime JDK for simulator / desktop run**: JDK 11 through 25 is supported. The Codename One Maven plugin checks this on entry to `cn1:run` and `cn1:debug` and aborts with a friendly error when an older JDK is in use. The build-time goals (`generate-desktop-app-wrapper`, `prepare-simulator-classpath`, the `executable-jar` profile) are not gated -- they still work on JDK 8 because they only generate icons / classpath metadata.

### Static Analysis Gates

PR CI (`.github/workflows/pr.yml`, Java 8 leg) runs SpotBugs over
`core-unittests`, `android`, `ios`, `ByteCodeTranslator` and
`codenameone-maven-plugin`, then enforces the result in
`.github/scripts/generate-quality-report.py`.

- **SpotBugs is a zero-findings gate.** *Any* finding of *any* pattern in *any*
  of those projects fails the build, and a project that produces no SpotBugs
  report at all fails it too (see `QUALITY_REPORT_REQUIRED_SPOTBUGS`). There is
  no per-pattern allow-list, so a pattern nobody anticipated still fails.
- **Record intentional exceptions in the project's `spotbugs-exclude.xml`**
  (`maven/core-unittests/`, `Ports/Android/`, `Ports/iOSPort/`,
  `vm/ByteCodeTranslator/`, `maven/codenameone-maven-plugin/`), scoped to the
  class or method it applies to and with a comment explaining why. Keep the
  generated report at zero rather than tolerating known noise.
- PMD and Checkstyle still gate on their own lists in the same script.

To reproduce the SpotBugs gate locally:

```bash
source tools/env.sh   # JDK 8
cd maven && mvn -B -DskipTests=true -Pcompile-android \
  -pl android,ios,codenameone-maven-plugin -am verify
mvn -B -DunitTests -DskipTests=true -pl core-unittests verify
mvn -B -DskipTests=true -f ../vm/ByteCodeTranslator/pom.xml verify
```

**Run the `core-unittests` line too, and do not skip it because the first
command "already covered" that module -- it does not.** `core-unittests` is
declared inside the `unittests` profile, so `-am` never reaches it and its
`target/spotbugsXml.xml` is left exactly as some earlier run wrote it. Reading
that file after the first command reports whatever was true last time, which is
how a real finding was declared clean locally and then failed `build-test (8)`.
Delete the report before re-running if you want to be certain you are reading
this run's answer.

Note the module analyses the **core** classes, not just its own tests: a
finding there names a `com.codename1.*` framework class, and adding a caller or
removing one can make a previously-used private method dead.

Findings land in each module's `target/spotbugsXml.xml`.

### Never rely on ClassCastException

**ParparVM's `CHECKCAST` is unchecked.** `BC_CHECKCAST` expands to nothing and the
optimizer drops the instruction, so a failed cast does *not* throw on iOS -- the
wrong object is handed to the next instruction and the target type's fields get
read out of it. That is a native crash no Java `catch` can see (issue #5531).

Never write a cast whose failure you expect to handle:

```java
// WRONG -- the handler never runs on iOS
try { return Double.parseDouble((String) o); } catch (Exception e) { return def; }

// RIGHT -- works on every platform
if (o instanceof Number) { return ((Number) o).doubleValue(); }
```

`scripts/check-cast-semantics.sh` enforces this in PR CI (Java 8 leg). It reports
a `CHECKCAST` inside a `try` whose handler catches `ClassCastException` or a
supertype, and holds the result against `scripts/cast-semantics-baseline.txt` --
a ratchet of pre-existing debt, not an allow-list. **New code must not add
entries**; delete one when you fix the method. An `instanceof`-guarded cast is
recognized and never reported. Note the rule is about the *cast*: a
`catch (ClassCastException)` with no cast under it is fine, because an
*explicitly thrown* ClassCastException propagates normally.

Run it locally (needs core/android/ios/JavaAPI built; unbuilt modules are skipped
with a note):

```bash
source tools/env.sh
scripts/check-cast-semantics.sh
scripts/check-cast-semantics.sh --write-baseline   # after fixing a method
```

### Working with Native Code

Platform-specific native code locations:
- **iOS**: `Ports/iOSPort/nativeSources/` (Objective-C)
- **Android**: Within Android port module (Java/Kotlin)
- **JavaSE**: Within JavaSE port (Java)

#### ParparVM native names are checked, and getting one wrong is silent

ParparVM encodes the **whole Java signature in the C function name**. For
`boolean isBiometricsSupported()` on `com.codename1.impl.ios.IOSNative`:

```c
JAVA_BOOLEAN com_codename1_impl_ios_IOSNative_isBiometricsSupported___R_boolean(
        CODENAME_ONE_THREAD_STATE, JAVA_OBJECT __cn1ThisObject)
```

Three rules, all easy to get wrong by hand:

- `__` opens the argument list and **every argument adds its own leading `_`**, so
  a no-arg method ends in `__` and a one-int method ends in `___int` -- three
  underscores.
- A non-void return appends **`_R` plus that same per-type `_`**: `_R_boolean`,
  `_R_java_lang_String`. Omitting it still compiles.
- Array dimensions collapse to one token: `byte[][]` is `byte_2ARRAY`, never
  `byte_1ARRAY_1ARRAY`.
- Instance methods take `JAVA_OBJECT` after the thread state; static ones do not.

**Neither the compiler nor the linker catches a mistake here.** A misspelled name
is just a different function, so it compiles and links; the correctly named symbol
is then absent, and since a native method is kept alive *by* its symbol appearing
in the native sources (`BytecodeMethod.isMethodUsedByNative`), the dead-code pass
reads that absence as "unused" and drops the Java method. The build is green and
the feature is inert on the device. A right name with a wrong *prototype* is worse:
C links on the name alone, so it runs and reads its arguments out of the wrong
registers.

`NativeSignatureVerifier` gates this, in two places -- **both of them ours, neither
of them on by default in a customer build**:

- **A translation** (iOS, native Windows, native Linux) verifies the generated
  project -- app, cn1libs and the native-interface glue the builders inject -- and
  fails before emitting any C. This is **opt-in**: it does nothing unless
  `CN1_NATIVE_VERIFY` or `-Dparparvm.nativeVerify` says `strict` or `warn`. Making
  the old soft failure hard would change the outcome of app builds that succeed
  today, so our CI opts in (`CN1_NATIVE_VERIFY: strict` at workflow level in
  `pr.yml`, `parparvm-tests.yml` and `parparvm-tests-windows.yml`, inherited by
  every forked translation) and nobody else has to. The `nativeVerify` build hint
  turns it on for a single build. With the check on, the per-symbol opt-out for a
  native that lives in a prebuilt `.a`/`.framework` is
  `cn1-native-verify-ignore.txt` beside the native sources.
- **PR CI** runs the same verifier offline over our own ports, which needs no
  device build. This half is a CI tool rather than part of any build, so it is
  always strict:

```bash
source tools/env.sh
scripts/check-native-signatures.sh          # skips ports that are not built
scripts/check-native-signatures.sh --require-all   # what CI runs
```

Findings come in three kinds. `MISSING` and `SIGNATURE` fail the build. `ORPHAN`
-- a C function whose name is a near miss for one of ours and that nothing else
calls -- is a warning: it is dead code, not a broken build. Note the offline gate
reads `target/classes`, so **a stale port build reports natives that no longer
exist**; rebuild the module before believing a finding.

### Integration Tests

Located in `maven/integration-tests/`:
```bash
cd maven/integration-tests
./cn1app-archetype-test.sh      # Test archetype generation
./android-native-interface-test.sh  # Test Android native interfaces
```

## Important Files and Locations

- **`maven/pom.xml`** - Root Maven POM, defines all modules and dependencies
- **`maven/CodeNameOneBuildClient.jar`** - Build client (copied to `~/.codenameone/`)
- **`scripts/setup-workspace.sh`** - Initial workspace setup script
- **`scripts/build-android-port.sh`** - Android port build script
- **`scripts/build-ios-port.sh`** - iOS port build script
- **`tools/env.sh`** - Environment variables (created by setup-workspace.sh)
- **`BUILDING.md`** - Detailed build instructions
- **`README.md`** - Project overview and getting started

## Common Patterns

### Resource Files

Resources are managed via `.res` files:
- `CodenameOne/src/CN1Resource.res` - Default resources
- Edit with Designer tool or programmatically

### Theme and Styling

- CSS-based styling supported
- Material Design font included: `CodenameOne/src/material-design-font.ttf`
- Theme files are part of `.res` resources

### Version Management

Version is centrally managed in `maven/pom.xml`:
```bash
# Update version
cd maven
bash update-version.sh 8.0.1
```

## Deployment and Release

### Maven Central Deployment

See `maven/README.adoc` for full process. Summary:
1. Update to release version: `bash update-version.sh X.Y.Z`
2. Push tags (triggers GitHub Actions workflow)
3. Update to next SNAPSHOT: `bash update-version.sh X.Y.Z+1-SNAPSHOT`
4. Close and release staging repository on Sonatype

### Build Server

Codename One uses build servers for cloud builds. The build client (`CodeNameOneBuildClient.jar`) communicates with these servers for Android/iOS builds when not building locally.

## Debugging and Troubleshooting

### Simulator

The JavaSE port serves as the simulator with:
- Fast startup (no emulator overhead)
- Live code reload support
- CSS live updates
- Component inspector
- Network monitor
- Interactive Groovy console

### Native Debugging

- **iOS**: Open generated Xcode project, use Xcode debugger and Instruments
- **Android**: Standard Android debugging via Android Studio

### Common Issues

- **JDK version mismatch**: Ensure JAVA_HOME is JDK 8 for building the framework, JAVA17_HOME is JDK 17 for the Android port
- **`Unrecognized option: --add-exports=...`** when running the simulator or desktop app: the project is being executed on a JDK older than 11. Switch to JDK 11 through 25 (Eclipse Temurin from <https://adoptium.net>) and re-run. The Codename One Maven plugin now detects this and prints a friendly error before the JVM is forked.
- **Missing cn1-binaries**: Run `setup-workspace.sh` or manually clone to `../cn1-binaries`
- **Build client missing**: Copy `maven/CodeNameOneBuildClient.jar` to `~/.codenameone/`
- **macOS ARM JDK8**: Setup script downloads x64 version (works via Rosetta)

## Contributing

- Discuss changes in [discussion forum](https://www.codenameone.com/discussion-forum.html) or [Stack Overflow](http://stackoverflow.com/tags/codenameone)
- File clear, concise issues with test cases
- JavaDoc editable directly in source
- Developer guide wiki: https://github.com/codenameone/CodenameOne/wiki/
- By contributing, you grant Codename One shared ownership of your work

## Additional Resources

- Main site: https://www.codenameone.com
- JavaDoc: https://www.codenameone.com/javadoc/
- Developer Guide: https://www.codenameone.com/manual/
- Maven Manual: https://shannah.github.io/codenameone-maven-manual/
- Build from source blog: https://www.codenameone.com/blog/building-codename-one-from-source-maven-edition.html
