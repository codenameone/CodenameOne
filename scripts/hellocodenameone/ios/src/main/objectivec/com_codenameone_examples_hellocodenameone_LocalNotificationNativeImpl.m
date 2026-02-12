#import "com_codenameone_examples_hellocodenameone_LocalNotificationNativeImpl.h"
#import <UIKit/UIKit.h>

@implementation com_codenameone_examples_hellocodenameone_LocalNotificationNativeImpl

-(int)countPendingNotificationsWithId:(NSString*)notificationId {
    if (notificationId == nil) {
        return 0;
    }
    UIApplication *app = [UIApplication sharedApplication];
    NSArray *scheduledNotifications = [app scheduledLocalNotifications];
    NSInteger matches = 0;
    for (UILocalNotification *notification in scheduledNotifications) {
        NSDictionary *userInfo = notification.userInfo;
        NSString *scheduledId = [NSString stringWithFormat:@"%@", [userInfo valueForKey:@"__ios_id__"]];
        if ([notificationId isEqualToString:scheduledId]) {
            matches++;
        }
    }
    return (int)matches;
}

-(BOOL)isSupported{
    return YES;
}

@end
