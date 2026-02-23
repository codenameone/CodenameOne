#import "com_codenameone_examples_hellocodenameone_LocalNotificationNativeImpl.h"
#import <UIKit/UIKit.h>
#import <UserNotifications/UserNotifications.h>

@implementation com_codenameone_examples_hellocodenameone_LocalNotificationNativeImpl

-(void)clearScheduledLocalNotifications:(NSString*)param{
   if (param == nil) {
        return;
    }
    if (@available(iOS 10.0, *)) {
        UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
        dispatch_semaphore_t sem = dispatch_semaphore_create(0);
        __block NSMutableArray<NSString *> *matches = [NSMutableArray array];
        [center getPendingNotificationRequestsWithCompletionHandler:^(NSArray<UNNotificationRequest *> * _Nonnull requests) {
            for (UNNotificationRequest *request in requests) {
                NSString *uid = [NSString stringWithFormat:@"%@", [request.content.userInfo valueForKey:@"__ios_id__"]];
                if ([param isEqualToString:uid]) {
                    [matches addObject:request.identifier];
                }
            }
            dispatch_semaphore_signal(sem);
        }];
        dispatch_semaphore_wait(sem, dispatch_time(DISPATCH_TIME_NOW, (int64_t)(5 * NSEC_PER_SEC)));
        if ([matches count] > 0) {
            [center removePendingNotificationRequestsWithIdentifiers:matches];
            [center removeDeliveredNotificationsWithIdentifiers:matches];
        }
    }
}

-(int)getScheduledLocalNotificationCount:(NSString*)param{
    if (param == nil) {
        return 0;
    }
    __block int count = 0;
    if (@available(iOS 10.0, *)) {
        UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
        dispatch_semaphore_t sem = dispatch_semaphore_create(0);
        [center getPendingNotificationRequestsWithCompletionHandler:^(NSArray<UNNotificationRequest *> * _Nonnull requests) {
            for (UNNotificationRequest *request in requests) {
                NSString *uid = [NSString stringWithFormat:@"%@", [request.content.userInfo valueForKey:@"__ios_id__"]];
                if ([param isEqualToString:uid]) {
                    count++;
                }
            }
            dispatch_semaphore_signal(sem);
        }];
        dispatch_semaphore_wait(sem, dispatch_time(DISPATCH_TIME_NOW, (int64_t)(5 * NSEC_PER_SEC)));
    }
    return count;
}

-(BOOL)isSupported{
    return YES;
}

@end
