---
name: ios-cn1ss-xcode-debug
description: Reproduce and debug HelloCodenameOne iOS CN1SS build/test failures on a Mac using local ios-source generation plus xcodebuild logs.
---

# iOS CN1SS Xcode Debug

Use this skill when CN1SS iOS runs fail, produce blank screenshots, or crash during build/test.
This workflow is validated to compile and run locally against repository snapshots.

## Preconditions

- macOS with Xcode CLI tools installed (`xcodebuild` available).
- Java 8 available locally.
- Repository root checked out.

## Fast Workflow

1. Set Java 8 for Maven runs.
2. Install CN1 iOS artifacts from this repo into `~/.m2`.
3. Force-regenerate iOS local source for `scripts/hellocodenameone` so local snapshots are actually copied.
4. Run `xcodebuild` on the generated workspace.
5. Run the full CN1SS runner and inspect screenshot/log artifacts.
6. Extract first compile/runtime/test failure from logs.

## Commands

Run from repo root unless noted.

```bash
export JAVA_HOME=/Users/shai/Library/Java/JavaVirtualMachines/azul-1.8.0_372/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
```

```bash
cd maven
mvn -q -pl ios -am -DskipTests -Dmaven.javadoc.skip=true install
```

```bash
cd ../scripts/hellocodenameone
rm -rf ios/target/hellocodenameone-ios-1.0-SNAPSHOT-ios-source
./mvnw -q package -DskipTests -Dcodename1.platform=ios -Dcodename1.buildTarget=ios-source -o -e
```

```bash
cd ios/target/hellocodenameone-ios-1.0-SNAPSHOT-ios-source
xcodebuild -list -workspace HelloCodenameOne.xcworkspace
xcodebuild -workspace HelloCodenameOne.xcworkspace -scheme HelloCodenameOne -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build
```

For full logs plus focused errors:

```bash
xcodebuild -workspace HelloCodenameOne.xcworkspace -scheme HelloCodenameOne -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build > /tmp/hellocn1-xcodebuild.log 2>&1
rg -n "error:|fatal error:|undeclared function|BUILD FAILED|test host" /tmp/hellocn1-xcodebuild.log
```

Run the full UI test harness:

```bash
XCODE_APP=/Applications/Xcode.app \
scripts/run-ios-ui-tests.sh \
scripts/hellocodenameone/ios/target/hellocodenameone-ios-1.0-SNAPSHOT-ios-source/HelloCodenameOne.xcworkspace \
HelloCodenameOne
```

Post-run quick checks:

```bash
ls -1 artifacts/*.png | wc -l
shasum artifacts/*.png | sort | uniq -c | sort -nr | head
rg -n "CN1SS:ERR|CN1SS:SUITE:FINISHED" artifacts/device-runner.log
```

## Common Failure Signatures

- `Could not find test host for HelloCodenameOneTests`
  - The generated unit-test scheme host path is invalid. Prefer building/running `HelloCodenameOne` scheme directly for CN1SS troubleshooting.

- `call to undeclared function 'virtual_com_codename1_...'`
  - Common cause: static invocation of an instance API in app/test code.
  - Verify generated symbols in `HelloCodenameOne-src/com_codename1_*.m` and match call style.
  - Reinstall local CN1 maven artifacts from current repo (`maven -pl ios -am`) and regenerate ios-source.
  - Re-generate `ios-source` after reinstall.

- Build succeeds but screenshots are blank or repetitive
  - Check CN1SS log markers in app/test output:
    - `CN1SS:INFO:test=... png_bytes=...`
    - `CN1SS:ERR:...`
  - If PNG bytes are tiny or absent, investigate app startup and current form readiness (`Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot` path).
  - If many screenshots share the same hash, the runner is capturing a stable frame repeatedly. Check test navigation/state transitions before capture.
  - If tests appear hung and simulator shows system permission alerts, this is usually not a screenshot pipeline bug; unblock/disable the modal first.

- `Scheme file not found for env injection ... xcschemes/*.xcscheme`
  - The runner can continue, but scheme-based env injection is skipped.
  - Inspect `artifacts/device-runner.log` and decoded screenshots directly.

## Artifacts To Inspect

- Generated iOS project:
  - `scripts/hellocodenameone/ios/target/hellocodenameone-ios-1.0-SNAPSHOT-ios-source`
- Build log:
  - `/tmp/hellocn1-xcodebuild.log`
- CN1SS device log:
  - `artifacts/device-runner.log`
- Decoded screenshots:
  - `artifacts/*.png`
- Xcode derived data for this project:
  - `~/Library/Developer/Xcode/DerivedData/HelloCodenameOne-*`

## Notes

- `scripts/hellocodenameone/ios/pom.xml` includes Objective-C files from `src/main/objectivec` as resources; headers should only include imports that are available in generated project context.
- If a native interface exists in common, platform impl classes should exist for every built platform variant used by generated stubs (at least android/ios/javase in this project).

## Lessons Learned (Local Notifications + White Screenshots Incident)

- Keep only stable fixes in core code:
  - Temporary diagnostics (`NSLog`, extra CN1SS/CN1SHOT channels, timeout tweaks) are useful for triage but should be removed once root cause is proven.
- Propagation is a common trap:
  - Rebuilding `maven/ios` is not enough if generated `ios-source` is stale.
  - Always delete `scripts/hellocodenameone/ios/target/...-ios-source` before regeneration when validating port/native changes.
- Simulator behavior differs from device behavior:
  - Startup notification authorization can block CN1SS runs with an iOS modal.
  - A correct simulator-safe fix is to avoid notification authorization prompts on simulator startup paths.
- Stale SpringBoard state can mask fixes:
  - After changing notification authorization behavior, erase the simulator before re-testing:
    - `xcrun simctl shutdown <udid>`
    - `xcrun simctl erase <udid>`
- Validate assumptions with direct evidence:
  - Capture live simulator screenshots while tests run.
  - Query logs directly with `simctl log show` for app process markers to confirm which code path actually executed.
