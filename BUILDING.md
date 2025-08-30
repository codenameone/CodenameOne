# Building Codename One

This guide explains how to build Codename One and its Android and iOS ports locally with Maven.

## Prerequisites

- **JDK 8**
- **JDK 17** for building the Android port
- **Apache Maven 3.6+**
- macOS with Xcode (required only for the iOS port)

The helper scripts in the `scripts/` directory download these dependencies when they are not already installed.

### Installing JDKs on Linux

Download binaries from [Adoptium](https://adoptium.net):

```bash
# JDK 8 (Linux x64; adjust `_x64_linux_` for your platform)
curl -L -o temurin8.tar.gz https://github.com/adoptium/temurin8-binaries/releases/download/jdk8u462-b08/OpenJDK8U-jdk_x64_linux_hotspot_8u462b08.tar.gz
tar xf temurin8.tar.gz
export JAVA_HOME=$PWD/jdk8u462-b08

# JDK 17 (Linux x64; adjust `_x64_linux_` for your platform)
curl -L -o temurin17.tar.gz https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.16%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.16_8.tar.gz
tar xf temurin17.tar.gz
export JAVA_HOME_17=$PWD/jdk-17.0.16+8

export PATH="$JAVA_HOME/bin:$PATH"
```

## Preparing the workspace

Clone the repository and run the setup script to download JDK 8 and JDK 17, install Maven, build the core modules, and install the Maven archetypes. This step must be performed before building any ports.

```bash
git clone https://github.com/codenameone/CodenameOne
cd CodenameOne
./scripts/setup-workspace.sh -DskipTests
source tools/env.sh
```

The script runs `mvn install` in `maven/`, installs `cn1-maven-archetypes`, and ensures `~/.codenameone/CodeNameOneBuildClient.jar` is installed by invoking the `cn1:install-codenameone` Maven goal. If that goal fails, the script copies the jar from `maven/CodeNameOneBuildClient.jar`. After the script finishes, `tools/env.sh` contains environment variables for the provisioned JDKs and Maven.

## Building the Android port

The Android port uses JDK 17 for compilation while Maven runs with JDK 8. Run the build script:

```bash
./scripts/build-android-port.sh -DskipTests
```

Artifacts are placed in `maven/android/target`.

## Building the iOS port

The iOS port can only be built on macOS with Xcode installed. Run the iOS script:

```bash
./scripts/build-ios-port.sh -DskipTests
```

Artifacts are produced in `maven/ios/target`.

## Convenience scripts

- `setup-workspace.sh` – installs Maven, downloads JDK 8 and JDK 17, builds the core modules, installs Maven archetypes, provisions the Codename One build client, and writes `tools/env.sh`.
- `build-android-port.sh` – builds the Android port using JDK 8 for Maven and JDK 17 for compilation.
- `build-ios-port.sh` – builds the iOS port on macOS with JDK 8.

## Further reading

- Blog post: <https://www.codenameone.com/blog/building-codename-one-from-source-maven-edition.html>
- Maven developers guide: <https://shannah.github.io/codenameone-maven-manual/>
