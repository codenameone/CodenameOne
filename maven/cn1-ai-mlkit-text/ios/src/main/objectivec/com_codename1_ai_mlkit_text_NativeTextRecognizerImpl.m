#import "com_codename1_ai_mlkit_text_NativeTextRecognizerImpl.h"
#import <UIKit/UIKit.h>
#import <MLKitTextRecognition/MLKitTextRecognition.h>
#import <MLKitVision/MLKitVision.h>

@implementation com_codename1_ai_mlkit_text_NativeTextRecognizerImpl

-(NSString*)recognize:(NSData*)param {
    UIImage *image = [UIImage imageWithData:param];
    if (!image) return @"";
    MLKVisionImage *vision = [[MLKVisionImage alloc] initWithImage:image];
    MLKTextRecognizer *recognizer = [MLKTextRecognizer textRecognizerWithOptions:
                                      [[MLKCommonTextRecognizerOptions alloc] init]];
    __block NSString *result = @"";
    dispatch_semaphore_t sem = dispatch_semaphore_create(0);
    [recognizer processImage:vision completion:^(MLKText * _Nullable text, NSError * _Nullable err) {
        if (text && !err) {
            result = text.text ?: @"";
        } else if (err) {
            result = @"";
        }
        dispatch_semaphore_signal(sem);
    }];
    dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
    return result;
}

-(BOOL)isSupported{
    return YES;
}

@end
