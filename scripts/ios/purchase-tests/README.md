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

## StoreKit 1 and StoreKit 2

A build that uses the payment API and sets `ios.storeKit2=true` gets the
StoreKit 2 bridge (`CN1StoreKit2.swift`, injected by `StoreKit2Injector` from
`maven/build-engine/.../builders/storekit/CN1StoreKit2.swifts`), and on iOS 15+
`CN1StoreKit.m` routes every purchase through it instead of `SKPaymentQueue`.
Without the hint StoreKit 1 runs everywhere.

`CN1_IAP_STOREKIT` (default `1`) picks the mode `run-ios-purchase-tests.sh`
expects; `purchase-e2e.yml` runs both, the `2` leg with the hint set:

- `PurchaseStoreKitTests` runs in both modes and checks the build really is the
  one asked for: the `CN1StoreKit2` class is linked in mode 2 only, and the
  receipt's order data is a JWS in mode 2 and the app receipt in mode 1.
- `PurchaseStoreKit2Tests` (Swift, iOS 17+) runs in mode 2 only. It drives the
  app through `CN1IAPTestCommands` and reads `CN1IAPTestEvents` back: product
  lookup with an unknown id, a non-consumable, a subscription and its renewal
  (delivered through `Transaction.updates` alone), a store failure, Ask to Buy,
  a refund, restore, exactly-once delivery, and a transaction whose signature
  does not verify. Every transaction must also end up finished.

Two things the hosted test cannot cover, so they are not in it: a relaunch with
an unfinished transaction (the app process is the test's), and promotional
offers (they need a signature from a server key).

### The scheme needs the StoreKit configuration

`attach-storekit-config.py` puts `Products.storekit` on the test scheme's Test
and Launch actions. Without it StoreKit 2 settles on the sandbox when the app
starts `Transaction.updates` at launch -- before any test creates its
`SKTestSession` -- and every product request comes back empty (the log says
"Error updating Storefront ... environment: Sandbox"). StoreKit 1 does not
notice, which is why its test passed without it.

`SKTestSession.buyProduct` is no stand-in for a purchase outside the app: it
runs in the app's own process, StoreKit counts it as the app's purchase, and
`Transaction.updates` never emits it. A forced subscription renewal does reach
updates.

Locally, run on a simulator of your own (`CN1_IAP_SIMULATOR_ID`). StoreKitTest
on a simulator another session is driving fails with
`SKInternalErrorDomain Code=3` before the app is involved.

## Files

- `native-tests/PurchaseStoreKitTests.m` -- the hosted StoreKitTest XCTest.
- `native-tests/PurchaseStoreKit2Tests.swift` -- the StoreKit 2 lifecycle test.
- `Products.storekit` -- StoreKit configuration: consumables
  (`com.codenameone.hello.pro` among them), the non-consumable
  `com.codenameone.hello.lifetime` and the monthly subscription
  `com.codenameone.hello.monthly`.
- `attach-storekit-config.py` -- puts that configuration on the shared schemes.
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
