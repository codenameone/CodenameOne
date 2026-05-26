        #import "com_codename1_ai_mlkit_labeling_NativeImageLabelerImpl.h"
        #import <UIKit/UIKit.h>
        #import <MLKitImageLabeling/MLKImageLabeler.h>
#import <MLKitImageLabeling/MLKImageLabelerOptions.h>
#import <MLKitImageLabeling/MLKImageLabel.h>
#import <MLKitVision/MLKVisionImage.h>
#import <arpa/inet.h>


        @implementation com_codename1_ai_mlkit_labeling_NativeImageLabelerImpl

        - (NSData *)label:(NSData *)imageBytes {
    UIImage *image = [UIImage imageWithData:imageBytes];
    if (!image) return [self packStrings:@[]];
    MLKVisionImage *vision = [[MLKVisionImage alloc] initWithImage:image];
    MLKImageLabelerOptions *opts = [[MLKImageLabelerOptions alloc] init];
    MLKImageLabeler *labeler = [MLKImageLabeler imageLabelerWithOptions:opts];
    __block NSArray<MLKImageLabel *> *labels = @[];
    dispatch_semaphore_t sem = dispatch_semaphore_create(0);
    [labeler processImage:vision completion:^(NSArray<MLKImageLabel *> * _Nullable r, NSError * _Nullable e) {
        labels = r ?: @[];
        dispatch_semaphore_signal(sem);
    }];
    dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
    NSMutableArray *m = [NSMutableArray array];
    for (MLKImageLabel *l in labels) { if (l.text) [m addObject:l.text]; }
    return [self packStrings:m];
}

- (NSData *)packStrings:(NSArray<NSString *> *)strings {
    NSMutableData *out = [NSMutableData data];
    uint32_t count = htonl((uint32_t)strings.count);
    [out appendBytes:&count length:sizeof(count)];
    for (NSString *s in strings) {
        NSData *u = [s dataUsingEncoding:NSUTF8StringEncoding];
        uint32_t len = htonl((uint32_t)u.length);
        [out appendBytes:&len length:sizeof(len)];
        [out appendData:u];
    }
    return out;
}


        -(BOOL)isSupported {
            return YES;
        }

        @end
