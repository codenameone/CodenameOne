#import "com_codename1_ai_mlkit_translate_NativeTranslatorImpl.h"
#import <UIKit/UIKit.h>
#import <MLKitTranslate/MLKTranslator.h>
#import <MLKitTranslate/MLKTranslatorOptions.h>
#import <MLKitCommon/MLKModelDownloadConditions.h>

@implementation com_codename1_ai_mlkit_translate_NativeTranslatorImpl

- (NSString *)translate:(NSString *)text :(NSString *)sourceLang :(NSString *)targetLang {
    MLKTranslatorOptions *opts = [[MLKTranslatorOptions alloc]
                                  initWithSourceLanguage:sourceLang
                                  targetLanguage:targetLang];
    MLKTranslator *t = [MLKTranslator translatorWithOptions:opts];
    MLKModelDownloadConditions *cond = [[MLKModelDownloadConditions alloc]
                                        initWithAllowsCellularAccess:YES
                                        allowsBackgroundDownloading:YES];
    __block NSString *result = @"";
    dispatch_semaphore_t sem = dispatch_semaphore_create(0);
    [t downloadModelIfNeededWithConditions:cond completion:^(NSError * _Nullable err) {
        if (err) { dispatch_semaphore_signal(sem); return; }
        [t translateText:text completion:^(NSString * _Nullable r, NSError * _Nullable e) {
            if (r && !e) result = r;
            dispatch_semaphore_signal(sem);
        }];
    }];
    dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
    return result;
}

- (BOOL)isSupported {
    return YES;
}

@end
