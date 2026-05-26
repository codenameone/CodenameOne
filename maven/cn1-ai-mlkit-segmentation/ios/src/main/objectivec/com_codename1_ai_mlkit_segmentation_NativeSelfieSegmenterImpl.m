        #import "com_codename1_ai_mlkit_segmentation_NativeSelfieSegmenterImpl.h"
        #import <UIKit/UIKit.h>
        #import <MLKitSegmentationSelfie/MLKSelfieSegmenterOptions.h>
#import <MLKitSegmentationCommon/MLKSegmenter.h>
#import <MLKitSegmentationCommon/MLKSegmentationMask.h>
#import <MLKitVision/MLKVisionImage.h>
#import <CoreVideo/CoreVideo.h>


        @implementation com_codename1_ai_mlkit_segmentation_NativeSelfieSegmenterImpl

        - (NSData *)segment:(NSData *)imageBytes {
    UIImage *image = [UIImage imageWithData:imageBytes];
    if (!image) return [NSData data];
    MLKVisionImage *vision = [[MLKVisionImage alloc] initWithImage:image];
    MLKSelfieSegmenterOptions *opts = [[MLKSelfieSegmenterOptions alloc] init];
    opts.segmenterMode = MLKSegmenterModeSingleImage;
    MLKSegmenter *seg = [MLKSegmenter segmenterWithOptions:opts];
    __block NSData *result = [NSData data];
    dispatch_semaphore_t sem = dispatch_semaphore_create(0);
    [seg processImage:vision completion:^(MLKSegmentationMask * _Nullable mask, NSError * _Nullable e) {
        if (mask) {
            size_t w = mask.width, h = mask.height;
            void *base = CVPixelBufferGetBaseAddress(mask.buffer);
            CVPixelBufferLockBaseAddress(mask.buffer, kCVPixelBufferLock_ReadOnly);
            NSMutableData *m = [NSMutableData dataWithLength:w * h];
            uint8_t *out = m.mutableBytes;
            float *src = (float *)base;
            for (size_t i = 0; i < w * h; i++) {
                float v = src[i];
                out[i] = (uint8_t)(v * 255.0f);
            }
            CVPixelBufferUnlockBaseAddress(mask.buffer, kCVPixelBufferLock_ReadOnly);
            result = m;
        }
        dispatch_semaphore_signal(sem);
    }];
    dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
    return result;
}


        -(BOOL)isSupported {
            return YES;
        }

        @end
