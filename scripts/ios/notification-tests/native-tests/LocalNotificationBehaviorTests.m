#import <XCTest/XCTest.h>
#import <UserNotifications/UserNotifications.h>
#import <objc/runtime.h>

#import "com_codenameone_examples_hellocodenameone_LocalNotificationNativeImpl.h"

@interface CN1FakeNotificationCenter : NSObject
@property(nonatomic, strong) NSMutableDictionary<NSString *, UNNotificationRequest *> *pending;
@property(nonatomic, strong) NSMutableSet<NSString *> *deliveredRemoved;
+ (instancetype)shared;
- (void)reset;
- (void)addNotificationRequest:(UNNotificationRequest *)request withCompletionHandler:(void (^)(NSError * _Nullable error))completionHandler;
- (void)getPendingNotificationRequestsWithCompletionHandler:(void (^)(NSArray<UNNotificationRequest *> *requests))completionHandler;
- (void)removePendingNotificationRequestsWithIdentifiers:(NSArray<NSString *> *)identifiers;
- (void)removeDeliveredNotificationsWithIdentifiers:(NSArray<NSString *> *)identifiers;
- (void)removeAllPendingNotificationRequests;
- (void)removeAllDeliveredNotifications;
@end

@implementation CN1FakeNotificationCenter

+ (instancetype)shared {
    static CN1FakeNotificationCenter *center;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        center = [CN1FakeNotificationCenter new];
        center.pending = [NSMutableDictionary dictionary];
        center.deliveredRemoved = [NSMutableSet set];
    });
    return center;
}

- (void)reset {
    [self.pending removeAllObjects];
    [self.deliveredRemoved removeAllObjects];
}

- (void)addNotificationRequest:(UNNotificationRequest *)request withCompletionHandler:(void (^)(NSError * _Nullable error))completionHandler {
    self.pending[request.identifier] = request;
    if (completionHandler) {
        completionHandler(nil);
    }
}

- (void)getPendingNotificationRequestsWithCompletionHandler:(void (^)(NSArray<UNNotificationRequest *> *requests))completionHandler {
    if (completionHandler) {
        completionHandler(self.pending.allValues);
    }
}

- (void)removePendingNotificationRequestsWithIdentifiers:(NSArray<NSString *> *)identifiers {
    for (NSString *identifier in identifiers) {
        [self.pending removeObjectForKey:identifier];
    }
}

- (void)removeDeliveredNotificationsWithIdentifiers:(NSArray<NSString *> *)identifiers {
    for (NSString *identifier in identifiers) {
        [self.deliveredRemoved addObject:identifier];
    }
}

- (void)removeAllPendingNotificationRequests {
    [self.pending removeAllObjects];
}

- (void)removeAllDeliveredNotifications {
    [self.deliveredRemoved removeAllObjects];
}

@end

static IMP gOriginalCurrentCenterIMP = nil;

static id cn1FakeCurrentNotificationCenter(id self, SEL _cmd) {
    (void)self;
    (void)_cmd;
    return [CN1FakeNotificationCenter shared];
}

@interface LocalNotificationBehaviorTests : XCTestCase
@end

@implementation LocalNotificationBehaviorTests

+ (void)setUp {
    [super setUp];
    Method classMethod = class_getClassMethod([UNUserNotificationCenter class], @selector(currentNotificationCenter));
    gOriginalCurrentCenterIMP = method_setImplementation(classMethod, (IMP)cn1FakeCurrentNotificationCenter);
}

+ (void)tearDown {
    if (gOriginalCurrentCenterIMP != nil) {
        Method classMethod = class_getClassMethod([UNUserNotificationCenter class], @selector(currentNotificationCenter));
        method_setImplementation(classMethod, gOriginalCurrentCenterIMP);
        gOriginalCurrentCenterIMP = nil;
    }
    [super tearDown];
}

- (void)setUp {
    [super setUp];
    self.continueAfterFailure = NO;
    [[CN1FakeNotificationCenter shared] reset];
}

- (void)testDuplicateIdentifierReplacesPendingRequest {
    NSString *identifier = [self uniqueIdentifier];
    [self addNotificationWithRequestIdentifier:identifier iosId:identifier body:@"first"];
    [self addNotificationWithRequestIdentifier:identifier iosId:identifier body:@"second"];

    NSArray<UNNotificationRequest *> *matching = [self pendingRequestsMatchingIosId:identifier];
    XCTAssertEqual(matching.count, 1, @"Expected one pending request after replacing duplicate identifier.");
    UNNotificationRequest *request = matching.firstObject;
    XCTAssertEqualObjects(request.content.body, @"second", @"Expected second request to replace first.");
    XCTAssertEqualObjects(request.content.userInfo[@"__ios_id__"], identifier, @"Expected __ios_id__ userInfo roundtrip.");
}

- (void)testClearByIosIdRemovesAllMatchingRequests {
    NSString *iosId = [self uniqueIdentifier];
    [self addNotificationWithRequestIdentifier:[iosId stringByAppendingString:@"-1"] iosId:iosId body:@"a"];
    [self addNotificationWithRequestIdentifier:[iosId stringByAppendingString:@"-2"] iosId:iosId body:@"b"];

    com_codenameone_examples_hellocodenameone_LocalNotificationNativeImpl *impl = [com_codenameone_examples_hellocodenameone_LocalNotificationNativeImpl new];
    int before = [impl getScheduledLocalNotificationCount:iosId];
    XCTAssertEqual(before, 2, @"Expected two requests with matching __ios_id__.");

    [impl clearScheduledLocalNotifications:iosId];

    int after = [impl getScheduledLocalNotificationCount:iosId];
    XCTAssertEqual(after, 0, @"Expected all matching requests to be removed.");
}

- (void)testGetScheduledCountIgnoresNonMatchingRequests {
    NSString *target = [self uniqueIdentifier];
    [self addNotificationWithRequestIdentifier:[target stringByAppendingString:@"-ok"] iosId:target body:@"x"];
    [self addNotificationWithRequestIdentifier:[target stringByAppendingString:@"-other"] iosId:[target stringByAppendingString:@"-different"] body:@"y"];

    com_codenameone_examples_hellocodenameone_LocalNotificationNativeImpl *impl = [com_codenameone_examples_hellocodenameone_LocalNotificationNativeImpl new];
    int count = [impl getScheduledLocalNotificationCount:target];
    XCTAssertEqual(count, 1, @"Expected count to include only matching __ios_id__ values.");
}

- (NSString *)uniqueIdentifier {
    return [NSString stringWithFormat:@"cn1-local-notif-%@", NSUUID.UUID.UUIDString];
}

- (void)addNotificationWithRequestIdentifier:(NSString *)requestIdentifier iosId:(NSString *)iosId body:(NSString *)body {
    UNMutableNotificationContent *content = [UNMutableNotificationContent new];
    content.title = @"CN1 Local Notification Test";
    content.body = body;
    content.userInfo = @{@"__ios_id__": iosId};

    UNTimeIntervalNotificationTrigger *trigger =
        [UNTimeIntervalNotificationTrigger triggerWithTimeInterval:60 repeats:NO];
    UNNotificationRequest *request =
        [UNNotificationRequest requestWithIdentifier:requestIdentifier content:content trigger:trigger];

    XCTestExpectation *expectation = [self expectationWithDescription:@"add notification request"];
    [(id)[UNUserNotificationCenter currentNotificationCenter]
        addNotificationRequest:request
         withCompletionHandler:^(NSError * _Nullable error) {
             XCTAssertNil(error, @"Expected addNotificationRequest to succeed.");
             [expectation fulfill];
         }];
    [self waitForExpectations:@[expectation] timeout:2.0];
}

- (NSArray<UNNotificationRequest *> *)pendingRequestsMatchingIosId:(NSString *)iosId {
    __block NSArray<UNNotificationRequest *> *matching = @[];
    XCTestExpectation *expectation = [self expectationWithDescription:@"fetch pending requests"];
    [(id)[UNUserNotificationCenter currentNotificationCenter]
        getPendingNotificationRequestsWithCompletionHandler:^(NSArray<UNNotificationRequest *> * _Nonnull requests) {
            NSPredicate *predicate = [NSPredicate predicateWithBlock:^BOOL(UNNotificationRequest *evaluatedObject, NSDictionary *bindings) {
                (void)bindings;
                return [evaluatedObject.content.userInfo[@"__ios_id__"] isEqualToString:iosId];
            }];
            matching = [requests filteredArrayUsingPredicate:predicate];
            [expectation fulfill];
        }];
    [self waitForExpectations:@[expectation] timeout:2.0];
    return matching;
}

@end
