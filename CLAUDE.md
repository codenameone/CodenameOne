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
- JDK 8 (required for core build)
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
- **Main JAVA_HOME**: Must be JDK 8

### Working with Native Code

Platform-specific native code locations:
- **iOS**: `Ports/iOSPort/nativeSources/` (Objective-C)
- **Android**: Within Android port module (Java/Kotlin)
- **JavaSE**: Within JavaSE port (Java)

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

- **JDK version mismatch**: Ensure JAVA_HOME is JDK 8, JAVA17_HOME is JDK 17
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
