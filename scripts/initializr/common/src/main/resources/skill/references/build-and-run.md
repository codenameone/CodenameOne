# Build and Run Reference

## Required JDKs

| Task | JDK |
| --- | --- |
| Compiling the `common` module | JDK 17+ available as `JAVA_HOME` |
| Running `cn1:run` / `cn1:debug` (the simulator) | **JDK 17–25** (CN1 plugin aborts on older JDKs with a friendly error) |
| Cross-compiling for Android natively | JDK 17 in `JAVA17_HOME` |
| Cross-compiling for iOS locally | macOS only — see Xcode prerequisites below |

`codename1.arg.java.version=17` in `codenameone_settings.properties` selects the **build server's** JDK 17 toolchain. Keep it set even when you upgrade your local JDK (21, 25, …) — the local JDK only runs Maven and the simulator; the property routes the cloud-build target.

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

## Automated (headless) builds for CI / LLM workflows

Add `-Dautomated=true` to any `package` goal that targets the build server. This tells the CN1 Maven plugin not to prompt for credentials or open a browser, makes it return a non-zero exit code on build failure, and downloads `result.zip` directly into `target/`:

```bash
mvn -pl ios     package -Dcodename1.platform=ios     -Dcodename1.buildTarget=ios-device     -Dautomated=true
mvn -pl android package -Dcodename1.platform=android -Dcodename1.buildTarget=android-device -Dautomated=true
```

Credentials must already be set (Enterprise CN1 accounts include API tokens). The non-interactive options:

- Provide `-Duser=<email> -Dtoken=<api-token>` on the command line.
- Or run `mvn cn1:set-user-token -Duser=<email> -Dtoken=<token>` once on the build machine; the credentials are then stored in Java preferences for the user.

This is the right entry point for CI/CD pipelines, LLM agents that need to compile native artifacts, and unattended nightly builds.

## Local iOS builds (`ios-source`) prerequisites

`-Dcodename1.buildTarget=ios-source` skips the build server and emits an Xcode project under `ios/target/codenameone/ios/dist/<App>-src/`. You can open it in Xcode and produce an `.ipa` locally. Requirements:

| Requirement | Notes |
| --- | --- |
| macOS | Apple silicon or Intel; no other OS can build iOS apps. |
| Xcode | Latest stable from the App Store. Older Xcodes lag behind App Store submission requirements; if you intend to submit, use the version currently mandated by Apple. |
| Xcode command-line tools | `xcode-select --install`. Required for `xcodebuild` and the iOS Simulator. |
| CocoaPods | `sudo gem install cocoapods` if your project (or any cn1lib) declares `codename1.arg.ios.pods`. |
| Apple developer account + signing | Set `codename1.arg.ios.teamId=ABCDEF1234` in `codenameone_settings.properties` so the generated Xcode project signs with your team automatically. |
| Deployment target | Set `codename1.arg.ios.deployment_target=14.0` (or current minimum) to match Apple's accepted submission range. |

After the project is generated, you can also run `xcodebuild -workspace <App>.xcworkspace -scheme <App> -configuration Release archive` from CI to produce an archive without opening Xcode interactively.

## `codenameone_settings.properties` — the configuration source of truth

This file lives at `common/codenameone_settings.properties`. The most useful keys:

```properties
codename1.packageName=com.example.myapp
codename1.mainName=MyAppName
codename1.displayName=My App Name
codename1.arg.java.version=17                 # Required: routes the build to the Java 17 build server
codename1.arg.android.googlePlayVersion=true
codename1.arg.ios.includePush=false
codename1.kotlin=false
codename1.arg.android.xPermissions=...
codename1.arg.ios.deployment_target=14.0
codename1.arg.ios.teamId=ABCDEF1234
codename1.arg.build.compile=true              # ahead-of-time compile (recommended for iOS)
```

Anything prefixed `codename1.arg.<platform>.<key>` is forwarded to the build server. See [`build-hints.md`](build-hints.md) for the curated index of build hints. The complete reference is in the Codename One Developer Guide at <https://www.codenameone.com/manual/>.

## Layout invariants

- `common/src/main/css/theme.css` — **the** CSS file. Compiled to `common/target/classes/theme.res`.
- `common/src/main/l10n/messages*.properties` — i18n bundles. **MUST** live here (not in `src/main/resources`), or the CSS compiler will not bake them in.
- `common/src/main/java/<pkg>/<MainClass>.java` — entry point. Class name must match `codename1.mainName`.
- `common/src/main/resources/` — arbitrary runtime resources. Loaded via `Display.getInstance().getResourceAsStream("/file.json")`.
- `common/src/main/guibuilder/` — optional GUI builder XML (auto-generates Java).

## Common build errors and fixes

- **`Resources.getGlobalResources().getL10N(...)` returns null at runtime** → your bundles are under `common/src/main/resources/` instead of `common/src/main/l10n/`. Move them.
- **`Cannot find symbol class XxxView`** after using the GUI builder → run `mvn -pl common generate-sources` to regenerate.
- **`OutOfMemoryError` running `cn1:test`** → bump `<argLine>-Xmx2g</argLine>` in the surefire/cn1 plugin config in `common/pom.xml`.
- **Build server returns "build args invalid"** → check `codename1.arg.*` keys against [`build-hints.md`](build-hints.md) or the Developer Guide.
- **`-Dautomated=true` exits 0 even though the build failed** → make sure you're on a recent `cn1.plugin.version`; older versions of the plugin swallowed errors.

## The build client jar

The build server protocol is driven by `CodeNameOneBuildClient.jar`. The Codename One Maven plugin places it under `~/.codenameone/CodeNameOneBuildClient.jar` automatically the first time you run a `cn1:build` / `package` goal. If it ever goes missing, download the canonical copy:

<https://github.com/codenameone/CodenameOne/raw/refs/heads/master/maven/CodeNameOneBuildClient.jar>

Put it at `~/.codenameone/CodeNameOneBuildClient.jar`.

## Reference links

- Codename One Developer Guide: <https://www.codenameone.com/manual/>
- JavaDoc: <https://www.codenameone.com/javadoc/>
- Maven plugin manual: <https://shannah.github.io/codenameone-maven-manual/>
- Build hints curated index: [`build-hints.md`](build-hints.md)
