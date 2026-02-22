#import <Foundation/Foundation.h>

@interface com_codenameone_examples_hellocodenameone_LocalNotificationNativeImpl : NSObject {
}

-(void)clearScheduledLocalNotifications:(NSString*)param;
-(int)getScheduledLocalNotificationCount:(NSString*)param;
-(BOOL)isSupported;
@end
