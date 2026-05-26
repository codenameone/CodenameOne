        #import "com_codename1_ai_mlkit_langid_NativeLanguageIdentifierImpl.h"
        #import <UIKit/UIKit.h>
        #import <MLKitLanguageID/MLKLanguageIdentification.h>


        @implementation com_codename1_ai_mlkit_langid_NativeLanguageIdentifierImpl

        - (NSString *)identify:(NSString *)input {
    MLKLanguageIdentification *id = [MLKLanguageIdentification languageIdentification];
    __block NSString *result = @"und";
    dispatch_semaphore_t sem = dispatch_semaphore_create(0);
    [id identifyLanguageForText:input completion:^(NSString * _Nullable lang, NSError * _Nullable e) {
        if (lang) result = lang;
        dispatch_semaphore_signal(sem);
    }];
    dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
    return result;
}


        -(BOOL)isSupported {
            return YES;
        }

        @end
