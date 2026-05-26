        #import "com_codename1_ai_mlkit_pose_NativePoseDetectorImpl.h"
        #import <UIKit/UIKit.h>
        #import <MLKitPoseDetection/MLKPoseDetector.h>
#import <MLKitPoseDetection/MLKPoseDetectorOptions.h>
#import <MLKitPoseDetection/MLKPose.h>
#import <MLKitPoseDetection/MLKPoseLandmark.h>
#import <MLKitVision/MLKVisionImage.h>


        @implementation com_codename1_ai_mlkit_pose_NativePoseDetectorImpl

        - (NSData *)detect:(NSData *)imageBytes {
    UIImage *image = [UIImage imageWithData:imageBytes];
    if (!image) return [NSData data];
    MLKVisionImage *vision = [[MLKVisionImage alloc] initWithImage:image];
    MLKPoseDetectorOptions *opts = [[MLKPoseDetectorOptions alloc] init];
    MLKPoseDetector *det = [MLKPoseDetector poseDetectorWithOptions:opts];
    __block MLKPose *pose = nil;
    dispatch_semaphore_t sem = dispatch_semaphore_create(0);
    [det processImage:vision completion:^(NSArray<MLKPose *> * _Nullable r, NSError * _Nullable e) {
        if (r.count > 0) pose = r[0];
        dispatch_semaphore_signal(sem);
    }];
    dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER);
    float buf[99] = {0};
    if (pose) {
        for (NSInteger i = 0; i < 33 && i < pose.landmarks.count; i++) {
            MLKPoseLandmark *lm = pose.landmarks[i];
            buf[i * 3]     = (float)lm.position.x;
            buf[i * 3 + 1] = (float)lm.position.y;
            buf[i * 3 + 2] = (float)lm.inFrameLikelihood;
        }
    }
    // Pack as big-endian float bytes (matches JAVA_ARRAY_FLOAT on iOS port).
    return [NSData dataWithBytes:buf length:sizeof(buf)];
}


        -(BOOL)isSupported {
            return YES;
        }

        @end
