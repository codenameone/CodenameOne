#import <Foundation/Foundation.h>

@interface com_codename1_freshdesk_MobihelpNativeImpl : NSObject
@end

@implementation com_codename1_freshdesk_MobihelpNativeImpl

-(void)showFeedback{
    // Stubbed implementation for compilation in documentation demos.
}

-(void)getUnreadCountAsync:(int)param{
    // Stubbed implementation for compilation in documentation demos.
}

@end

#if 0
// tag::mobihelpImport[]
#import "Mobihelp.h"
// end::mobihelpImport[]

// tag::mobihelpControllerImport[]
#import "CodenameOne_GLViewController.h"
// end::mobihelpControllerImport[]

// tag::mobihelpCallbackImport[]
#import "com_codename1_freshdesk_MobihelpNativeCallback.h"
// end::mobihelpCallbackImport[]

@implementation com_codename1_freshdesk_MobihelpNativeImpl

// tag::mobihelpShowFeedback[]
-(void)showFeedback{
    dispatch_async(dispatch_get_main_queue(), ^{
        [[Mobihelp sharedInstance] presentFeedback:[CodenameOne_GLViewController instance]];
    });
}
// end::mobihelpShowFeedback[]

// tag::mobihelpGetUnreadCountAsync[]
-(void)getUnreadCountAsync:(int)param{
    dispatch_async(dispatch_get_main_queue(), ^{
        [[Mobihelp sharedInstance]
            unreadCountWithCompletion:^(NSInteger count){
                com_codename1_freshdesk_MobihelpNativeCallback_fireUnreadUpdatesCallback___int_int_int(
                    CN1_THREAD_GET_STATE_PASS_ARG param, 3 /*SUCCESS*/, count);
            }];
    });
}
// end::mobihelpGetUnreadCountAsync[]

@end
#endif
