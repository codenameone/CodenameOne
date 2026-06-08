#import "com_codenameone_examples_purchasetest_PurchaseTestSinkImpl.h"

// Persist submitted receipt transactionIds in NSUserDefaults so the hosted
// XCTest (PurchaseStoreKitTests) can read them back in-process after driving a
// purchase through SKTestSession. Key is shared with the test.
static NSString * const CN1IAPTestSubmittedKey = @"CN1IAPTestSubmittedReceipts";

@implementation com_codenameone_examples_purchasetest_PurchaseTestSinkImpl

-(void)recordSubmittedReceipt:(NSString*)transactionId {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSArray *existing = [defaults arrayForKey:CN1IAPTestSubmittedKey];
    NSMutableArray *updated = existing ? [existing mutableCopy] : [NSMutableArray array];
    [updated addObject:(transactionId != nil ? transactionId : @"<null>")];
    [defaults setObject:updated forKey:CN1IAPTestSubmittedKey];
    [defaults synchronize];
}

-(NSString*)recordedSubmissions {
    NSArray *existing = [[NSUserDefaults standardUserDefaults] arrayForKey:CN1IAPTestSubmittedKey];
    return existing ? [existing componentsJoinedByString:@","] : @"";
}

-(void)reset {
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:CN1IAPTestSubmittedKey];
    [[NSUserDefaults standardUserDefaults] synchronize];
}

-(BOOL)isSupported {
    return YES;
}

@end
