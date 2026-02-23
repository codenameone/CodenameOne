# iOS Native Notification Tests

This directory contains native XCTest assets used by CI and local scripts to
validate local-notification behavior using the standard Xcode test runner.

## Files

- `native-tests/LocalNotificationBehaviorTests.m`
  - XCTest cases that verify duplicate identifier replacement, cancel-by-id,
    and `__ios_id__` userInfo persistence.
- `install-native-notification-tests.sh`
  - Copies test sources into generated Xcode project and wires them into the
    `HelloCodenameOneTests` target.

## Related Runner

- `scripts/run-ios-native-tests.sh`
  - Calls `install-native-notification-tests.sh` and runs `xcodebuild test`
    for the generated iOS workspace on simulator.
