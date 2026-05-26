        #import "com_codename1_ai_imagegen_NativeStableDiffusionImpl.h"
        #import <UIKit/UIKit.h>


        @implementation com_codename1_ai_imagegen_NativeStableDiffusionImpl

        // Apple's StableDiffusion swift package compiled into the app as
// `CN1StableDiffusionRunner.swift` (shipped via the cn1lib). The Obj-C
// bridge invokes a thin C-callable wrapper around the Swift runner.
extern NSData *cn1_sd_generate(const char *prompt, int w, int h, int steps);

- (NSData *)generate:(NSString *)prompt :(int)width :(int)height :(int)steps {
    return cn1_sd_generate([prompt UTF8String], width, height, steps);
}


        -(BOOL)isSupported {
            return YES;
        }

        @end
