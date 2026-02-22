#import "com_codenameone_examples_hellocodenameone_LocalNotificationNativeImpl.h"
#import <UIKit/UIKit.h>

@implementation com_codenameone_examples_hellocodenameone_LocalNotificationNativeImpl

-(void)clearScheduledLocalNotifications:(NSString*)param{
   if (param == nil) {
        return;
    }
    dispatch_sync(dispatch_get_main_queue(), ^{
        UIApplication *app = [UIApplication sharedApplication];
        NSArray *scheduled = [app scheduledLocalNotifications];
        for (UILocalNotification *notification in scheduled) {
            NSDictionary *userInfo = notification.userInfo;
            NSString *uid = [NSString stringWithFormat:@"%@", [userInfo valueForKey:@"__ios_id__"]];
            if ([param isEqualToString:uid]) {
                [app cancelLocalNotification:notification];
            }
        }
    });
}

-(int)getScheduledLocalNotificationCount:(NSString*)param{
    if (param == nil) {
        return 0;
    }
    __block int count = 0;
    dispatch_sync(dispatch_get_main_queue(), ^{
        UIApplication *app = [UIApplication sharedApplication];
        NSArray *scheduled = [app scheduledLocalNotifications];
        for (UILocalNotification *notification in scheduled) {
            NSDictionary *userInfo = notification.userInfo;
            NSString *uid = [NSString stringWithFormat:@"%@", [userInfo valueForKey:@"__ios_id__"]];
            if ([param isEqualToString:uid]) {
                count++;
            }
        }
    });
    return count;
}

-(BOOL)isSupported{
    return YES;
}

@end
