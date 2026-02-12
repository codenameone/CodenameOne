#import "com_codenameone_examples_hellocodenameone_LocalNotificationNativeImpl.h"
#import <UserNotifications/UserNotifications.h>

@implementation com_codenameone_examples_hellocodenameone_LocalNotificationNativeImpl

-(int)countPendingNotificationsWithId:(NSString*)notificationId {
    if (@available(iOS 10.0, *)) {
        __block NSInteger matches = 0;
        dispatch_semaphore_t semaphore = dispatch_semaphore_create(0);
        [[UNUserNotificationCenter currentNotificationCenter] getPendingNotificationRequestsWithCompletionHandler:^(NSArray<UNNotificationRequest *> * _Nonnull requests) {
            for (UNNotificationRequest* request in requests) {
                if ([request.identifier isEqualToString:notificationId]) {
                    matches++;
                }
            }
            dispatch_semaphore_signal(semaphore);
        }];
        dispatch_time_t timeout = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(2 * NSEC_PER_SEC));
        dispatch_semaphore_wait(semaphore, timeout);
        return (int)matches;
    }
    return -1;
}

-(BOOL)isSupported{
    return YES;
}

@end
