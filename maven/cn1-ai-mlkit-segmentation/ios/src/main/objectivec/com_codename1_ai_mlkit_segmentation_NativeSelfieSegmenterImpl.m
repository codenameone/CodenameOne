#import "com_codename1_ai_mlkit_segmentation_NativeSelfieSegmenterImpl.h"
#import <UIKit/UIKit.h>
#import <MLKitSegmentationSelfie/MLKitSegmentationSelfie.h>
#import <MLKitSegmentationCommon/MLKitSegmentationCommon.h>
#import <MLKitVision/MLKitVision.h>
#import <CoreVideo/CoreVideo.h>

@implementation com_codename1_ai_mlkit_segmentation_NativeSelfieSegmenterImpl

-(NSData*)segment:(NSData*)param {
    UIImage *image = [UIImage imageWithData:param];
    if (!image) return [NSData data];
    MLKVisionImage *vision = [[MLKVisionImage alloc] initWithImage:image];
    MLKSelfieSegmenterOptions *opts = [[MLKSelfieSegmenterOptions alloc] init];
    opts.segmenterMode = MLKSegmenterModeSingleImage;
    MLKSegmenter *seg = [MLKSegmenter segmenterWithOptions:opts];
    __block NSData *result = [NSData data];
    dispatch_semaphore_t sem = dispatch_semaphore_create(0);
    [seg processImage:vision completion:^(MLKSegmentationMask * _Nullable mask, NSError * _Nullable e) {
        if (mask) {
            // MLKSegmentationMask exposes only `buffer` (a
            // CVPixelBuffer); dimensions come from CVPixelBufferGet*.
            CVPixelBufferRef buf = mask.buffer;
            size_t w = CVPixelBufferGetWidth(buf);
            size_t h = CVPixelBufferGetHeight(buf);
            CVPixelBufferLockBaseAddress(buf, kCVPixelBufferLock_ReadOnly);
            void *base = CVPixelBufferGetBaseAddress(buf);
            NSMutableData *m = [NSMutableData dataWithLength:w * h];
            uint8_t *out = m.mutableBytes;
            float *src = (float *)base;
            for (size_t i = 0; i < w * h; i++) {
                float v = src[i];
                out[i] = (uint8_t)(v * 255.0f);
            }
            CVPixelBufferUnlockBaseAddress(buf, kCVPixelBufferLock_ReadOnly);
            result = m;
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
