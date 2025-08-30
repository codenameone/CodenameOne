# Building Codename One

This guide describes how to build Codename One and its Android and iOS ports locally using Maven. It includes reproducible steps for setting up the workspace and compiling each port.

## Prerequisites

- **JDK 11** for building the main project and the iOS port.
- **JDK 17** for building the Android port.
- **Apache Maven 3.6+**.

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
```

The script runs `mvn install` in `maven/`, installs `cn1-maven-archetypes`, and ensures `~/.codenameone/CodeNameOneBuildClient.jar` is installed by invoking the `cn1:install-codenameone` Maven goal. If that goal fails, the script copies the jar from `maven/CodeNameOneBuildClient.jar`.

## Building the Android port

Run the Android build script. It requires `JAVA_HOME` pointing to JDK 11 and `JAVA_HOME_17` pointing to JDK 17. If these variables are unset, the script will look for the JDKs under `tools/` as provisioned by `setup-workspace.sh`:

```bash
./scripts/build-android-port.sh -DskipTests
```

The resulting artifacts are placed in `maven/android/target`.

## Building the iOS port

The iOS port can only be built on macOS with Xcode installed. The build script expects `JAVA_HOME` to point to JDK 11 and will search `tools/` if it is not set:

```bash
./scripts/build-ios-port.sh -DskipTests
```

The build output is in `maven/ios/target`.

## Convenience scripts

The `scripts` directory contains helper scripts:

- `setup-workspace.sh` – installs Maven, downloads JDK 11 and JDK 17, builds the core modules, installs Maven archetypes, and provisions the Codename One build client.
- `build-android-port.sh` – builds the Android port using JDK 11 for Maven and JDK 17 for compilation.
- `build-ios-port.sh` – builds the iOS port on macOS with JDK 11.

These scripts accept additional Maven arguments, which are passed through to the underlying `mvn` commands.

## Further reading

- Blog post: *Building Codename One from source, Maven edition* – https://www.codenameone.com/blog/building-codename-one-from-source-maven-edition.html
- Maven developers guide – https://shannah.github.io/codenameone-maven-manual/
