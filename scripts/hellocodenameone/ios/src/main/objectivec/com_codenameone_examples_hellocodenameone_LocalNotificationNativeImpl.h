#import <Foundation/Foundation.h>

@interface com_codenameone_examples_hellocodenameone_LocalNotificationNativeImpl : NSObject {
}

- (void)clearScheduledLocalNotifications:(NSString*)notificationId;
- (int)getScheduledLocalNotificationCount:(NSString*)notificationId;
- (BOOL)isSupported;

@end
