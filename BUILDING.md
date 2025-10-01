# Building Codename One

This guide explains how to build Codename One from source using Maven. It provides reproducible steps to compile the core framework and its Android and iOS ports.

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
export JAVA_HOME=$PWD/jdk-8.0.28+6

# JDK 17 (Linux x64; adjust `_x64_linux_` for your platform)
curl -L -o temurin17.tar.gz https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.16%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.16_8.tar.gz
tar xf temurin17.tar.gz
export JAVA17_HOME=$PWD/jdk-17.0.16+8

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

The script clones the [cn1-binaries repository](https://github.com/codenameone/cn1-binaries) locally to a directory next to the current directory.

It copies the `maven/CodeNameOneBuildClient.jar` file into the `~/.codenameone` directory.

It then executes the following while JDK 8 is defined as `JAVA_HOME` and is first in the path:

```bash
mvn" -f maven/pom.xml -DskipTests -Djava.awt.headless=true -Dcn1.binaries="$CN1_BINARIES" -Dcodename1.platform=javase -P local-dev-javase,compile-android install
```

Note that it's important that `$CN1_BINARIES` points at the locally cloned [cn1-binaries repository](https://github.com/codenameone/cn1-binaries) and is an absolute path, not a relative path.

## Building the Android port

The Android port uses JDK 8 as well. However, it needs a `JAVA17_HOME` environment variable that points at JDK 17.

```bash
./scripts/build-android-port.sh -DskipTests
```

Artifacts are placed in `maven/android/target`.

Internally this executes:

```bash
mvn -q -f maven/pom.xml -pl android -am -Dmaven.javadoc.skip=true -Djava.awt.headless=true clean install
```

## Building the iOS port

JDK 8 isn't commonly available for Mac OS ARM machines. As a workaround we use the x86 version of the JDK even on ARM.

The iOS port can only be built on macOS with Xcode installed. Run the iOS script:

```bash
./scripts/build-ios-port.sh -DskipTests
```

Artifacts are produced in `maven/ios/target`.

Internally this executes:

```bash
mvn -q -f maven/pom.xml -pl ios -am -Djava.awt.headless=true clean install
```

## Convenience scripts

- `setup-workspace.sh` – installs Maven, downloads JDK 8 and JDK 17 to a temporary directory, builds the core modules, installs Maven archetypes, provisions the Codename One build client, and writes `tools/env.sh`.
- `build-android-port.sh` – builds the Android port using JDK 11 for Maven and JDK 17 for compilation.
- `build-ios-port.sh` – builds the iOS port on macOS with JDK 11.

## Further reading

- Blog post: <https://www.codenameone.com/blog/building-codename-one-from-source-maven-edition.html>
- Maven developers guide: <https://shannah.github.io/codenameone-maven-manual/>
