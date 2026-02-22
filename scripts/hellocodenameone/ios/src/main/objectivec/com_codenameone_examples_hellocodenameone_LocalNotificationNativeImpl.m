#import "com_codenameone_examples_hellocodenameone_LocalNotificationNativeImpl.h"
#import <UIKit/UIKit.h>

@implementation com_codenameone_examples_hellocodenameone_LocalNotificationNativeImpl

- (void)clearScheduledLocalNotifications:(NSString*)notificationId {
    /*if (notificationId == nil) {
        return;
    }
    dispatch_sync(dispatch_get_main_queue(), ^{
        UIApplication *app = [UIApplication sharedApplication];
        NSArray *scheduled = [app scheduledLocalNotifications];
        for (UILocalNotification *notification in scheduled) {
            NSDictionary *userInfo = notification.userInfo;
            NSString *uid = [NSString stringWithFormat:@"%@", [userInfo valueForKey:@"__ios_id__"]];
            if ([notificationId isEqualToString:uid]) {
                [app cancelLocalNotification:notification];
            }
        }
    });*/
}

- (int)getScheduledLocalNotificationCount:(NSString*)notificationId {
    /*if (notificationId == nil) {
        return 0;
    }
    __block int count = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        UIApplication *app = [UIApplication sharedApplication];
        NSArray *scheduled = [app scheduledLocalNotifications];
        for (UILocalNotification *notification in scheduled) {
            NSDictionary *userInfo = notification.userInfo;
            NSString *uid = [NSString stringWithFormat:@"%@", [userInfo valueForKey:@"__ios_id__"]];
            if ([notificationId isEqualToString:uid]) {
                count++;
            }
        }
    });
    return count;*/
    return 0;
}

- (BOOL)isSupported {
    return YES;
}

@end
