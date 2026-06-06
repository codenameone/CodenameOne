#import <XCTest/XCTest.h>
#import <StoreKit/StoreKit.h>

// StoreKitTest (Xcode 12+/iOS 14+) lets us drive a *simulated* App Store
// purchase locally on the simulator -- no sandbox account, no network. The
// purchase flows through the real SKPaymentQueue, so Codename One's actual
// StoreKit observer (paymentQueue:updatedTransactions: in
// CodenameOne_GLViewController.m) fires, calls the generated
// Purchase.postReceipt(...), and the receipt-sync engine submits it to the
// ReceiptStore the sample app installed at startup (RecordingReceiptStore).
// That store forwards the transactionId to the PurchaseTestSink native
// interface, whose iOS implementation persists it in NSUserDefaults -- which
// this hosted XCTest reads back in-process to assert the end-to-end path.
//
// This is the iOS-level guard for issue #5186: the observer submits through a
// freshly-constructed Purchase instance, so a recorded submission proves the
// store installed on a different instance at startup was visible to it.
#if __has_include(<StoreKitTest/StoreKitTest.h>)
#import <StoreKitTest/StoreKitTest.h>
#define CN1_HAS_STOREKIT_TEST 1

// SKTestSession's synchronous buy selector is buyProductWithIdentifier:error:
// (verified against the simulator runtime). StoreKitTest ships no umbrella
// header in the SDK, so declare it to dispatch correctly without a warning.
@interface SKTestSession (CN1PurchaseTest)
- (BOOL)buyProductWithIdentifier:(NSString *)productIdentifier error:(NSError **)error;
@end
#endif

static NSString * const kProductId = @"com.codenameone.hello.pro";
static NSString * const kSinkKey = @"CN1IAPTestSubmittedReceipts";

@interface PurchaseStoreKitTests : XCTestCase
@end

@implementation PurchaseStoreKitTests {
#ifdef CN1_HAS_STOREKIT_TEST
    SKTestSession *_session;
#endif
}

- (void)setUp {
    [super setUp];
    self.continueAfterFailure = NO;
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:kSinkKey];
    [[NSUserDefaults standardUserDefaults] synchronize];
}

- (void)tearDown {
#ifdef CN1_HAS_STOREKIT_TEST
    if (@available(iOS 14.0, *)) {
        [_session clearTransactions];
    }
    _session = nil;
#endif
    [super tearDown];
}

- (NSString *)recordedSubmissions {
    NSArray *recorded = [[NSUserDefaults standardUserDefaults] arrayForKey:kSinkKey];
    return recorded ? [recorded componentsJoinedByString:@","] : @"";
}

// Spin the run loop until `predicate` is true or the timeout elapses. The CN1
// VM boots and registers the StoreKit observer asynchronously, so we poll
// rather than assume immediate delivery.
- (BOOL)waitUntil:(BOOL (^)(void))predicate timeout:(NSTimeInterval)timeout {
    NSDate *deadline = [NSDate dateWithTimeIntervalSinceNow:timeout];
    while ([deadline timeIntervalSinceNow] > 0) {
        if (predicate()) {
            return YES;
        }
        [[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode
                                 beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.25]];
    }
    return predicate();
}

- (void)testStoreKitPurchaseReachesReceiptStore {
#ifndef CN1_HAS_STOREKIT_TEST
    XCTSkip("StoreKitTest framework unavailable in this SDK");
#else
    if (@available(iOS 14.0, *)) {
        NSError *error = nil;
        _session = [[SKTestSession alloc] initWithConfigurationFileNamed:@"Products" error:&error];
        XCTAssertNotNil(_session, @"Failed to load Products.storekit configuration: %@", error);
        _session.disableDialogs = YES;
        _session.askToBuyEnabled = NO;
        [_session clearTransactions];

        // Give the CN1 VM a moment to boot and register the SKPaymentQueue
        // observer. Even if the buy lands first, StoreKit re-delivers the
        // queued transaction once the observer attaches, so this is just to
        // reduce flake.
        [self waitUntil:^BOOL{ return NO; } timeout:5.0];

        NSError *buyError = nil;
        BOOL bought = [_session buyProductWithIdentifier:kProductId error:&buyError];
        XCTAssertTrue(bought, @"buyProductWithIdentifier failed: %@", buyError);

        BOOL submitted = [self waitUntil:^BOOL{
            return [[NSUserDefaults standardUserDefaults] arrayForKey:kSinkKey].count > 0;
        } timeout:60.0];

        XCTAssertTrue(submitted,
            @"No receipt was submitted to the ReceiptStore within the timeout. "
            @"Recorded submissions: '%@'. This means the StoreKit purchase did not "
            @"flow through the CN1 observer into the receipt-sync engine and the "
            @"installed ReceiptStore.", [self recordedSubmissions]);
    } else {
        XCTSkip("StoreKitTest requires iOS 14+");
    }
#endif
}

@end
