# Building Codename One

This document provides reproducible instructions for building Codename One and its Android and iOS ports with Maven. It is written so that both developers and automated tools can follow it step by step.

## Prerequisites

- **JDK 11** (Codename One also builds with JDK 8, but these instructions use JDK 11).
- **JDK 17** for building the Android port.
- **Apache Maven 3.6+**.
- macOS with Xcode (only for building the iOS port).

The `setup-workspace.sh` script downloads these dependencies automatically when they are not already installed.

### Installing JDKs on Linux

Download binaries from [Adoptium](https://adoptium.net):

```bash
# JDK 11 (Linux x64; adjust `_x64_linux_` for your OS and architecture)
curl -L -o temurin11.tar.gz https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.28%2B6/OpenJDK11U-jdk_x64_linux_hotspot_11.0.28_6.tar.gz
tar xf temurin11.tar.gz
export JAVA_HOME=$PWD/jdk-11*

# JDK 17 (Linux x64; adjust `_x64_linux_` for your OS and architecture)
curl -L -o temurin17.tar.gz https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.16%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.16_8.tar.gz
tar xf temurin17.tar.gz
export JAVA_HOME_17=$PWD/jdk-17*

export PATH="$JAVA_HOME/bin:$PATH"
```

The Android port uses JDK 17 while Maven runs with JDK 11. The helper scripts download both JDKs and set `JAVA_HOME` and `JAVA_HOME_17`.

## Preparing the workspace

Clone the repository and run the setup script to install Maven, download JDK 11 and JDK 17, build the core modules, and install the Maven archetypes. This step must be performed before building any ports.

```bash
git clone https://github.com/codenameone/CodenameOne
cd CodenameOne
./scripts/setup-workspace.sh -DskipTests
source tools/env.sh
```

The script runs `mvn install` in `maven/`, installs `cn1-maven-archetypes`, and ensures `~/.codenameone/CodeNameOneBuildClient.jar` is installed by invoking the `cn1:install-codenameone` Maven goal. If that goal fails, the script copies the jar from `maven/CodeNameOneBuildClient.jar`. After the script finishes, `tools/env.sh` contains environment variables for the provisioned JDKs and Maven.

## Building the Android port

Run the Android build script. It sources the environment from `tools/env.sh`, ensuring `JAVA_HOME` points to JDK 11 and `JAVA_HOME_17` points to JDK 17. If the JDKs are missing, the script will run `setup-workspace.sh` to download them:

```bash
./scripts/build-android-port.sh -DskipTests
```

The resulting artifacts are placed in `maven/android/target`.

## Building the iOS port

The iOS port can only be built on macOS with Xcode installed. The build script sources `tools/env.sh` and ensures `JAVA_HOME` points to JDK 11, running `setup-workspace.sh` if necessary:

```bash
./scripts/build-ios-port.sh -DskipTests
```

The build output is in `maven/ios/target`.

## Convenience scripts

The `scripts` directory contains helper scripts:

- `setup-workspace.sh` – installs Maven, downloads JDK 11 and JDK 17, builds the core modules, installs Maven archetypes, provisions the Codename One build client, and writes `tools/env.sh`.
- `build-android-port.sh` – builds the Android port using JDK 11 for Maven and JDK 17 for compilation.
- `build-ios-port.sh` – builds the iOS port on macOS with JDK 11.

These scripts accept additional Maven arguments, which are passed through to the underlying `mvn` commands.

## Further reading

- Blog post: *Building Codename One from source, Maven edition* – https://www.codenameone.com/blog/building-codename-one-from-source-maven-edition.html
- Maven developers guide – https://shannah.github.io/codenameone-maven-manual/
