#import "org_myapp_app_cn1_InterfaceFirebaseImpl.h"
#import <Firebase/Firebase.h>
@implementation org_myapp_app_cn1_InterfaceFirebaseImpl
-(void)launchFirebase{
    [FIRApp configure];
}
-(void)logWithFirebase:(NSString*)param param1:(NSString*)param1{
    [FIRAnalytics logEventWithName:@"share_image"
                        parameters:@{
                                     @"name": param,
                                     @"full_text": param1
                                     }];
    NSLog(@"hello!"); 
}

-(BOOL)isSupported{
    return YES;
}
@end