# Build and Run Reference

## Required JDKs

| Task | JDK |
| --- | --- |
| Compiling the `common` module (Java 17 default) | JDK 17+ available as `JAVA_HOME` |
| Compiling the `common` module (legacy Java 8 setting) | JDK 8+, source/target=1.8 |
| Running `cn1:run` / `cn1:debug` (the simulator) | **JDK 11–25** (CN1 plugin aborts on older JDKs with a friendly error) |
| Cross-compiling for Android natively | JDK 17 set in `JAVA17_HOME` |
| Cross-compiling for iOS | macOS + Xcode + JDK 11–25 |

The simulator forks a JVM that needs the `--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED` argument on macOS / JDK 17+. The initializr already wires this into `.idea/workspace.xml` "Run in Simulator" run configurations.

## Maven goal cheat sheet

All goals come from the `codenameone-maven-plugin` and live under the `cn1:` prefix. Run them from the project root unless noted.

```bash
# Desktop simulator (live, with hot CSS reload)
mvn -pl common cn1:run

# Debug from your IDE
mvn -pl common cn1:debug

# Run the CN1 test runner (NOT surefire)
mvn -pl common cn1:test

# Run as a real desktop app (.jar wrapped with bundled JRE)
mvn -pl javase package -Pexecutable-jar

# Cross-build for a target platform via the CN1 build server
mvn -pl android    package -Dcodename1.platform=android    -Dcodename1.buildTarget=android-device
mvn -pl ios        package -Dcodename1.platform=ios        -Dcodename1.buildTarget=ios-device
mvn -pl ios        package -Dcodename1.platform=ios        -Dcodename1.buildTarget=ios-source
mvn -pl javascript package -Dcodename1.platform=javascript -Dcodename1.buildTarget=javascript

# Compile CSS to theme.res without running the app
mvn -pl common compile          # CSS is compiled in the process-resources phase
```

`-Dcodename1.buildTarget=ios-source` is special: it does not require build server credentials and instead generates an Xcode project locally that you can build and debug yourself in Xcode.

## `codenameone_settings.properties` — the configuration source of truth

This file lives at `common/codenameone_settings.properties`. The most useful keys:

```properties
codename1.packageName=com.example.myapp
codename1.mainName=MyAppName
codename1.displayName=My App Name
codename1.arg.java.version=17                 # 17 = use Java 17 build server; remove for Java 8
codename1.arg.android.googlePlayVersion=true
codename1.arg.ios.includePush=false
codename1.kotlin=false
codename1.arg.android.xPermissions=...
codename1.arg.ios.deployment_target=11.0
codename1.arg.ios.teamId=ABCDEF1234           # used by the locally-generated Xcode project
codename1.arg.build.compile=true              # ahead-of-time compile (recommended for iOS)
```

Anything prefixed `codename1.arg.<platform>.<key>` is forwarded to the build server's build args. The full list is in the Codename One developer guide.

## Layout invariants

- `common/src/main/css/theme.css` — **the** CSS file. Compiled to `common/target/classes/theme.res`.
- `common/src/main/l10n/messages*.properties` — i18n bundles. **MUST** live here (not in `src/main/resources`), or the CSS compiler will not bake them in.
- `common/src/main/java/<pkg>/<MainClass>.java` — entry point. Class name must match `codename1.mainName`.
- `common/src/main/resources/` — arbitrary runtime resources. Loaded via `Display.getInstance().getResourceAsStream("/file.json")`.
- `common/src/main/guibuilder/` — optional GUI builder XML (auto-generates Java).

## Pointing the project at a local Codename One snapshot

When iterating on the CN1 framework itself, point the generated project at a local SNAPSHOT instead of Maven Central:

```xml
<properties>
    <cn1.version>8.0-SNAPSHOT</cn1.version>
    <cn1.plugin.version>8.0-SNAPSHOT</cn1.plugin.version>
</properties>
```

Then build the framework from `/path/to/CodenameOne/maven` with `mvn install -Plocal-dev-javase`. The generated project will resolve the SNAPSHOT from `~/.m2/repository`.

## Common build errors and fixes

- **`Unrecognized option: --add-exports=...` when running the simulator** → the runtime JDK is < 11. Switch to JDK 11–25.
- **`Resources.getGlobalResources().getL10N(...)` returns null at runtime** → your bundles are under `common/src/main/resources/` instead of `common/src/main/l10n/`. Move them.
- **`Cannot find symbol class XxxView`** after using the GUI builder → run `mvn -pl common generate-sources` to regenerate.
- **`OutOfMemoryError` running `cn1:test`** → bump `<argLine>-Xmx2g</argLine>` in the surefire/cn1 plugin config in `common/pom.xml`.
- **Build server returns "build args invalid"** → check `codename1.arg.*` keys spelled exactly as documented in the Codename One Developer Guide.

## The build client

The CN1 Maven plugin ships a build client jar that is copied to `~/.codenameone/CodeNameOneBuildClient.jar` on first use. If you ever see "build client missing", copy it manually from `maven/CodeNameOneBuildClient.jar` of the framework checkout.
