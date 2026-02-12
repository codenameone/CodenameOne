#import <Foundation/Foundation.h>

@interface com_codenameone_examples_hellocodenameone_LocalNotificationNativeImpl : NSObject {
}

-(int)countPendingNotificationsWithId:(NSString*)notificationId;
-(BOOL)isSupported;

@end
