# iOS Native Purchase Tests (StoreKitTest)

Native XCTest assets that validate the In-App-Purchase / ReceiptStore stack end
to end on the iOS simulator using Apple's **StoreKitTest** framework
(`SKTestSession`) -- a *simulated* App Store, no sandbox account and no network.

## How it works

1. The sample app (`HelloCodenameOne`) references `com.codename1.payment.*`, so
   the build's bytecode scanner flips `IPhoneBuilder.usesPurchaseAPI`, which
   defines `CN1_USE_STOREKIT` and links `StoreKit.framework`. The CN1 StoreKit
   observer (`paymentQueue:updatedTransactions:`) is therefore compiled in and
   registered at runtime.
2. At startup the app installs `RecordingReceiptStore`, which forwards every
   submitted receipt's `transactionId` through the `PurchaseTestSink` native
   interface; the iOS implementation persists it in `NSUserDefaults`.
3. The hosted XCTest (`PurchaseStoreKitTests`) creates an `SKTestSession` from
   `Products.storekit`, buys a product, and the purchase flows through the real
   `SKPaymentQueue` into the CN1 observer -> generated `Purchase.postReceipt`
   -> receipt-sync engine -> the installed `RecordingReceiptStore`.
4. The test reads back the `NSUserDefaults` sink (same process, hosted test) and
   asserts the receipt was submitted.

This is the iOS-level guard for issue #5186: the observer submits through a
freshly-constructed `Purchase` instance, so a recorded submission proves the
store installed on a *different* instance at startup was visible to it (the
shared/static `receiptStore`).

## Files

- `native-tests/PurchaseStoreKitTests.m` -- the hosted StoreKitTest XCTest.
- `Products.storekit` -- StoreKit configuration (one consumable,
  `com.codenameone.hello.pro`).
- `install-native-purchase-tests.sh` -- copies the test sources +
  `Products.storekit` into the generated Xcode project, configures the test
  target as hosted, and links `StoreKit` + `StoreKitTest`.

## Related runner

- `scripts/run-ios-purchase-tests.sh` -- installs the assets and runs
  `xcodebuild test` on a simulator.

## Sample-app wiring (committed)

- `scripts/hellocodenameone/common/.../PurchaseTestSink.java` -- native iface.
- `scripts/hellocodenameone/common/.../RecordingReceiptStore.java` -- the store.
- `scripts/hellocodenameone/ios/src/main/objectivec/...PurchaseTestSinkImpl.m`
  -- iOS sink (NSUserDefaults); javase/android impls record in memory.
- `HelloCodenameOne.kt` `init()` references `Purchase` and installs the store.
