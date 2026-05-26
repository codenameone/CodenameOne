#import "com_codename1_ai_imagegen_NativeStableDiffusionImpl.h"
#import <UIKit/UIKit.h>

// Apple's StableDiffusion swift package compiled into the app as
// `CN1StableDiffusionRunner.swift` (shipped via the cn1lib). The
// Obj-C bridge invokes a thin C-callable wrapper around the Swift
// runner.
extern NSData *cn1_sd_generate(const char *prompt, int w, int h, int steps);

@implementation com_codename1_ai_imagegen_NativeStableDiffusionImpl

-(NSData*)generate:(NSString*)param param1:(int)param1 param2:(int)param2 param3:(int)param3 {
    return cn1_sd_generate([param UTF8String], param1, param2, param3);
}

-(BOOL)isSupported{
    return YES;
}

@end
